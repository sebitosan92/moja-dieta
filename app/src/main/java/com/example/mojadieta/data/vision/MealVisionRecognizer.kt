package com.example.mojadieta.data.vision

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.mojadieta.domain.model.RecognizedMeal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.Base64
import kotlin.math.max
import kotlin.math.roundToInt

data class NutritionLabelResult(
    val name: String,
    val caloriesPer100g: Int,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val fiberPer100g: Double? = null,
    val sugarPer100g: Double? = null,
    val saltPer100g: Double? = null
)

data class FridgeRecipeSuggestion(
    val title: String,
    val description: String,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val ingredients: List<String>,
    val instructions: List<String>
)

/**
 * Klient API Google Gemini do rozpoznawania posiłków ze zdjęć i tekstu,
 * skanowania etykiet odżywczych oraz generowania przepisów ze składników w lodówce.
 */
class MealVisionRecognizer {

    private val candidateModels = listOf(
        "gemini-3.5-flash-lite",
        "gemini-3.8-flash",
        "gemini-3.7-flash",
        "gemini-3.5-flash",
        "gemini-flash-latest"
    )

    /** Rozpoznaje posiłek na podstawie przesłanego zdjęcia. */
    suspend fun recognizeMeal(imageFile: File, apiKey: String): RecognizedMeal = withContext(Dispatchers.IO) {
        if (!imageFile.exists() || imageFile.length() == 0L) {
            throw IllegalArgumentException("Plik zdjęcia nie istnieje lub jest pusty.")
        }
        val base64Image = prepareBase64Image(imageFile)
        val prompt = "Jesteś precyzyjnym dietetykiem. Przeanalizuj to zdjęcie posiłku i oszacuj jego zawartość dla całej widocznej porcji. " +
            "Zwróć WYŁĄCZNIE obiekt JSON w formacie: " +
            "{\"name\": \"zwięzła nazwa dania po polsku\", \"kcal\": liczba_całkowita, \"protein\": gramy_białka, \"carbs\": gramy_węglowodanów, \"fat\": gramy_tłuszczu, \"fiber\": gramy_błonnika_lub_null, \"sugar\": gramy_cukru_lub_null, \"salt\": gramy_soli_lub_null}."

        val jsonText = executeGeminiPrompt(prompt, base64Image, apiKey)
        parseMealJson(jsonText)
    }

    /** Szacuje wartości odżywcze posiłku na podstawie opisu tekstowego / głosowego. */
    suspend fun recognizeMealFromText(description: String, apiKey: String): RecognizedMeal = withContext(Dispatchers.IO) {
        val cleanDesc = description.trim()
        if (cleanDesc.isBlank()) {
            throw IllegalArgumentException("Opis posiłku nie może być pusty.")
        }
        val prompt = "Jesteś precyzyjnym dietetykiem. Użytkownik opisał posiłek, który zjadł: \"$cleanDesc\". " +
            "Oszacuj zawartość całego posiłku na podstawie tego opisu (przyjmując standardowe porcje, jeśli nie podano wagi). " +
            "Zwróć WYŁĄCZNIE obiekt JSON w formacie: " +
            "{\"name\": \"zwięzła nazwa dania po polsku\", \"kcal\": liczba_całkowita, \"protein\": gramy_białka, \"carbs\": gramy_węglowodanów, \"fat\": gramy_tłuszczu, \"fiber\": gramy_błonnika_lub_null, \"sugar\": gramy_cukru_lub_null, \"salt\": gramy_soli_lub_null}."

        val jsonText = executeGeminiPrompt(prompt, null, apiKey)
        parseMealJson(jsonText)
    }

