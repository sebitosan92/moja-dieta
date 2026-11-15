package com.example.mojadieta.data.repository

import android.content.Context
import androidx.core.content.edit
import com.example.mojadieta.data.food.FoodCatalog
import com.example.mojadieta.data.local.dao.ActivityDao
import com.example.mojadieta.data.local.dao.BodyMetricDao
import com.example.mojadieta.data.local.dao.ProductUsageDao
import com.example.mojadieta.data.local.dao.ShoppingDao
import com.example.mojadieta.data.local.entity.DailyStepsEntity
import com.example.mojadieta.data.local.entity.ShoppingItemEntity
import com.example.mojadieta.data.mapper.toDomain
import com.example.mojadieta.data.mapper.toEntity
import com.example.mojadieta.domain.model.ActivityEntry
import com.example.mojadieta.domain.model.ActivityTemplate
import com.example.mojadieta.domain.model.BodyMetric
import com.example.mojadieta.domain.model.BodyMetricType
import com.example.mojadieta.domain.model.DailySteps
import com.example.mojadieta.domain.model.PlannedActivity
import com.example.mojadieta.domain.model.ProductUsage
import com.example.mojadieta.domain.model.ShoppingItem
import com.example.mojadieta.domain.model.WeightEntry
import com.example.mojadieta.domain.repository.PlannerRepository
import com.example.mojadieta.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

// ---------------- Aktywność ----------------

class ActivityRepository(context: Context, private val dao: ActivityDao) {

    /** Id treningów z Health Connect usuniętych przez użytkownika – nie importujemy ich ponownie. */
    private val prefs = context.getSharedPreferences("activity", Context.MODE_PRIVATE)

    fun observeDay(date: LocalDate): Flow<List<ActivityEntry>> =
        dao.observeByDate(date.toEpochDay()).map { list -> list.map { it.toDomain() } }

    fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<ActivityEntry>> =
        dao.observeBetween(start.toEpochDay(), end.toEpochDay()).map { list -> list.map { it.toDomain() } }

    suspend fun getBetween(start: LocalDate, end: LocalDate): List<ActivityEntry> =
        dao.getBetween(start.toEpochDay(), end.toEpochDay()).map { it.toDomain() }

    fun observeActiveDays(): Flow<Set<LocalDate>> =
        dao.observeActiveDays().map { days -> days.map { LocalDate.ofEpochDay(it) }.toSet() }

    suspend fun get(id: Long): ActivityEntry? = dao.getById(id)?.toDomain()

    suspend fun save(entry: ActivityEntry): Long =
        if (entry.id == 0L) dao.insert(entry.toEntity()) else entry.id.also { dao.update(entry.toEntity()) }

    suspend fun delete(entry: ActivityEntry) {
        entry.externalId?.let { id ->
            val dismissed = prefs.getStringSet(KEY_DISMISSED, emptySet()).orEmpty() + id
            prefs.edit { putStringSet(KEY_DISMISSED, dismissed) }
        }
        dao.deleteById(entry.id)
    }

    /** Przywraca usunięty wpis (cofnięcie z paska „Cofnij”). */
    suspend fun restore(entry: ActivityEntry) {
        entry.externalId?.let { id ->
            val dismissed = prefs.getStringSet(KEY_DISMISSED, emptySet()).orEmpty() - id
            prefs.edit { putStringSet(KEY_DISMISSED, dismissed) }
        }
        dao.insert(entry.toEntity())
    }

    /** Zapisuje treningi z Health Connect, pomijając znane i usunięte. Zwraca liczbę nowych. */
    suspend fun importExternal(entries: List<ActivityEntry>): Int {
        val dismissed = prefs.getStringSet(KEY_DISMISSED, emptySet()).orEmpty()
        var added = 0
        entries.filter { it.externalId !in dismissed }.forEach {
            if (dao.insert(it.copy(id = 0).toEntity()) > 0) added++
        }
        return added
    }

    // Szablony
    fun observeTemplates(): Flow<List<ActivityTemplate>> =
        dao.observeTemplates().map { list -> list.map { it.toDomain() } }

    suspend fun addTemplate(template: ActivityTemplate) = dao.insertTemplate(template.toEntity())
    suspend fun deleteTemplate(id: Long) = dao.deleteTemplate(id)

    // Plan treningów
    fun observePlanned(start: LocalDate, end: LocalDate): Flow<List<PlannedActivity>> =
        dao.observePlanned(start.toEpochDay(), end.toEpochDay()).map { list -> list.map { it.toDomain() } }

