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
import java.net.URL
import java.util.Base64
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Klient API Google Gemini Vision do automatycznego rozpoznawania posiłków
 * i szacowania ich wartości odżywczych na podstawie przesłanego zdjęcia.
 */
class MealVisionRecognizer {

    private val candidateModels = listOf(
        "gemini-3.5-flash-lite",
        "gemini-3.8-flash",
        "gemini-3.7-flash",
        "gemini-3.5-flash",
        "gemini-flash-latest"
    )

    suspend fun recognizeMeal(imageFile: File, apiKey: String): RecognizedMeal = withContext(Dispatchers.IO) {
        if (!imageFile.exists() || imageFile.length() == 0L) {
            throw IllegalArgumentException("Plik zdjęcia nie istnieje lub jest pusty.")
        }
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            throw IllegalArgumentException("Brak klucza Gemini API. Skonfiguruj go w Ustawieniach.")
        }

        val base64Image = prepareBase64Image(imageFile)

        var lastException: Exception? = null
        for (attempt in 1..2) {
            for (model in candidateModels) {
                try {
                    return@withContext callGeminiApi(model, base64Image, cleanKey)
                } catch (e: ModelNotFoundException) {
                    lastException = e
                    continue
                } catch (e: IOException) {
                    lastException = e
                    if (e.message?.contains("przeciążone", ignoreCase = true) == true) {
                        continue // próbuj kolejny model z listy przy 503
                    }
                    throw e
                } catch (e: Exception) {
                    throw e
                }
            }
            if (attempt == 1) kotlinx.coroutines.delay(1000)
        }
        throw lastException ?: IOException("Nie udało się połączyć z usługą Gemini.")
    }

    private fun prepareBase64Image(imageFile: File): String {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(imageFile.absolutePath, bounds)

        val maxDimension = 1024
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
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.getEncoder().encodeToString(bytes)
    }

    private fun callGeminiApi(model: String, base64Image: String, apiKey: String): RecognizedMeal {
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 25_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Accept", "application/json")
        }

        val prompt = "Jesteś precyzyjnym dietetykiem. Przeanalizuj to zdjęcie posiłku i oszacuj jego zawartość dla całej widocznej porcji. " +
            "Zwróć WYŁĄCZNIE obiekt JSON w formacie: " +
            "{\"name\": \"zwięzła nazwa dania po polsku\", \"kcal\": liczba_całkowita, \"protein\": gramy_białka, \"carbs\": gramy_węglowodanów, \"fat\": gramy_tłuszczu, \"fiber\": gramy_błonnika_lub_null, \"sugar\": gramy_cukru_lub_null, \"salt\": gramy_soli_lub_null}."

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    })
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
            return parseGeminiResponse(responseBody)
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

    internal fun parseGeminiResponse(jsonString: String): RecognizedMeal {
        val root = JSONObject(jsonString)
        val candidates = root.optJSONArray("candidates")
            ?: throw IOException("Brak odpowiedzi od modelu.")
        if (candidates.length() == 0) throw IOException("Model nie wygenerował odpowiedzi.")

        val firstCandidate = candidates.getJSONObject(0)
        val parts = firstCandidate.optJSONObject("content")?.optJSONArray("parts")
            ?: throw IOException("Brak treści w odpowiedzi.")
        if (parts.length() == 0) throw IOException("Pusta odpowiedź z modelu.")

        val text = parts.getJSONObject(0).optString("text").trim()
        return parseMealJson(text)
    }

    internal fun parseMealJson(text: String): RecognizedMeal {
        val cleanedJson = if (text.startsWith("```")) {
            text.removePrefix("```json").removePrefix("```").substringBeforeLast("```").trim()
        } else {
            text
        }

        val obj = JSONObject(cleanedJson)
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

    private fun round1(v: Double): Double = (v * 10.0).roundToInt() / 10.0

    private class ModelNotFoundException(msg: String) : Exception(msg)
}
