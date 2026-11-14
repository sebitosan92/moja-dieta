package com.example.mojadieta.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.mojadieta.data.local.entity.CustomProductEntity
import com.example.mojadieta.data.local.entity.MealEntryEntity
import com.example.mojadieta.data.local.entity.RecipeIngredientEntity
import com.example.mojadieta.data.local.entity.NoteEntity
import com.example.mojadieta.data.local.entity.PlannedMealEntity
import com.example.mojadieta.data.local.entity.PlannedMealWithRecipe
import com.example.mojadieta.data.local.entity.RecipeEntity
import com.example.mojadieta.data.local.entity.WaterEntity
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