    /** Odczytuje tabelę wartości odżywczych ze zdjęcia etykiety produktu spożywczego (na 100 g). */
    suspend fun recognizeNutritionLabel(imageFile: File, apiKey: String): NutritionLabelResult = withContext(Dispatchers.IO) {
        if (!imageFile.exists() || imageFile.length() == 0L) {
            throw IllegalArgumentException("Plik zdjęcia nie istnieje lub jest pusty.")
        }
        val base64Image = prepareBase64Image(imageFile)
        val prompt = "Jesteś precyzyjnym skanerem etykiet spożywczych (OCR). Odczytaj ze zdjęcia tabelę wartości odżywczych na 100 g lub 100 ml produktu oraz nazwę produktu jeśli jest widoczna. " +
            "Zwróć WYŁĄCZNIE obiekt JSON w formacie: " +
            "{\"name\": \"nazwa produktu lub Pusty jeśli brak\", \"kcal\": liczba_kcal_na_100g, \"protein\": gramy_białka_na_100g, \"carbs\": gramy_węglowodanów_na_100g, \"fat\": gramy_tłuszczu_na_100g, \"fiber\": gramy_błonnika_na_100g_lub_null, \"sugar\": gramy_cukru_na_100g_lub_null, \"salt\": gramy_soli_na_100g_lub_null}."

        val jsonText = executeGeminiPrompt(prompt, base64Image, apiKey)
        parseLabelJson(jsonText)
    }

    /** Generuje 2-3 propozycje przepisów z posiadanych składników (lodówka AI), uwzględniając pozostały limit kalorii. */
    suspend fun generateFridgeRecipes(
        ingredientsText: String,
        imageFile: File?,
        remainingCalories: Int?,
        apiKey: String
    ): List<FridgeRecipeSuggestion> = withContext(Dispatchers.IO) {
        val base64Image = imageFile?.takeIf { it.exists() && it.length() > 0L }?.let { prepareBase64Image(it) }
        val calLimitDesc = if (remainingCalories != null && remainingCalories > 0) "Mój pozostały bilans na dziś to około $remainingCalories kcal. " else ""
        val prompt = "Jesteś kreatywnym szefem kuchni i dietetykiem. Użytkownik chce ugotować coś z tego co ma w lodówce. " +
            "Dostępne składniki: \"${ingredientsText.ifBlank { "widoczne na załączonym zdjęciu" }}\". $calLimitDesc" +
            "Zaproponuj 2 do 3 prostych, zdrowych dań. " +
            "Zwróć WYŁĄCZNIE tablicę JSON w formacie: " +
            "[{\"title\": \"Nazwa dania\", \"description\": \"Krótki opis\", \"kcal\": liczba_kcal, \"protein\": gramy_białka, \"carbs\": gramy_węglowodanów, \"fat\": gramy_tłuszczu, \"ingredients\": [\"składnik 1 z gramaturą\", \"składnik 2\"], \"instructions\": [\"krok 1\", \"krok 2\"]}]"

        val jsonText = executeGeminiPrompt(prompt, base64Image, apiKey)
        parseFridgeRecipesJson(jsonText)
    }

