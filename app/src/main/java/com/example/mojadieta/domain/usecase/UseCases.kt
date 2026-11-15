package com.example.mojadieta.domain.usecase

import com.example.mojadieta.domain.model.DailyGoal
import com.example.mojadieta.domain.model.MealEntry
import com.example.mojadieta.domain.model.Sex
import com.example.mojadieta.domain.model.UserProfile
import com.example.mojadieta.domain.model.MealType
import com.example.mojadieta.domain.model.NutritionSummary
import com.example.mojadieta.domain.model.Recipe
import com.example.mojadieta.domain.model.RecipeIngredient
import com.example.mojadieta.domain.repository.DiaryRepository
import com.example.mojadieta.domain.repository.PlannerRepository
import java.time.LocalDate

/** Sumuje kalorie i makroskładniki z listy posiłków. */
class CalculateNutritionSummaryUseCase {
    operator fun invoke(meals: List<MealEntry>, onlyEaten: Boolean): NutritionSummary {
        val source = if (onlyEaten) meals.filter { it.isEaten } else meals
        val fiber = source.mapNotNull { it.fiber }
        val sugar = source.mapNotNull { it.sugar }
        val salt = source.mapNotNull { it.salt }
        val anyExtras = fiber.isNotEmpty() || sugar.isNotEmpty() || salt.isNotEmpty()
        return NutritionSummary(
            calories = source.sumOf { it.calories },
            protein = source.sumOf { it.protein },
            carbs = source.sumOf { it.carbs },
            fat = source.sumOf { it.fat },
            fiber = fiber.takeIf { it.isNotEmpty() }?.sum(),
            sugar = sugar.takeIf { it.isNotEmpty() }?.sum(),
            salt = salt.takeIf { it.isNotEmpty() }?.sum(),
            extrasIncomplete = anyExtras && source.any { it.fiber == null && it.sugar == null && it.salt == null }
        )
    }
}

/** Suma znanych wartości albo null, gdy żaden element nie ma danych. */
fun <T> List<T>.sumKnown(selector: (T) -> Double?): Double? {
    val known = mapNotNull(selector)
    return if (known.isEmpty()) null else known.sum()
}

/** Wynik przeliczenia przepisu: cały przepis i jedna porcja. */
data class RecipeNutrition(
    val total: NutritionSummary,
    val perServing: NutritionSummary
)

/** Sumuje wartości odżywcze składników i dzieli przez liczbę porcji. */
class CalculateRecipeNutritionUseCase {
    operator fun invoke(ingredients: List<RecipeIngredient>, servings: Int): RecipeNutrition {
        val portions = servings.coerceAtLeast(1)
        val kcal = ingredients.sumOf { it.calories }
        val protein = ingredients.sumOf { it.protein }
        val carbs = ingredients.sumOf { it.carbs }
        val fat = ingredients.sumOf { it.fat }
        val fiber = ingredients.sumKnown { it.fiber }
        val sugar = ingredients.sumKnown { it.sugar }
        val salt = ingredients.sumKnown { it.salt }
        val incomplete = (fiber != null || sugar != null || salt != null) &&
            ingredients.any { it.fiber100 == null && it.sugar100 == null && it.salt100 == null }
        return RecipeNutrition(
            total = NutritionSummary(
                calories = Math.round(kcal).toInt(),
                protein = round1(protein),
                carbs = round1(carbs),
                fat = round1(fat),
                fiber = fiber?.let(::round1),
                sugar = sugar?.let(::round1),
                salt = salt?.let { round2(it) },
                extrasIncomplete = incomplete
            ),
            perServing = NutritionSummary(
                calories = Math.round(kcal / portions).toInt(),
                protein = round1(protein / portions),
                carbs = round1(carbs / portions),
                fat = round1(fat / portions),
                fiber = fiber?.let { round1(it / portions) },
                sugar = sugar?.let { round1(it / portions) },
                salt = salt?.let { round2(it / portions) },
                extrasIncomplete = incomplete
            )
        )
    }

    private fun round2(value: Double): Double = Math.round(value * 100.0) / 100.0

    private fun round1(value: Double): Double = Math.round(value * 10.0) / 10.0
}

/** Zmienia liczbę porcji wpisu, przeliczając kalorie i makro proporcjonalnie. */
fun MealEntry.scaledTo(newServings: Double): MealEntry {
    val factor = newServings / servings.coerceAtLeast(0.01)
    return copy(
        servings = newServings,
        calories = Math.round(calories * factor).toInt(),
        protein = Math.round(protein * factor * 10.0) / 10.0,
        carbs = Math.round(carbs * factor * 10.0) / 10.0,
        fat = Math.round(fat * factor * 10.0) / 10.0,
        fiber = fiber?.let { Math.round(it * factor * 10.0) / 10.0 },
        sugar = sugar?.let { Math.round(it * factor * 10.0) / 10.0 },
        salt = salt?.let { Math.round(it * factor * 100.0) / 100.0 }
    )
}

/**
 * Dzienne zapotrzebowanie: BMR wzorem Mifflina-St Jeora × aktywność, skorygowane o cel.
 * Białko w g/kg masy ciała, tłuszcze ~27% kalorii, reszta to węglowodany.
 */
class CalculateDailyGoalUseCase {
    data class Result(val bmr: Int, val tdee: Int, val goal: DailyGoal)

    operator fun invoke(profile: UserProfile): Result {
        val base = 10 * profile.weightKg + 6.25 * profile.heightCm - 5 * profile.age
        val bmr = if (profile.sex == Sex.MALE) base + 5 else base - 161
        val tdee = bmr * profile.activity.factor
        val calculated = run {
            val kcal = (tdee * (1 + profile.goal.kcalModifier)).coerceAtLeast(1000.0)
            val protein = profile.weightKg * profile.goal.proteinPerKg
            val fat = kcal * 0.27 / 9.0
            val carbs = ((kcal - protein * 4 - fat * 9) / 4.0).coerceAtLeast(0.0)
            DailyGoal(
                calories = (Math.round(kcal / 10.0) * 10).toInt(),
                protein = Math.round(protein).toInt(),
                carbs = Math.round(carbs).toInt(),
                fat = Math.round(fat).toInt()
            )
        }
        val goal = if (profile.manualGoals) {
            DailyGoal(profile.manualCalories, profile.manualProtein, profile.manualCarbs, profile.manualFat)
        } else {
            calculated
        }
        return Result(Math.round(bmr).toInt(), Math.round(tdee).toInt(), goal)
    }
}

/** Dodaje przepis jako wpis w dzienniku wybranego dnia. */
class AddRecipeToDiaryUseCase(private val diaryRepository: DiaryRepository) {
    suspend operator fun invoke(
        recipe: Recipe,
        date: LocalDate,
        mealType: MealType,
        eaten: Boolean = false,
        servings: Double = 1.0
    ): Long = diaryRepository.addMeal(recipe.toMealEntry(date, mealType, eaten).scaledTo(servings))
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
    isEaten = eaten,
    imagePath = null,
    fiber = fiber,
    sugar = sugar,
    salt = salt
)