    suspend fun addPlanned(planned: PlannedActivity) = dao.insertPlanned(planned.toEntity())
    suspend fun deletePlanned(id: Long) = dao.deletePlanned(id)

    // Kroki
    fun observeSteps(date: LocalDate): Flow<DailySteps?> = dao.observeSteps(date.toEpochDay()).map { it?.toDomain() }

    fun observeStepsBetween(start: LocalDate, end: LocalDate): Flow<List<DailySteps>> =
        dao.observeStepsBetween(start.toEpochDay(), end.toEpochDay()).map { list -> list.map { it.toDomain() } }

    suspend fun getStepsBetween(start: LocalDate, end: LocalDate): List<DailySteps> =
        dao.getStepsBetween(start.toEpochDay(), end.toEpochDay()).map { it.toDomain() }

    suspend fun saveSteps(days: List<DailySteps>) = dao.upsertSteps(
        days.map { DailyStepsEntity(it.date.toEpochDay(), it.steps, it.activeCalories, it.distanceKm) }
    )

    private fun DailyStepsEntity.toDomain() =
        DailySteps(LocalDate.ofEpochDay(date), steps, activeCalories, distanceKm)

    private companion object {
        const val KEY_DISMISSED = "dismissed_external"
    }
}

// ---------------- Skład ciała i obwody ----------------

class BodyMetricRepository(private val dao: BodyMetricDao) {
    fun observeAll(): Flow<List<BodyMetric>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun addManual(type: BodyMetricType, value: Double, date: LocalDate) {
        val time = if (date == LocalDate.now()) System.currentTimeMillis()
        else date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        dao.insert(
            BodyMetric(date = date, timeMillis = time, type = type, value = value, source = WeightEntry.SOURCE_MANUAL)
                .toEntity()
        )
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    suspend fun importExternal(metrics: List<BodyMetric>): Int {
        var added = 0
        metrics.forEach { if (dao.insert(it.copy(id = 0).toEntity()) > 0) added++ }
        return added
    }
}

// ---------------- Ostatnie i ulubione produkty ----------------

class ProductUsageRepository(private val dao: ProductUsageDao) {
    fun observeRecent(limit: Int = 12): Flow<List<ProductUsage>> =
        dao.observeRecent(limit).map { list -> list.map { ProductUsage(it.name, it.count, it.lastUsed, it.favorite) } }

    fun observeFavorites(): Flow<List<ProductUsage>> =
        dao.observeFavorites().map { list -> list.map { ProductUsage(it.name, it.count, it.lastUsed, it.favorite) } }

    suspend fun recordUse(name: String) =
        dao.recordUse(FoodCatalog.normalize(name), name.trim(), System.currentTimeMillis())

