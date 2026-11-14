package com.example.mojadieta.data.update

import android.content.Context
import com.example.mojadieta.domain.update.AppRelease
import com.example.mojadieta.domain.update.UpdateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

/**
 * Pobiera najnowsze wydanie z publicznego repozytorium GitHub
 * (https://api.github.com/repos/{owner}/{repo}/releases/latest) – bez logowania.
 */
class GitHubUpdateRepository(
    private val context: Context,
    private val repository: String
) : UpdateRepository {

    override suspend fun fetchLatestRelease(): AppRelease? = withContext(Dispatchers.IO) {
        val connection = (URL("https://api.github.com/repos/$repository/releases/latest")
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "MojaDieta-Android")
        }
        try {
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    parseRelease(JSONObject(body))
                }
                HttpURLConnection.HTTP_NOT_FOUND -> null // brak opublikowanych wydań
                else -> throw IOException("Serwer GitHub odpowiedział kodem ${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseRelease(json: JSONObject): AppRelease? {
        val assets = json.optJSONArray("assets") ?: return null
        var apkUrl: String? = null
        var apkSize = 0L
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                apkUrl = asset.optString("browser_download_url")
                apkSize = asset.optLong("size")
                break
            }
        }
        if (apkUrl.isNullOrBlank()) return null
        val tag = json.optString("tag_name")
        return AppRelease(
            versionName = tag.removePrefix("v").removePrefix("V"),
            title = json.optString("name").ifBlank { tag },
            notes = json.optString("body").trim(),
            apkUrl = apkUrl,
            apkSizeBytes = apkSize,
            pageUrl = json.optString("html_url")
        )
    }

    override suspend fun downloadApk(release: AppRelease, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            // Usuwamy stare pobrane wersje, żeby nie zajmowały miejsca.
            dir.listFiles()?.forEach { it.delete() }
            val target = File(dir, "moja-dieta-${release.versionName}.apk")
            val partial = File(dir, "${target.name}.part")

            val connection = (URL(release.apkUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 20_000
                readTimeout = 30_000
                setRequestProperty("User-Agent", "MojaDieta-Android")
            }
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("Nie udało się pobrać pliku (kod ${connection.responseCode})")
                }
                val total = connection.contentLengthLong.takeIf { it > 0 } ?: release.apkSizeBytes
                connection.inputStream.use { input ->
                    partial.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var downloaded = 0L
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (total > 0) onProgress((downloaded.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }
                if (!partial.renameTo(target)) throw IOException("Nie udało się zapisać pliku aktualizacji")
                onProgress(1f)
                target
            } finally {
                connection.disconnect()
                if (partial.exists()) partial.delete()
            }
        }
}
