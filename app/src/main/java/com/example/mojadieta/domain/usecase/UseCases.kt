package com.example.mojadieta.domain.usecase

import com.example.mojadieta.domain.model.MealEntry
import com.example.mojadieta.domain.model.MealType
import com.example.mojadieta.domain.model.NutritionSummary
import com.example.mojadieta.domain.model.Recipe
import com.example.mojadieta.domain.repository.DiaryRepository
import com.example.mojadieta.domain.repository.PlannerRepository
import java.time.LocalDate

/** Sumuje kalorie i makroskładniki z listy posiłków. */
class CalculateNutritionSummaryUseCase {
    operator fun invoke(meals: List<MealEntry>, onlyEaten: Boolean): NutritionSummary {
        val source = if (onlyEaten) meals.filter { it.isEaten } else meals
        return NutritionSummary(
            calories = source.sumOf { it.calories },
            protein = source.sumOf { it.protein },
            carbs = source.sumOf { it.carbs },
            fat = source.sumOf { it.fat }
        )
    }
}

/** Dodaje przepis jako wpis w dzienniku wybranego dnia. */
class AddRecipeToDiaryUseCase(private val diaryRepository: DiaryRepository) {
    suspend operator fun invoke(
        recipe: Recipe,
        date: LocalDate,
        mealType: MealType,
        eaten: Boolean = false
    ): Long = diaryRepository.addMeal(recipe.toMealEntry(date, mealType, eaten))
}

/**
 * Kopiuje zaplanowane posiłki z zakresu dat do dziennika.
 * Pomija pozycje, które już istnieją w dzienniku (ten sam przepis, dzień i pora),
 * aby wielokrotne kliknięcie nie tworzyło duplikatów. Zwraca liczbę skopiowanych posiłków.
 */
class CopyPlannedMealsToDiaryUseCase(
    private val plannerRepository: PlannerRepository,
    private val diaryRepository: DiaryRepository
) {
    suspend operator fun invoke(start: LocalDate, end: LocalDate): Int {
        val planned = plannerRepository.getPlannedMeals(start, end)
        val toInsert = mutableListOf<MealEntry>()
        planned.groupBy { it.date }.forEach { (date, plannedForDay) ->
            val existing = diaryRepository.getMeals(date)
                .mapNotNull { entry -> entry.recipeId?.let { it to entry.mealType } }
                .toMutableSet()
            plannedForDay.forEach { plannedMeal ->
                val key = plannedMeal.recipe.id to plannedMeal.mealType
                if (key !in existing) {
                    toInsert += plannedMeal.recipe.toMealEntry(date, plannedMeal.mealType, eaten = false)
                    existing += key
                }
            }
        }
        if (toInsert.isNotEmpty()) diaryRepository.addMeals(toInsert)
        return toInsert.size
    }
}

private fun Recipe.toMealEntry(date: LocalDate, mealType: MealType, eaten: Boolean) = MealEntry(
    date = date,
    mealType = mealType,
    name = name,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    recipeId = id,
    isEaten = eaten
)
