package com.example.mojadieta.data.importer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.example.mojadieta.data.images.ImageStorage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Odczyt tekstu ze zdjęć/zrzutów ekranu i plików PDF.
 * Strony PDF są renderowane do obrazu i rozpoznawane przez ML Kit (działa na telefonie, bez internetu),
 * więc działa to zarówno dla „tekstowych” PDF-ów, jak i skanów.
 */
class TextExtractor(private val context: Context, private val imageStorage: ImageStorage) {

    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    suspend fun fromImages(uris: List<Uri>, onProgress: (done: Int, total: Int) -> Unit): String {
        val parts = mutableListOf<String>()
        uris.forEachIndexed { index, uri ->
            onProgress(index, uris.size)
            val bitmap = imageStorage.loadBitmap(uri, maxSide = 2400) ?: return@forEachIndexed
            parts += recognize(bitmap)
        }
        onProgress(uris.size, uris.size)
        return parts.joinToString("\n\n")
    }

    suspend fun fromPdf(uri: Uri, onProgress: (done: Int, total: Int) -> Unit): String {
        val pages = withContext(Dispatchers.IO) {
            val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
                ?: throw IllegalStateException("Nie można otworzyć pliku PDF")
            descriptor.use { pfd -> PdfRenderer(pfd).use { renderer -> renderer.pageCount } }
        }
        val total = pages.coerceAtMost(MAX_PDF_PAGES)
        val parts = mutableListOf<String>()
        for (index in 0 until total) {
            onProgress(index, total)
            val bitmap = withContext(Dispatchers.IO) {
                context.contentResolver.openFileDescriptor(uri, "r")!!.use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        renderer.openPage(index).use { page ->
                            val scale = PDF_RENDER_WIDTH.toFloat() / page.width
                            val bmp = Bitmap.createBitmap(
                                PDF_RENDER_WIDTH,
                                (page.height * scale).toInt().coerceAtLeast(1),
                                Bitmap.Config.ARGB_8888
                            )
                            bmp.eraseColor(Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bmp
                        }
                    }
                }
            }
            parts += recognize(bitmap)
            bitmap.recycle()
        }
        onProgress(total, total)
        return parts.joinToString("\n\n")
    }

    private suspend fun recognize(bitmap: Bitmap): String {
        val result: Text = suspendCancellableCoroutine { cont ->
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
        return orderBlocks(result, bitmap.width)
    }

    /**
     * Układa bloki tekstu w kolejności czytania. Jeśli strona ma dwie kolumny,
     * najpierw czytamy całą lewą, potem prawą.
     */
    private fun orderBlocks(text: Text, pageWidth: Int): String {
        val blocks = text.textBlocks.filter { it.boundingBox != null }
        if (blocks.isEmpty()) return text.text
        val half = pageWidth / 2
        val left = blocks.count { it.boundingBox!!.right < half + pageWidth * 0.05 }
        val right = blocks.count { it.boundingBox!!.left > half - pageWidth * 0.05 }
        val twoColumns = left >= 3 && right >= 3 && left + right >= blocks.size * 0.8
        val sorted = if (twoColumns) {
            blocks.sortedWith(compareBy({ if (it.boundingBox!!.centerX() < half) 0 else 1 }, { it.boundingBox!!.top }))
        } else {
            blocks.sortedWith(compareBy({ it.boundingBox!!.top / 12 }, { it.boundingBox!!.left }))
        }
        return sorted.joinToString("\n") { block -> block.lines.joinToString("\n") { it.text } }
    }

    private companion object {
        const val MAX_PDF_PAGES = 80
        const val PDF_RENDER_WIDTH = 1600
    }
}
