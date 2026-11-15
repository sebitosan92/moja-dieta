package com.example.mojadieta.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.mojadieta.data.local.entity.ActivityEntity
import com.example.mojadieta.data.local.entity.ActivityTemplateEntity
import com.example.mojadieta.data.local.entity.BodyMetricEntity
import com.example.mojadieta.data.local.entity.DailyStepsEntity
import com.example.mojadieta.data.local.entity.DayTotalsRow
import com.example.mojadieta.data.local.entity.PlannedActivityEntity
import com.example.mojadieta.data.local.entity.ProductUsageEntity
import com.example.mojadieta.data.local.entity.ShoppingItemEntity
import com.example.mojadieta.data.local.entity.CustomProductEntity
import com.example.mojadieta.data.local.entity.MealEntryEntity
import com.example.mojadieta.data.local.entity.RecipeIngredientEntity
import com.example.mojadieta.data.local.entity.NoteEntity
import com.example.mojadieta.data.local.entity.PlannedMealEntity
import com.example.mojadieta.data.local.entity.PlannedMealWithRecipe
import com.example.mojadieta.data.local.entity.RecipeEntity
import com.example.mojadieta.data.local.entity.WaterEntity
import com.example.mojadieta.data.local.entity.WeightEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RecipeDao {
    @Query("SELECT * FROM recipes")
    abstract fun observeAll(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE id = :id")
    abstract fun observeById(id: Long): Flow<RecipeEntity?>

    @Query("SELECT * FROM recipes WHERE id = :id")
    abstract suspend fun getById(id: Long): RecipeEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insert(recipe: RecipeEntity): Long

    @Update
    abstract suspend fun update(recipe: RecipeEntity)

    @Query("DELETE FROM recipes WHERE id = :id")
    abstract suspend fun deleteById(id: Long)

    @Query("SELECT imagePath FROM recipes WHERE imagePath IS NOT NULL")
    abstract suspend fun imagePaths(): List<String>

    @Query("UPDATE recipes SET favorite = :favorite WHERE id = :id")
    abstract suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("SELECT * FROM recipe_ingredients WHERE recipeId IN (:recipeIds)")
    abstract suspend fun getIngredientsFor(recipeIds: List<Long>): List<RecipeIngredientEntity>

    // --- Składniki przepisu ---

    @Query("SELECT * FROM recipe_ingredients WHERE recipeId = :recipeId ORDER BY position, id")
    abstract fun observeIngredients(recipeId: Long): Flow<List<RecipeIngredientEntity>>

    @Query("SELECT * FROM recipe_ingredients WHERE recipeId = :recipeId ORDER BY position, id")
    abstract suspend fun getIngredients(recipeId: Long): List<RecipeIngredientEntity>

    @Insert
    abstract suspend fun insertIngredients(ingredients: List<RecipeIngredientEntity>)

    @Query("DELETE FROM recipe_ingredients WHERE recipeId = :recipeId")
    abstract suspend fun deleteIngredients(recipeId: Long)

    /** Zapis przepisu i pełnej listy składników w jednej transakcji. */
    @Transaction
    open suspend fun saveWithIngredients(
        recipe: RecipeEntity,
        ingredients: List<RecipeIngredientEntity>
    ): Long {
        val id = if (recipe.id == 0L) insert(recipe) else recipe.id.also { update(recipe) }
        deleteIngredients(id)
        if (ingredients.isNotEmpty()) {
            insertIngredients(ingredients.mapIndexed { index, item -> item.copy(id = 0, recipeId = id, position = index) })
        }
        return id
    }
}

@Dao
interface CustomProductDao {
    @Query("SELECT * FROM custom_products ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<CustomProductEntity>>

    @Insert
    suspend fun insert(product: CustomProductEntity): Long

    @Query("DELETE FROM custom_products WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM custom_products WHERE barcode = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): CustomProductEntity?
}

@Dao
interface MealEntryDao {
    @Query("SELECT * FROM meal_entries WHERE date = :date ORDER BY id")
    fun observeByDate(date: Long): Flow<List<MealEntryEntity>>

    @Query("SELECT * FROM meal_entries WHERE date = :date ORDER BY id")
    suspend fun getByDate(date: Long): List<MealEntryEntity>

    @Insert
    suspend fun insert(entry: MealEntryEntity): Long

    @Insert
    suspend fun insertAll(entries: List<MealEntryEntity>)

    @Query("UPDATE meal_entries SET eaten = :eaten WHERE id = :id")
    suspend fun setEaten(id: Long, eaten: Boolean)

    @Query("DELETE FROM meal_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Update
    suspend fun update(entry: MealEntryEntity)

    @Query("SELECT imagePath FROM meal_entries WHERE imagePath IS NOT NULL")
    suspend fun imagePaths(): List<String>

    @Query("SELECT * FROM meal_entries WHERE date = :date AND mealType = :mealType ORDER BY id")
    suspend fun getByDateAndMeal(date: Long, mealType: String): List<MealEntryEntity>

    @Query("UPDATE meal_entries SET eaten = :eaten WHERE date = :date AND mealType = :mealType")
    suspend fun setEatenForMeal(date: Long, mealType: String, eaten: Boolean)

    @Query("SELECT COUNT(*) FROM meal_entries WHERE date = :date AND mealType = :mealType")
    suspend fun countForMeal(date: Long, mealType: String): Int

    @Query(
        """
        SELECT date,
               CAST(COALESCE(SUM(CASE WHEN eaten THEN calories ELSE 0 END), 0) AS INTEGER) AS eatenCalories,
               COALESCE(SUM(CASE WHEN eaten THEN protein ELSE 0 END), 0) AS eatenProtein,
               COALESCE(SUM(CASE WHEN eaten THEN carbs ELSE 0 END), 0) AS eatenCarbs,
               COALESCE(SUM(CASE WHEN eaten THEN fat ELSE 0 END), 0) AS eatenFat,
               COUNT(*) AS entries,
               SUM(CASE WHEN eaten THEN 0 ELSE 1 END) AS notEaten
        FROM meal_entries WHERE date BETWEEN :start AND :end GROUP BY date ORDER BY date
        """
    )
    fun observeTotals(start: Long, end: Long): Flow<List<DayTotalsRow>>

    @Query(
        """
        SELECT date,
               CAST(COALESCE(SUM(CASE WHEN eaten THEN calories ELSE 0 END), 0) AS INTEGER) AS eatenCalories,
               COALESCE(SUM(CASE WHEN eaten THEN protein ELSE 0 END), 0) AS eatenProtein,
               COALESCE(SUM(CASE WHEN eaten THEN carbs ELSE 0 END), 0) AS eatenCarbs,
               COALESCE(SUM(CASE WHEN eaten THEN fat ELSE 0 END), 0) AS eatenFat,
               COUNT(*) AS entries,
               SUM(CASE WHEN eaten THEN 0 ELSE 1 END) AS notEaten
        FROM meal_entries WHERE date BETWEEN :start AND :end GROUP BY date ORDER BY date
        """
    )
    suspend fun getTotals(start: Long, end: Long): List<DayTotalsRow>
}

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_entries ORDER BY timeMillis DESC")
    fun observeAll(): Flow<List<WeightEntryEntity>>

    @Query("SELECT * FROM weight_entries ORDER BY timeMillis DESC LIMIT 1")
    fun observeLatest(): Flow<WeightEntryEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: WeightEntryEntity): Long

    @Query("DELETE FROM weight_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM weight_entries WHERE date BETWEEN :start AND :end ORDER BY timeMillis")
    suspend fun getBetween(start: Long, end: Long): List<WeightEntryEntity>

    @Query("SELECT COUNT(*) FROM weight_entries WHERE date = :date")
    suspend fun countOn(date: Long): Int
}

@Dao
interface PlannedMealDao {
    @Transaction
    @Query("SELECT * FROM planned_meals WHERE date BETWEEN :start AND :end ORDER BY date, id")
    fun observeBetween(start: Long, end: Long): Flow<List<PlannedMealWithRecipe>>

    @Transaction
    @Query("SELECT * FROM planned_meals WHERE date BETWEEN :start AND :end ORDER BY date, id")
    suspend fun getBetween(start: Long, end: Long): List<PlannedMealWithRecipe>

    @Insert
    suspend fun insert(plannedMeal: PlannedMealEntity): Long

    @Query("DELETE FROM planned_meals WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM planned_meals WHERE date BETWEEN :start AND :end")
    suspend fun countBetween(start: Long, end: Long): Int
}

@Dao
abstract class WaterDao {
    @Query("SELECT * FROM water_intake WHERE date = :date")
    abstract fun observe(date: Long): Flow<WaterEntity?>

    @Query("SELECT * FROM water_intake WHERE date = :date")
    abstract suspend fun get(date: Long): WaterEntity?

    /** Ostatnio ustawiony cel (z danego dnia lub wcześniejszego) – używany jako domyślny dla nowych dni. */
    @Query("SELECT goalMl FROM water_intake WHERE date <= :date ORDER BY date DESC LIMIT 1")
    abstract fun observeLatestGoal(date: Long): Flow<Int?>

    @Query("SELECT goalMl FROM water_intake WHERE date <= :date ORDER BY date DESC LIMIT 1")
    abstract suspend fun getLatestGoal(date: Long): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(entity: WaterEntity)

    @Query("SELECT * FROM water_intake WHERE date BETWEEN :start AND :end ORDER BY date")
    abstract fun observeBetween(start: Long, end: Long): Flow<List<WaterEntity>>

    @Query("SELECT * FROM water_intake WHERE date BETWEEN :start AND :end ORDER BY date")
    abstract suspend fun getBetween(start: Long, end: Long): List<WaterEntity>

    @Transaction
    open suspend fun changeAmount(date: Long, deltaMl: Int, defaultGoal: Int) {
        val current = get(date)
        if (current == null) {
            val goal = getLatestGoal(date) ?: defaultGoal
            upsert(WaterEntity(date = date, amountMl = deltaMl.coerceAtLeast(0), goalMl = goal))
        } else {
            upsert(current.copy(amountMl = (current.amountMl + deltaMl).coerceAtLeast(0)))
        }
    }

    @Transaction
    open suspend fun setGoal(date: Long, goalMl: Int) {
        val current = get(date)
        upsert(current?.copy(goalMl = goalMl) ?: WaterEntity(date = date, amountMl = 0, goalMl = goalMl))
    }
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities WHERE date = :date ORDER BY startMillis")
    fun observeByDate(date: Long): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE date BETWEEN :start AND :end ORDER BY startMillis")
    fun observeBetween(start: Long, end: Long): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE date BETWEEN :start AND :end ORDER BY startMillis")
    suspend fun getBetween(start: Long, end: Long): List<ActivityEntity>

    @Query("SELECT DISTINCT date FROM activities ORDER BY date DESC")
    fun observeActiveDays(): Flow<List<Long>>

    @Query("SELECT * FROM activities WHERE id = :id")
    suspend fun getById(id: Long): ActivityEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: ActivityEntity): Long

    @Update
    suspend fun update(entity: ActivityEntity)

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteById(id: Long)

    // --- Szablony ---
    @Query("SELECT * FROM activity_templates ORDER BY name COLLATE NOCASE")
    fun observeTemplates(): Flow<List<ActivityTemplateEntity>>

    @Insert
    suspend fun insertTemplate(entity: ActivityTemplateEntity): Long

    @Query("DELETE FROM activity_templates WHERE id = :id")
    suspend fun deleteTemplate(id: Long)

    // --- Plan ---
    @Query("SELECT * FROM planned_activities WHERE date BETWEEN :start AND :end ORDER BY date, id")
    fun observePlanned(start: Long, end: Long): Flow<List<PlannedActivityEntity>>

    @Insert
    suspend fun insertPlanned(entity: PlannedActivityEntity): Long

    @Query("DELETE FROM planned_activities WHERE id = :id")
    suspend fun deletePlanned(id: Long)

    @Query("SELECT * FROM planned_activities WHERE id = :id")
    suspend fun getPlanned(id: Long): PlannedActivityEntity?

    // --- Kroki ---
    @Query("SELECT * FROM daily_steps WHERE date = :date")
    fun observeSteps(date: Long): Flow<DailyStepsEntity?>

    @Query("SELECT * FROM daily_steps WHERE date BETWEEN :start AND :end ORDER BY date")
    fun observeStepsBetween(start: Long, end: Long): Flow<List<DailyStepsEntity>>

    @Query("SELECT * FROM daily_steps WHERE date BETWEEN :start AND :end ORDER BY date")
    suspend fun getStepsBetween(start: Long, end: Long): List<DailyStepsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSteps(entities: List<DailyStepsEntity>)
}

@Dao
abstract class ShoppingDao {
    @Query("SELECT * FROM shopping_items ORDER BY position, id")
    abstract fun observeAll(): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items")
    abstract suspend fun getAll(): List<ShoppingItemEntity>

    @Insert
    abstract suspend fun insert(entity: ShoppingItemEntity): Long

    @Insert
    abstract suspend fun insertAll(entities: List<ShoppingItemEntity>)

    @Query("UPDATE shopping_items SET checked = :checked WHERE id = :id")
    abstract suspend fun setChecked(id: Long, checked: Boolean)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    abstract suspend fun deleteById(id: Long)

    @Query("DELETE FROM shopping_items WHERE checked = 1")
    abstract suspend fun deleteChecked()

    @Query("DELETE FROM shopping_items WHERE manual = 0")
    abstract suspend fun deleteGenerated()

    @Query("DELETE FROM shopping_items")
    abstract suspend fun deleteAll()

    /** Zastępuje pozycje wygenerowane z planera, zachowując ręczne. */
    @Transaction
    open suspend fun replaceGenerated(items: List<ShoppingItemEntity>) {
        deleteGenerated()
        insertAll(items)
    }
}

@Dao
interface BodyMetricDao {
    @Query("SELECT * FROM body_metrics ORDER BY timeMillis DESC")
    fun observeAll(): Flow<List<BodyMetricEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: BodyMetricEntity): Long

    @Query("DELETE FROM body_metrics WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
abstract class ProductUsageDao {
    @Query("SELECT * FROM product_usage WHERE favorite = 1 ORDER BY name COLLATE NOCASE")
    abstract fun observeFavorites(): Flow<List<ProductUsageEntity>>

    @Query("SELECT * FROM product_usage WHERE count > 0 ORDER BY lastUsed DESC LIMIT :limit")
    abstract fun observeRecent(limit: Int): Flow<List<ProductUsageEntity>>

    @Query("SELECT * FROM product_usage WHERE `key` = :key")
    abstract suspend fun get(key: String): ProductUsageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(entity: ProductUsageEntity)

    @Transaction
    open suspend fun recordUse(key: String, name: String, now: Long) {
        val current = get(key)
        upsert(
            current?.copy(name = name, count = current.count + 1, lastUsed = now)
                ?: ProductUsageEntity(key, name, 1, now, false)
        )
    }

    @Transaction
    open suspend fun toggleFavorite(key: String, name: String, now: Long) {
        val current = get(key)
        upsert(
            current?.copy(favorite = !current.favorite)
                ?: ProductUsageEntity(key, name, 0, now, true)
        )
    }
}
