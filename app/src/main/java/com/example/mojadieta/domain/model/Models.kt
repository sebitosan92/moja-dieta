package com.example.mojadieta.domain.model

import java.time.LocalDate

/** Pora posiłku w dzienniku i planerze. */
enum class MealType(val label: String) {
    BREAKFAST("Śniadanie"),
    SECOND_BREAKFAST("II Śniadanie"),
    LUNCH("Obiad"),
    AFTERNOON_SNACK("Podwieczorek"),
    DINNER("Kolacja");

    companion object {
        fun fromName(name: String): MealType =
            entries.firstOrNull { it.name == name } ?: BREAKFAST
    }
}

/** Kategoria przepisu w bazie przepisów. */
enum class RecipeCategory(val label: String) {
    BREAKFAST("Śniadania"),
    LUNCH("Obiady"),
    DINNER("Kolacje"),
    SNACK("Przekąski");

    companion object {
        fun fromName(name: String): RecipeCategory =
            entries.firstOrNull { it.name == name } ?: BREAKFAST
    }
}

data class Recipe(
    val id: Long = 0,
    val name: String,
    val category: RecipeCategory,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val ingredients: String,
    val instructions: String
) {
    val ingredientList: List<String>
        get() = ingredients.lines().map { it.trim().removePrefix("-").trim() }.filter { it.isNotBlank() }

    val instructionSteps: List<String>
        get() = instructions.lines().map { it.trim() }.filter { it.isNotBlank() }
}

data class MealEntry(
    val id: Long = 0,
    val date: LocalDate,
    val mealType: MealType,
    val name: String,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val recipeId: Long? = null,
    val isEaten: Boolean = false
)

data class PlannedMeal(
    val id: Long = 0,
    val date: LocalDate,
    val mealType: MealType,
    val recipe: Recipe
)

data class WaterIntake(
    val date: LocalDate,
    val amountMl: Int,
    val goalMl: Int
) {
    val progress: Float
        get() = if (goalMl <= 0) 0f else (amountMl.toFloat() / goalMl).coerceIn(0f, 1f)

    companion object {
        const val DEFAULT_GOAL_ML = 2500
    }
}

data class Note(
    val id: Long = 0,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
)

data class NutritionSummary(
    val calories: Int = 0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0
)
