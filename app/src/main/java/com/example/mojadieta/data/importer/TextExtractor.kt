package com.example.mojadieta.data.importer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.example.mojadieta.data.images.ImageStorage
import com.example.mojadieta.domain.importer.OcrPageMerger
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
        // Kolejne zrzuty przewijanej strony zwykle się pokrywają – wspólny fragment usuwamy.
        return OcrPageMerger.merge(parts)
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
        return orderBlocks(result, bitmap.width, bitmap.height)
    }

    private class Piece(val text: String, val box: Rect)

    /**
     * Układa tekst w kolejności czytania.
     * Zrzut ekranu telefonu (wysoki obraz): pomijamy pasek stanu i pasek nawigacji, czytamy wierszami.
     * Strona dokumentu: jeśli ma dwie kolumny, najpierw czytamy całą lewą, potem prawą.
     */
    private fun orderBlocks(text: Text, pageWidth: Int, pageHeight: Int): String {
        val blocks = text.textBlocks.filter { it.boundingBox != null }
        if (blocks.isEmpty()) return text.text
        val screenshot = pageHeight >= pageWidth * 1.6f
        val top = (pageHeight * 0.045f).toInt()
        val bottom = (pageHeight * 0.955f).toInt()
        fun pieces(list: List<Text.TextBlock>) = list.flatMap { block ->
            block.lines.mapNotNull { line -> line.boundingBox?.let { Piece(line.text, it) } }
        }.filter { piece -> !screenshot || piece.box.centerY() in top..bottom }

        if (!screenshot) {
            val half = pageWidth / 2
            val margin = pageWidth * 0.05
            val left = blocks.count { it.boundingBox!!.right < half + margin }
            val right = blocks.count { it.boundingBox!!.left > half - margin }
            val twoColumns = left >= 3 && right >= 3 && left + right >= blocks.size * 0.8
            if (twoColumns) {
                val (l, r) = blocks.partition { it.boundingBox!!.centerX() < half }
                return (rows(pieces(l), pageWidth) + rows(pieces(r), pageWidth)).joinToString("\n")
            }
        }
        return rows(pieces(blocks), pageWidth).joinToString("\n")
    }

    /**
     * Grupuje linie w wiersze (po położeniu w pionie). W wierszu elementy blisko siebie łączymy,
     * a odległe (np. nazwa składnika po lewej i ilość po prawej, kolumny wartości odżywczych) zostawiamy jako osobne linie.
     */
    private fun rows(pieces: List<Piece>, pageWidth: Int): List<String> {
        val rows = mutableListOf<MutableList<Piece>>()
        pieces.sortedBy { it.box.top }.forEach { piece ->
            val row = rows.lastOrNull()
            val ref = row?.first()?.box
            if (row != null && ref != null && piece.box.centerY() in ref.top..ref.bottom) row += piece else rows += mutableListOf(piece)
        }
        val gap = pageWidth * 0.06
        return rows.flatMap { row ->
            val out = mutableListOf<StringBuilder>()
            var lastRight = Int.MIN_VALUE
            row.sortedBy { it.box.left }.forEach { piece ->
                if (out.isNotEmpty() && piece.box.left - lastRight < gap) out.last().append(' ').append(piece.text)
                else out += StringBuilder(piece.text)
                lastRight = maxOf(lastRight, piece.box.right)
            }
            out.map { it.toString() }
        }
    }

    private companion object {
        const val MAX_PDF_PAGES = 80
        const val PDF_RENDER_WIDTH = 1600
    }
}