    suspend fun toggleFavorite(name: String) =
        dao.toggleFavorite(FoodCatalog.normalize(name), name.trim(), System.currentTimeMillis())
}

// ---------------- Lista zakupów ----------------

class ShoppingRepository(
    private val dao: ShoppingDao,
    private val plannerRepository: PlannerRepository,
    private val recipeRepository: RecipeRepository
) {
    fun observeItems(): Flow<List<ShoppingItem>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun addManual(name: String, quantity: String) {
        val position = (dao.getAll().maxOfOrNull { it.position } ?: 0) + 1
        dao.insert(
            ShoppingItemEntity(
                name = name.trim(),
                quantity = quantity.trim(),
                groupName = FoodCatalog.groupFor(name),
                checked = false,
                manual = true,
                position = position
            )
        )
    }

    suspend fun setChecked(id: Long, checked: Boolean) = dao.setChecked(id, checked)
    suspend fun delete(id: Long) = dao.deleteById(id)
    suspend fun deleteChecked() = dao.deleteChecked()
    suspend fun clear() = dao.deleteAll()

    /**
     * Tworzy listę z posiłków zaplanowanych w zakresie dat. Każdy zaplanowany posiłek to jedna porcja,
     * więc gramatura składnika = gramy w przepisie ÷ liczba porcji. Przepisy bez składników z gramaturą
     * dają pozycje z tekstu listy składników. Ręczne pozycje zostają, odhaczenia przenoszą się po nazwie.
     * Zwraca liczbę pozycji wygenerowanych.
     */
    suspend fun generate(start: LocalDate, end: LocalDate): Int {
        val planned = plannerRepository.getPlannedMeals(start, end)
        val ingredientsByRecipe = recipeRepository.getIngredientsFor(planned.map { it.recipe.id }.toSet())

        val grams = linkedMapOf<String, Pair<String, Double>>() // klucz → (nazwa, gramy)
        val textLines = linkedMapOf<String, Pair<String, Int>>() // klucz → (tekst, ile razy)
        planned.forEach { meal ->
            val ingredients = ingredientsByRecipe[meal.recipe.id].orEmpty()
            if (ingredients.isNotEmpty()) {
                val portions = meal.recipe.servings.coerceAtLeast(1)
                ingredients.forEach { ing ->
                    val key = FoodCatalog.normalize(ing.name)
                    val prev = grams[key]
                    grams[key] = (prev?.first ?: ing.name.trim()) to ((prev?.second ?: 0.0) + ing.grams / portions)
                }
            } else {
                meal.recipe.ingredients.lines()
                    .map { it.trim().trimStart('-', '•', '*', '–').trim() }
                    .filter { it.isNotEmpty() }
                    .forEach { line ->
                        val key = FoodCatalog.normalize(line)
                        val prev = textLines[key]
                        textLines[key] = (prev?.first ?: line) to ((prev?.second ?: 0) + 1)
                    }
            }
        }

        val previous = dao.getAll()
        val checkedNames = previous.filter { it.checked }.map { FoodCatalog.normalize(it.name) }.toSet()
        val items = mutableListOf<ShoppingItemEntity>()
        grams.forEach { (key, value) ->
            items += ShoppingItemEntity(
                name = value.first,
                quantity = formatGrams(value.second, value.first),
                groupName = FoodCatalog.groupFor(value.first),
                checked = key in checkedNames,
                manual = false,
                position = 0
            )
        }
        textLines.forEach { (key, value) ->
            items += ShoppingItemEntity(
                name = value.first,
                quantity = if (value.second > 1) "×${value.second}" else "",
                groupName = FoodCatalog.groupFor(value.first),
                checked = key in checkedNames,
                manual = false,
                position = 0
            )
        }
        val ordered = items.sortedWith(
            compareBy<ShoppingItemEntity> { groupIndex(it.groupName) }.thenBy { FoodCatalog.normalize(it.name) }
        ).mapIndexed { index, item -> item.copy(position = index) }
        dao.replaceGenerated(ordered)
        return ordered.size
    }

    private fun groupIndex(group: String): Int =
        FoodCatalog.GROUP_ORDER.indexOf(group).let { if (it < 0) FoodCatalog.GROUP_ORDER.size else it }

    /** „350 g”, „1,2 kg”, a dla produktów liczonych w sztukach dodatkowo „(~3 szt.)”. */
    private fun formatGrams(value: Double, name: String): String {
        val base = if (value >= 1000) String.format(Locale.forLanguageTag("pl"), "%.1f kg", value / 1000.0)
        else "${roundNice(value)} g"
        val product = FoodCatalog.products.firstOrNull { FoodCatalog.normalize(it.name) == FoodCatalog.normalize(name) }
        val unitGrams = product?.unitGrams
        return if (product?.unitName == "szt." && unitGrams != null && unitGrams > 0) {
            val pieces = Math.ceil(value / unitGrams).toInt().coerceAtLeast(1)
            "$base (~$pieces szt.)"
        } else base
    }

    private fun roundNice(value: Double): Int = when {
        value < 20 -> Math.round(value).toInt().coerceAtLeast(1)
        value < 200 -> (Math.round(value / 5.0) * 5).toInt()
        else -> (Math.round(value / 10.0) * 10).toInt()
    }

    companion object {
        /** Lista w formie tekstu do udostępnienia (pomija odhaczone). */
        fun shareText(items: List<ShoppingItem>): String = buildString {
            append("🛒 Lista zakupów – Moja Dieta\n")
            items.filter { !it.checked }
                .groupBy { it.group }
                .toSortedMap(compareBy { g -> FoodCatalog.GROUP_ORDER.indexOf(g).let { if (it < 0) 99 else it } })
                .forEach { (group, list) ->
                    append("\n").append(group).append(":\n")
                    list.forEach { item ->
                        append("• ").append(item.name)
                        if (item.quantity.isNotBlank()) append(" – ").append(item.quantity)
                        append("\n")
                    }
                }
        }.trimEnd()
    }
}
