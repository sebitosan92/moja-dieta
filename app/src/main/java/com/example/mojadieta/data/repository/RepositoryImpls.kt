package com.example.mojadieta.data.repository

import com.example.mojadieta.data.food.FoodCatalog
import com.example.mojadieta.data.local.dao.CustomProductDao
import com.example.mojadieta.data.local.dao.MealEntryDao
import com.example.mojadieta.data.local.dao.NoteDao
import com.example.mojadieta.data.local.dao.PlannedMealDao
import com.example.mojadieta.data.local.dao.RecipeDao
import com.example.mojadieta.data.local.dao.WaterDao
import com.example.mojadieta.data.local.dao.WeightDao
import com.example.mojadieta.data.local.entity.CustomProductEntity
import com.example.mojadieta.data.local.entity.PlannedMealEntity
import com.example.mojadieta.data.local.entity.WeightEntryEntity
import com.example.mojadieta.data.mapper.toDomain
import com.example.mojadieta.data.mapper.toDomainOrNull
import com.example.mojadieta.data.mapper.toEntity
import com.example.mojadieta.domain.model.FoodProduct
import com.example.mojadieta.domain.model.MealEntry
import com.example.mojadieta.domain.model.RecipeIngredient
import com.example.mojadieta.domain.model.MealType
import com.example.mojadieta.domain.model.Note
import com.example.mojadieta.domain.model.PlannedMeal
import com.example.mojadieta.domain.model.Recipe
import com.example.mojadieta.domain.model.WaterIntake
import com.example.mojadieta.domain.model.WeightEntry
import com.example.mojadieta.domain.repository.DiaryRepository
import com.example.mojadieta.domain.repository.NoteRepository
import com.example.mojadieta.domain.repository.PlannerRepository
import com.example.mojadieta.domain.repository.ProfileRepository
import com.example.mojadieta.domain.repository.WeightRepository
import com.example.mojadieta.domain.repository.ProductRepository
import com.example.mojadieta.domain.repository.RecipeRepository
import com.example.mojadieta.domain.repository.WaterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class RecipeRepositoryImpl(private val dao: RecipeDao) : RecipeRepository {
    override fun observeRecipes(): Flow<List<Recipe>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeRecipe(id: Long): Flow<Recipe?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun getRecipe(id: Long): Recipe? = dao.getById(id)?.toDomain()

    override suspend fun saveRecipe(recipe: Recipe): Long =
        if (recipe.id == 0L) {
            dao.insert(recipe.toEntity())
        } else {
            dao.update(recipe.toEntity())
            recipe.id
        }

    override suspend fun deleteRecipe(id: Long) = dao.deleteById(id)

    override fun observeIngredients(recipeId: Long): Flow<List<RecipeIngredient>> =
        dao.observeIngredients(recipeId).map { list -> list.map { it.toDomain() } }

    override suspend fun getIngredients(recipeId: Long): List<RecipeIngredient> =
        dao.getIngredients(recipeId).map { it.toDomain() }

    override suspend fun imagePaths(): List<String> = dao.imagePaths()

    override suspend fun saveRecipeWithIngredients(recipe: Recipe, ingredients: List<RecipeIngredient>): Long =
        dao.saveWithIngredients(
            recipe.toEntity(),
            ingredients.mapIndexed { index, item -> item.toEntity(recipe.id, index) }
        )
}

class ProductRepositoryImpl(private val dao: CustomProductDao) : ProductRepository {
    override fun observeProducts(): Flow<List<FoodProduct>> =
        dao.observeAll().map { custom -> custom.map { it.toDomain() } + FoodCatalog.products }

    override suspend fun addCustomProduct(product: FoodProduct): FoodProduct {
        val id = dao.insert(
            CustomProductEntity(
                name = product.name.trim(),
                kcal = product.kcal,
                protein = product.protein,
                carbs = product.carbs,
                fat = product.fat,
                barcode = product.barcode
            )
        )
        return product.copy(name = product.name.trim(), customId = id)
    }

    override suspend fun deleteCustomProduct(id: Long) = dao.deleteById(id)

    override suspend fun findByBarcode(barcode: String): FoodProduct? = dao.findByBarcode(barcode)?.toDomain()
}

class DiaryRepositoryImpl(private val dao: MealEntryDao) : DiaryRepository {
    override fun observeMeals(date: LocalDate): Flow<List<MealEntry>> =
        dao.observeByDate(date.toEpochDay()).map { list -> list.map { it.toDomain() } }

