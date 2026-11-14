package com.example.mojadieta.data.images

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Zdjęcia przepisów i posiłków: wczytywanie (z obrotem EXIF), zapis przyciętej wersji
 * w pamięci aplikacji (JPEG, max [MAX_SIDE] px) i sprzątanie nieużywanych plików.
 */
class ImageStorage(private val context: Context) {

    private val dir: File get() = File(context.filesDir, "images").apply { mkdirs() }

    /** Wczytuje zdjęcie z galerii/aparatu, zmniejszone do [maxSide] px i obrócone zgodnie z EXIF. */
    suspend fun loadBitmap(uri: Uri, maxSide: Int = 2048): Bitmap? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // Przy inJustDecodeBounds decodeStream zawsze zwraca null – liczą się tylko wymiary w [bounds].
        val stream = resolver.openInputStream(uri) ?: return@withContext null
        stream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return@withContext null

        val rotation = runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                when (ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        }.getOrDefault(0f)

        if (rotation == 0f) {
            decoded
        } else {
            val matrix = Matrix().apply { postRotate(rotation) }
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        }
    }

    /** Zapisuje wycięty fragment zdjęcia, zmniejszony do [MAX_SIDE] px. Zwraca ścieżkę pliku. */
    suspend fun saveCropped(
        source: Bitmap,
        left: Int,
        top: Int,
        width: Int,
        height: Int
    ): String = withContext(Dispatchers.IO) {
        val safeLeft = left.coerceIn(0, source.width - 1)
        val safeTop = top.coerceIn(0, source.height - 1)
        val safeWidth = width.coerceIn(1, source.width - safeLeft)
        val safeHeight = height.coerceIn(1, source.height - safeTop)
        var cropped = Bitmap.createBitmap(source, safeLeft, safeTop, safeWidth, safeHeight)
        val longest = max(cropped.width, cropped.height)
        if (longest > MAX_SIDE) {
            val scale = MAX_SIDE.toFloat() / longest
            cropped = Bitmap.createScaledBitmap(
                cropped,
                (cropped.width * scale).roundToInt(),
                (cropped.height * scale).roundToInt(),
                true
            )
        }
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        file.outputStream().use { cropped.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        file.absolutePath
    }

    fun delete(path: String?) {
        if (path == null) return
        val file = File(path)
        // Usuwamy wyłącznie pliki z naszego folderu zdjęć.
        if (file.parentFile?.absolutePath == dir.absolutePath) file.delete()
    }

    /** Usuwa zdjęcia, do których nie odwołuje się żaden przepis ani wpis w dzienniku. */
    suspend fun cleanup(usedPaths: Set<String>) = withContext(Dispatchers.IO) {
        val oneHourAgo = System.currentTimeMillis() - 60 * 60 * 1000
        dir.listFiles()?.forEach { file ->
            // Świeże pliki zostawiamy – mogą należeć do formularza, który jest właśnie otwarty.
            if (file.absolutePath !in usedPaths && file.lastModified() < oneHourAgo) file.delete()
        }
    }

    /** Tymczasowy plik na zdjęcie z aparatu (udostępniany aparatowi przez FileProvider). */
    fun newCameraFile(): File {
        val photos = File(context.cacheDir, "photos").apply { mkdirs() }
        photos.listFiles()?.forEach { it.delete() }
        return File(photos, "camera_${System.currentTimeMillis()}.jpg")
    }

    companion object {
        const val MAX_SIDE = 1280
    }
}
