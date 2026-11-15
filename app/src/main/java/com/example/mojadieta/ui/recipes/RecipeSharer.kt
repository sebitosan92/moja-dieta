package com.example.mojadieta.ui.recipes

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.mojadieta.domain.model.Recipe
import com.example.mojadieta.domain.model.RecipeIngredient
import com.example.mojadieta.ui.util.formatGrams
import java.io.File

/** Udostępnianie przepisu jako tekst (i zdjęcie, jeśli jest) – np. do Messengera, WhatsAppa, maila. */
object RecipeSharer {

    fun text(recipe: Recipe, ingredients: List<RecipeIngredient>): String = buildString {
        append("🍽 ").append(recipe.name).append("\n")
        append(recipe.category.label)
        if (recipe.servings > 1) append(" · porcje: ").append(recipe.servings)
        append("\n\n")
        append("Na porcję: ${recipe.calories} kcal · B ${formatGrams(recipe.protein)} g · ")
        append("W ${formatGrams(recipe.carbs)} g · T ${formatGrams(recipe.fat)} g\n")
        if (recipe.tags.isNotEmpty()) append(recipe.tags.joinToString(" ") { "#" + it.replace(" ", "") }).append("\n")
        if (ingredients.isNotEmpty() || recipe.ingredients.isNotBlank()) {
            append("\nSkładniki:\n")
            ingredients.forEach { append("• ").append(it.name).append(" – ").append(formatGrams(it.grams)).append(" g\n") }
            recipe.ingredients.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach { append("• ").append(it).append("\n") }
        }
        if (recipe.instructions.isNotBlank()) {
            append("\nPrzygotowanie:\n").append(recipe.instructions.trim()).append("\n")
        }
        append("\n— z aplikacji Moja Dieta")
    }

    fun share(context: Context, recipe: Recipe, ingredients: List<RecipeIngredient>) {
        val intent = Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT, text(recipe, ingredients))
            .putExtra(Intent.EXTRA_SUBJECT, recipe.name)
        val image = recipe.imagePath?.let { File(it) }?.takeIf { it.exists() }
        if (image != null) {
            // Kopia w cache/share – FileProvider nie udostępnia katalogu zdjęć bezpośrednio.
            val dir = File(context.cacheDir, "share").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val copy = File(dir, "przepis.jpg")
            image.copyTo(copy, overwrite = true)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", copy)
            intent.type = "image/jpeg"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.clipData = ClipData.newRawUri(recipe.name, uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            intent.type = "text/plain"
        }
        context.startActivity(Intent.createChooser(intent, "Udostępnij przepis"))
    }
}