    private suspend fun executeGeminiPrompt(
        prompt: String,
        base64Image: String?,
        apiKey: String
    ): String = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            throw IllegalArgumentException("Brak klucza Gemini API. Skonfiguruj go w Ustawieniach.")
        }

        var lastException: Exception? = null
        for (attempt in 1..2) {
            for (model in candidateModels) {
                try {
                    return@withContext callGeminiRaw(model, prompt, base64Image, cleanKey)
                } catch (e: ModelNotFoundException) {
                    lastException = e
                    continue
                } catch (e: SocketTimeoutException) {
                    lastException = e
                    continue
                } catch (e: IOException) {
                    lastException = e
                    if (e.message?.contains("przeciążone", ignoreCase = true) == true) {
                        continue
                    }
                    throw e
                } catch (e: Exception) {
                    throw e
                }
            }
            if (attempt == 1) kotlinx.coroutines.delay(1000)
        }
        if (lastException is SocketTimeoutException) {
            throw IOException("Upłynął limit czasu odpowiedzi AI. Serwery Gemini odpowiadają zbyt wolno – spróbuj ponownie za moment.")
        }
        throw lastException ?: IOException("Nie udało się połączyć z usługą Gemini.")
    }

    private fun prepareBase64Image(imageFile: File): String {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(imageFile.absolutePath, bounds)

        val maxDimension = 768
        var sampleSize = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= maxDimension) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath, options)
            ?: throw IOException("Nie udało się odczytać pliku graficznego.")

        val longest = max(bitmap.width, bitmap.height)
        val scaled = if (longest > maxDimension) {
            val scale = maxDimension.toFloat() / longest
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).roundToInt(),
                (bitmap.height * scale).roundToInt(),
                true
            )
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.getEncoder().encodeToString(bytes)
    }

    private fun callGeminiRaw(
        model: String,
        prompt: String,
        base64Image: String?,
        apiKey: String
    ): String {
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 45_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Accept", "application/json")
        }

        val partsArray = JSONArray().apply {
            put(JSONObject().apply {
                put("text", prompt)
            })
            if (base64Image != null) {
                put(JSONObject().apply {
                    put("inline_data", JSONObject().apply {
                        put("mime_type", "image/jpeg")
                        put("data", base64Image)
                    })
                })
            }
        }

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", partsArray)
                })
            })
            put("generationConfig", JSONObject().apply {
                put("response_mime_type", "application/json")
            })
        }

        try {
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use {
                it.write(requestJson.toString())
                it.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                throw ModelNotFoundException("Model $model nie jest dostępny")
            }

            if (responseCode !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                handleHttpError(responseCode, errorBody)
            }

            val responseBody = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            return extractTextFromGeminiResponse(responseBody)
        } finally {
            connection.disconnect()
        }
    }

    private fun handleHttpError(code: Int, body: String): Nothing {
        val errorMsg = runCatching {
            JSONObject(body).optJSONObject("error")?.optString("message")
        }.getOrNull()

        when {
            code == 400 && errorMsg?.contains("API_KEY_INVALID", ignoreCase = true) == true || code == 403 -> {
                throw IllegalArgumentException("Nieprawidłowy klucz API Gemini. Sprawdź swój klucz.")
            }
            code == 429 -> {
                throw IOException("Przekroczono limit zapytań Gemini API. Spróbuj za chwilę.")
            }
            code == 503 -> {
                throw IOException("Serwery Google Gemini są chwilowo przeciążone. Spróbuj ponownie za moment.")
            }
            !errorMsg.isNullOrBlank() -> {
                throw IOException("Błąd Gemini API ($code): $errorMsg")
            }
            else -> {
                throw IOException("Serwer Gemini odpowiedział kodem błędu $code.")
            }
        }
    }

    private fun extractTextFromGeminiResponse(jsonString: String): String {
        val root = JSONObject(jsonString)
        val candidates = root.optJSONArray("candidates")
            ?: throw IOException("Brak odpowiedzi od modelu.")
        if (candidates.length() == 0) throw IOException("Model nie wygenerował odpowiedzi.")

        val firstCandidate = candidates.getJSONObject(0)
        val parts = firstCandidate.optJSONObject("content")?.optJSONArray("parts")
            ?: throw IOException("Brak treści w odpowiedzi.")
        if (parts.length() == 0) throw IOException("Pusta odpowiedź z modelu.")

        val text = parts.getJSONObject(0).optString("text").trim()
        return cleanJson(text)
    }

    internal fun cleanJson(text: String): String {
        val trimmed = text.trim()
        return if (trimmed.startsWith("```")) {
            trimmed.removePrefix("```json").removePrefix("```").substringBeforeLast("```").trim()
        } else {
            trimmed
        }
    }

    internal fun parseGeminiResponse(jsonString: String): RecognizedMeal {
        val text = extractTextFromGeminiResponse(jsonString)
        return parseMealJson(text)
    }

    internal fun parseMealJson(rawJson: String): RecognizedMeal {
        val obj = JSONObject(cleanJson(rawJson))
        val name = obj.optString("name").trim().ifBlank { "Posiłek ze zdjęcia" }
        val kcal = obj.optInt("kcal", 0).coerceAtLeast(0)
        val protein = round1(obj.optDouble("protein", 0.0).coerceAtLeast(0.0))
        val carbs = round1(obj.optDouble("carbs", 0.0).coerceAtLeast(0.0))
        val fat = round1(obj.optDouble("fat", 0.0).coerceAtLeast(0.0))

        val fiber = if (obj.has("fiber") && !obj.isNull("fiber")) round1(obj.optDouble("fiber").coerceAtLeast(0.0)) else null
        val sugar = if (obj.has("sugar") && !obj.isNull("sugar")) round1(obj.optDouble("sugar").coerceAtLeast(0.0)) else null
        val salt = if (obj.has("salt") && !obj.isNull("salt")) round1(obj.optDouble("salt").coerceAtLeast(0.0)) else null

        return RecognizedMeal(
            name = name,
            calories = kcal,
            protein = protein,
            carbs = carbs,
            fat = fat,
            fiber = fiber,
            sugar = sugar,
            salt = salt
        )
    }

    internal fun parseLabelJson(rawJson: String): NutritionLabelResult {
        val obj = JSONObject(cleanJson(rawJson))
        val name = obj.optString("name", "Produkt z etykiety").trim().ifBlank { "Produkt z etykiety" }
        val kcal = obj.optInt("kcal", 0).coerceAtLeast(0)
        val protein = round1(obj.optDouble("protein", 0.0).coerceAtLeast(0.0))
        val carbs = round1(obj.optDouble("carbs", 0.0).coerceAtLeast(0.0))
        val fat = round1(obj.optDouble("fat", 0.0).coerceAtLeast(0.0))

        val fiber = if (obj.has("fiber") && !obj.isNull("fiber")) round1(obj.optDouble("fiber").coerceAtLeast(0.0)) else null
        val sugar = if (obj.has("sugar") && !obj.isNull("sugar")) round1(obj.optDouble("sugar").coerceAtLeast(0.0)) else null
        val salt = if (obj.has("salt") && !obj.isNull("salt")) round1(obj.optDouble("salt").coerceAtLeast(0.0)) else null

        return NutritionLabelResult(
            name = name,
            caloriesPer100g = kcal,
            proteinPer100g = protein,
            carbsPer100g = carbs,
            fatPer100g = fat,
            fiberPer100g = fiber,
            sugarPer100g = sugar,
            saltPer100g = salt
        )
    }

    internal fun parseFridgeRecipesJson(rawJson: String): List<FridgeRecipeSuggestion> {
        val array = JSONArray(cleanJson(rawJson))
        val list = mutableListOf<FridgeRecipeSuggestion>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val title = obj.optString("title", "Przepis z lodówki")
            val desc = obj.optString("description", "")
            val kcal = obj.optInt("kcal", 0)
            val protein = round1(obj.optDouble("protein", 0.0))
            val carbs = round1(obj.optDouble("carbs", 0.0))
            val fat = round1(obj.optDouble("fat", 0.0))

            val ingArray = obj.optJSONArray("ingredients") ?: JSONArray()
            val ingredients = (0 until ingArray.length()).map { ingArray.getString(it) }

            val instArray = obj.optJSONArray("instructions") ?: JSONArray()
            val instructions = (0 until instArray.length()).map { instArray.getString(it) }

            list.add(
                FridgeRecipeSuggestion(
                    title = title,
                    description = desc,
                    calories = kcal,
                    protein = protein,
                    carbs = carbs,
                    fat = fat,
                    ingredients = ingredients,
                    instructions = instructions
                )
            )
        }
        return list
    }

    private fun round1(v: Double): Double = (v * 10.0).roundToInt() / 10.0
}

class ModelNotFoundException(message: String) : Exception(message)
