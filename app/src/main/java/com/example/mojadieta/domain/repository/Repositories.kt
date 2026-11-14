package com.example.mojadieta.domain.repository

import com.example.mojadieta.domain.model.FoodProduct
import com.example.mojadieta.domain.model.MealEntry
import com.example.mojadieta.domain.model.RecipeIngredient
import com.example.mojadieta.domain.model.MealType
import com.example.mojadieta.domain.model.Note
import com.example.mojadieta.domain.model.PlannedMeal
import com.example.mojadieta.domain.model.Recipe
import com.example.mojadieta.domain.model.WaterIntake
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface RecipeRepository {
    fun observeRecipes(): Flow<List<Recipe>>
    fun observeRecipe(id: Long): Flow<Recipe?>
    suspend fun getRecipe(id: Long): Recipe?
    /** Zapisuje przepis (nowy lub istniejący) i zwraca jego identyfikator. */
    suspend fun saveRecipe(recipe: Recipe): Long
    suspend fun deleteRecipe(id: Long)

    fun observeIngredients(recipeId: Long): Flow<List<RecipeIngredient>>
    suspend fun getIngredients(recipeId: Long): List<RecipeIngredient>
    /** Zapisuje przepis razem ze składnikami (w jednej transakcji) i zwraca id przepisu. */
    suspend fun saveRecipeWithIngredients(recipe: Recipe, ingredients: List<RecipeIngredient>): Long
}

interface ProductRepository {
    /** Wbudowana baza produktów + własne produkty użytkownika. */
    fun observeProducts(): Flow<List<FoodProduct>>
    suspend fun addCustomProduct(product: FoodProduct): FoodProduct
    suspend fun deleteCustomProduct(id: Long)
}

interface DiaryRepository {
    fun observeMeals(date: LocalDate): Flow<List<MealEntry>>
    suspend fun getMeals(date: LocalDate): List<MealEntry>
    suspend fun addMeal(entry: MealEntry): Long
    suspend fun addMeals(entries: List<MealEntry>)
    suspend fun setEaten(id: Long, eaten: Boolean)
    suspend fun deleteMeal(id: Long)
}

interface PlannerRepository {
    fun observePlannedMeals(start: LocalDate, end: LocalDate): Flow<List<PlannedMeal>>
    suspend fun getPlannedMeals(start: LocalDate, end: LocalDate): List<PlannedMeal>
    suspend fun addPlannedMeal(date: LocalDate, mealType: MealType, recipeId: Long): Long
    suspend fun deletePlannedMeal(id: Long)
}

interface WaterRepository {
    fun observeWater(date: LocalDate): Flow<WaterIntake>
    suspend fun changeAmount(date: LocalDate, deltaMl: Int)
    suspend fun setGoal(date: LocalDate, goalMl: Int)
}

interface NoteRepository {
    fun observeNotes(): Flow<List<Note>>
    suspend fun getNote(id: Long): Note?
    suspend fun saveNote(note: Note): Long
    suspend fun deleteNote(id: Long)
}