    override suspend fun getMeals(date: LocalDate): List<MealEntry> =
        dao.getByDate(date.toEpochDay()).map { it.toDomain() }

    override suspend fun addMeal(entry: MealEntry): Long = dao.insert(entry.copy(id = 0).toEntity())

    override suspend fun addMeals(entries: List<MealEntry>) =
        dao.insertAll(entries.map { it.copy(id = 0).toEntity() })

    override suspend fun setEaten(id: Long, eaten: Boolean) = dao.setEaten(id, eaten)

    override suspend fun deleteMeal(id: Long) = dao.deleteById(id)

    override suspend fun updateMeal(entry: MealEntry) = dao.update(entry.toEntity())

    override suspend fun imagePaths(): List<String> = dao.imagePaths()
}

class WeightRepositoryImpl(
    private val dao: WeightDao,
    private val profileRepository: ProfileRepository
) : WeightRepository {
    override fun observeEntries(): Flow<List<WeightEntry>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeLatest(): Flow<WeightEntry?> = dao.observeLatest().map { it?.toDomain() }

    override suspend fun addManual(date: LocalDate, weightKg: Double) {
        // Godzina: teraz dla dzisiejszego dnia, południe dla dni z przeszłości.
        val time = if (date == LocalDate.now()) System.currentTimeMillis()
        else date.atTime(12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        dao.insert(
            WeightEntryEntity(
                date = date.toEpochDay(),
                timeMillis = time,
                weightKg = weightKg,
                source = WeightEntry.SOURCE_MANUAL,
                externalId = null
            )
        )
        syncProfileWeight()
    }

    override suspend fun delete(id: Long) = dao.deleteById(id)

    override suspend fun importExternal(entries: List<WeightEntry>): Int {
        var added = 0
        entries.forEach { if (dao.insert(it.copy(id = 0).toEntity()) > 0) added++ }
        if (added > 0) syncProfileWeight()
        return added
    }

    /** Najnowsza waga trafia też do profilu, żeby kalkulator celu był aktualny. */
    private suspend fun syncProfileWeight() {
        dao.observeLatest().first()?.let { profileRepository.updateWeight(it.weightKg) }
    }
}

class PlannerRepositoryImpl(private val dao: PlannedMealDao) : PlannerRepository {
    override fun observePlannedMeals(start: LocalDate, end: LocalDate): Flow<List<PlannedMeal>> =
        dao.observeBetween(start.toEpochDay(), end.toEpochDay())
            .map { list -> list.mapNotNull { it.toDomainOrNull() } }

    override suspend fun getPlannedMeals(start: LocalDate, end: LocalDate): List<PlannedMeal> =
        dao.getBetween(start.toEpochDay(), end.toEpochDay()).mapNotNull { it.toDomainOrNull() }

    override suspend fun addPlannedMeal(date: LocalDate, mealType: MealType, recipeId: Long): Long =
        dao.insert(
            PlannedMealEntity(
                date = date.toEpochDay(),
                mealType = mealType.name,
                recipeId = recipeId
            )
        )

    override suspend fun deletePlannedMeal(id: Long) = dao.deleteById(id)
}

class WaterRepositoryImpl(private val dao: WaterDao) : WaterRepository {
    override fun observeWater(date: LocalDate): Flow<WaterIntake> {
        val day = date.toEpochDay()
        return combine(dao.observe(day), dao.observeLatestGoal(day)) { entity, latestGoal ->
            WaterIntake(
                date = date,
                amountMl = entity?.amountMl ?: 0,
                goalMl = entity?.goalMl ?: latestGoal ?: WaterIntake.DEFAULT_GOAL_ML
            )
        }
    }

    override suspend fun changeAmount(date: LocalDate, deltaMl: Int) =
        dao.changeAmount(date.toEpochDay(), deltaMl, WaterIntake.DEFAULT_GOAL_ML)

    override suspend fun setGoal(date: LocalDate, goalMl: Int) =
        dao.setGoal(date.toEpochDay(), goalMl)
}

class NoteRepositoryImpl(private val dao: NoteDao) : NoteRepository {
    override fun observeNotes(): Flow<List<Note>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getNote(id: Long): Note? = dao.getById(id)?.toDomain()

    override suspend fun saveNote(note: Note): Long =
        if (note.id == 0L) {
            dao.insert(note.toEntity())
        } else {
            dao.update(note.toEntity())
            note.id
        }

    override suspend fun deleteNote(id: Long) = dao.deleteById(id)
}
