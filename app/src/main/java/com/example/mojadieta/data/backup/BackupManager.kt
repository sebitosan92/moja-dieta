package com.example.mojadieta.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.mojadieta.data.local.DietDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.system.exitProcess

/**
 * Kopia zapasowa w jednym pliku ZIP: baza danych, zdjęcia i ustawienia.
 * Plik zapisuje się przez systemowe okno wyboru (można wskazać np. Dysk Google).
 */
class BackupManager(private val context: Context) {

    private val dbFile: File get() = context.getDatabasePath(DietDatabase.NAME)
    private val imagesDir: File get() = File(context.filesDir, "images")
    private val prefsDir: File get() = File(context.applicationInfo.dataDir, "shared_prefs")

    /** Pliki ustawień, które przenosimy (bez stanu aktualizacji, który dotyczy konkretnej instalacji). */
    private val prefsToBackup = setOf("profile.xml", "settings.xml", "activity.xml", "splash_quotes.xml")

    data class Summary(val images: Int, val sizeKb: Long)

    suspend fun createBackup(target: Uri): Summary = withContext(Dispatchers.IO) {
        // Tworzymy spójną migawkę bazy danych (VACUUM INTO dla SQLite 3.27+ / Android 11+, lub checkpoint WAL)
        val tempDb = File(context.cacheDir, "backup_snapshot.db").apply { if (exists()) delete() }
        val vacuumSuccess = runCatching {
            val db = DietDatabase.getInstance(context).openHelper.writableDatabase
            db.execSQL("VACUUM INTO '${tempDb.absolutePath.replace("'", "''")}'")
            tempDb.exists() && tempDb.length() > 0
        }.getOrDefault(false)

        if (!vacuumSuccess) {
            DietDatabase.getInstance(context).openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
        }

        var images = 0
        val output = context.contentResolver.openOutputStream(target, "wt") ?: throw IOException("Nie można zapisać pliku")
        try {
            output.use { raw ->
                ZipOutputStream(raw.buffered()).use { zip ->
                    zip.putNextEntry(ZipEntry(MANIFEST))
                    zip.write("Moja Dieta backup\nformat=1\ncreated=${System.currentTimeMillis()}\n".toByteArray())
                    zip.closeEntry()
                    if (vacuumSuccess) {
                        zip.addFile("databases/${DietDatabase.NAME}", tempDb)
                    } else {
                        listOf(dbFile, File(dbFile.path + "-wal"), File(dbFile.path + "-shm")).filter { it.exists() }.forEach {
                            zip.addFile("databases/${it.name}", it)
                        }
                    }
                    imagesDir.listFiles()?.filter { it.isFile }?.forEach {
                        zip.addFile("images/${it.name}", it)
                        images++
                    }
                    prefsDir.listFiles()?.filter { it.name in prefsToBackup }?.forEach {
                        zip.addFile("shared_prefs/${it.name}", it)
                    }
                }
            }
        } finally {
            if (tempDb.exists()) tempDb.delete()
        }
        val size = context.contentResolver.openFileDescriptor(target, "r")?.use { it.statSize / 1024 } ?: 0
        Summary(images, size)
    }

    /**
     * Przywraca kopię: sprawdza plik, zamyka bazę, podmienia bazę, zdjęcia i ustawienia.
     * Po udanym przywróceniu trzeba ponownie uruchomić aplikację ([restartApp]).
     */
    suspend fun restore(source: Uri) = withContext(Dispatchers.IO) {
        // 1. Rozpakowanie do katalogu tymczasowego i walidacja.
        val staging = File(context.cacheDir, "restore").apply { deleteRecursively(); mkdirs() }
        var hasManifest = false
        val input = context.contentResolver.openInputStream(source) ?: throw IOException("Nie można odczytać pliku")
        input.use { raw ->
            ZipInputStream(raw.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue
                    if (entry.name == MANIFEST) hasManifest = true
                    val out = File(staging, entry.name)
                    // Ochrona przed ścieżkami typu „../” w archiwum.
                    if (!out.canonicalPath.startsWith(staging.canonicalPath + File.separator)) continue
                    out.parentFile?.mkdirs()
                    out.outputStream().use { zip.copyTo(it) }
                }
            }
        }
        val newDb = File(staging, "databases/${DietDatabase.NAME}")
        if (!hasManifest || !newDb.exists()) {
            staging.deleteRecursively()
            throw IllegalArgumentException("To nie jest kopia zapasowa aplikacji Moja Dieta.")
        }

        // 2. Podmiana plików.
        DietDatabase.closeInstance()
        listOf(dbFile, File(dbFile.path + "-wal"), File(dbFile.path + "-shm"), File(dbFile.path + "-journal")).forEach { it.delete() }
        dbFile.parentFile?.mkdirs()
        File(staging, "databases").listFiles()?.forEach { it.copyTo(File(dbFile.parentFile, it.name), overwrite = true) }

        imagesDir.deleteRecursively()
        imagesDir.mkdirs()
        File(staging, "images").listFiles()?.forEach { it.copyTo(File(imagesDir, it.name), overwrite = true) }

        prefsDir.mkdirs()
        File(staging, "shared_prefs").listFiles()?.filter { it.name in prefsToBackup }?.forEach {
            it.copyTo(File(prefsDir, it.name), overwrite = true)
        }
        staging.deleteRecursively()
    }

    /** Uruchamia aplikację od nowa, aby wczytać przywrócone dane. */
    fun restartApp() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        context.startActivity(Intent.makeRestartActivityTask(intent.component))
        exitProcess(0)
    }

    private fun ZipOutputStream.addFile(name: String, file: File) {
        putNextEntry(ZipEntry(name))
        file.inputStream().use { it.copyTo(this) }
        closeEntry()
    }

    companion object {
        private const val MANIFEST = "moja-dieta-backup.txt"

        fun suggestedFileName(): String =
            "moja-dieta-kopia-${java.time.LocalDate.now()}.zip"
    }
}
