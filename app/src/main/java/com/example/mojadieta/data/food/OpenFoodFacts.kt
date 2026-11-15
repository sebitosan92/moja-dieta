package com.example.mojadieta.data.food

import com.example.mojadieta.domain.model.FoodProduct
import com.example.mojadieta.domain.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Klient otwartej bazy żywności Open Food Facts (https://world.openfoodfacts.org).
 * Dane na licencji ODbL – źródło jest podane na ekranie „O aplikacji”.
 */
class OpenFoodFactsClient {

    /** Produkt po kodzie kreskowym albo null, gdy nie ma go w bazie lub brakuje kalorii. */
    suspend fun productByBarcode(barcode: String): FoodProduct? = withContext(Dispatchers.IO) {
        val url = "https://world.openfoodfacts.org/api/v2/product/${URLEncoder.encode(barcode, "UTF-8")}" +
            "?fields=$FIELDS&lc=pl"
        val json = get(url) ?: return@withContext null
        if (json.optInt("status") != 1) return@withContext null
        json.optJSONObject("product")?.let { parse(it, barcode) }
    }

    /** Wyszukiwanie po nazwie (wolniejsze, wymaga internetu). */
    suspend fun search(query: String): List<FoodProduct> = withContext(Dispatchers.IO) {
        val url = "https://world.openfoodfacts.org/cgi/search.pl?search_terms=" +
            URLEncoder.encode(query, "UTF-8") +
            "&search_simple=1&action=process&json=1&page_size=25&lc=pl&fields=$FIELDS,code"
        val json = get(url) ?: return@withContext emptyList()
        val products = json.optJSONArray("products") ?: return@withContext emptyList()
        (0 until products.length()).mapNotNull { i ->
            val item = products.getJSONObject(i)
            parse(item, item.optString("code").ifBlank { null })
        }.distinctBy { it.name.lowercase() }
    }

    private fun parse(product: JSONObject, barcode: String?): FoodProduct? {
        val nutriments = product.optJSONObject("nutriments") ?: return null
        val kcal = nutriments.optDouble("energy-kcal_100g", Double.NaN).takeUnless { it.isNaN() }
            ?: nutriments.optDouble("energy_100g", Double.NaN).takeUnless { it.isNaN() }?.let { it / 4.184 }
            ?: return null
        val name = listOf("product_name_pl", "product_name", "generic_name_pl", "generic_name")
            .map { product.optString(it).trim() }
            .firstOrNull { it.isNotBlank() } ?: return null
        val brand = product.optString("brands").split(',').firstOrNull()?.trim().orEmpty()
        val fullName = if (brand.isNotBlank() && !name.contains(brand, ignoreCase = true)) "$name ($brand)" else name
        val quantity = product.optDouble("product_quantity", Double.NaN).takeUnless { it.isNaN() || it <= 0 }
        return FoodProduct(
            name = fullName,
            kcal = round1(kcal),
            protein = round1(nutriments.optDouble("proteins_100g", 0.0)),
            carbs = round1(nutriments.optDouble("carbohydrates_100g", 0.0)),
            fat = round1(nutriments.optDouble("fat_100g", 0.0)),
            unitName = if (quantity != null) "opak." else null,
            unitGrams = quantity,
            barcode = barcode,
            fiber = optional(nutriments, "fiber_100g"),
            sugar = optional(nutriments, "sugars_100g"),
            salt = optional(nutriments, "salt_100g")
        )
    }

    /** Wartość, jeśli producent ją podał (brak ≠ zero). */
    private fun optional(json: JSONObject, key: String): Double? =
        if (json.has(key)) json.optDouble(key, Double.NaN).takeUnless { it.isNaN() || it < 0 }?.let { round1(it) } else null

    private fun get(url: String): JSONObject? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("User-Agent", "MojaDieta-Android/1.3")
        }
        return try {
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                HttpURLConnection.HTTP_NOT_FOUND -> null
                else -> throw IOException("Open Food Facts: kod ${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun round1(value: Double) = Math.round(value * 10.0) / 10.0

    private companion object {
        const val FIELDS = "product_name,product_name_pl,generic_name,generic_name_pl,brands,nutriments,product_quantity"
    }
}

/** Wynik szukania po kodzie kreskowym. */
sealed interface BarcodeResult {
    data class Found(val product: FoodProduct) : BarcodeResult
    data class NotFound(val barcode: String) : BarcodeResult
    data class Error(val barcode: String, val message: String) : BarcodeResult
}

/**
 * Wspólna logika wyszukiwania produktów dla formularza przepisu i dziennika:
 * najpierw własna baza, potem Open Food Facts. Znalezione produkty zapisują się
 * w „Moich produktach”, więc kolejnym razem działają bez internetu.
 */
class ProductLookup(
    private val repository: ProductRepository,
    private val client: OpenFoodFactsClient
) {
    suspend fun findByBarcode(barcode: String): BarcodeResult {
        repository.findByBarcode(barcode)?.let { return BarcodeResult.Found(it) }
        return try {
            val online = client.productByBarcode(barcode) ?: return BarcodeResult.NotFound(barcode)
            BarcodeResult.Found(repository.addCustomProduct(online))
        } catch (e: Exception) {
            BarcodeResult.Error(barcode, "Brak połączenia z bazą produktów. Sprawdź internet.")
        }
    }

    suspend fun searchOnline(query: String): List<FoodProduct> = client.search(query)

    /** Zapisuje produkt w „Moich produktach” (jeśli jeszcze nie jest zapisany). */
    suspend fun save(product: FoodProduct): FoodProduct =
        if (product.isCustom) product else repository.addCustomProduct(product)
}
