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
    val instructions: String,
    /** Na ile porcji jest przepis – wartości odżywcze są podawane na 1 porcję. */
    val servings: Int = 1,
    /** true = kalorie i makro wyliczane automatycznie ze składników z gramaturą. */
    val autoNutrition: Boolean = false,
    /** Ścieżka do zdjęcia w pamięci aplikacji. */
    val imagePath: String? = null
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
    val isEaten: Boolean = false,
    /** Liczba porcji – kalorie i makro wpisu są już przemnożone przez tę wartość. */
    val servings: Double = 1.0,
    val imagePath: String? = null
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

/**
 * Produkt spożywczy z wartościami na 100 g.
 * [unitName]/[unitGrams] – typowa porcja, np. „szt.” = 50 g dla jajka.
 */
data class FoodProduct(
    val name: String,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val unitName: String? = null,
    val unitGrams: Double? = null,
    val customId: Long? = null,
    val barcode: String? = null
) {
    val isCustom: Boolean get() = customId != null
}

/** Składnik przepisu z gramaturą. Wartości na 100 g są zapisane jako kopia z chwili dodania. */
data class RecipeIngredient(
    val id: Long = 0,
    val name: String,
    val grams: Double,
    val kcal100: Double,
    val protein100: Double,
    val carbs100: Double,
    val fat100: Double
) {
    val calories: Double get() = kcal100 * grams / 100.0
    val protein: Double get() = protein100 * grams / 100.0
    val carbs: Double get() = carbs100 * grams / 100.0
    val fat: Double get() = fat100 * grams / 100.0

    companion object {
        fun from(product: FoodProduct, grams: Double) = RecipeIngredient(
            name = product.name,
            grams = grams,
            kcal100 = product.kcal,
            protein100 = product.protein,
            carbs100 = product.carbs,
            fat100 = product.fat
        )
    }
}

data class NutritionSummary(
    val calories: Int = 0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0
)

/** Pomiar wagi – wpisany ręcznie albo zaimportowany z Health Connect. */
data class WeightEntry(
    val id: Long = 0,
    val date: LocalDate,
    val timeMillis: Long,
    val weightKg: Double,
    /** "manual" albo nazwa pakietu aplikacji źródłowej z Health Connect. */
    val source: String,
    val externalId: String? = null
) {
    val isManual: Boolean get() = source == SOURCE_MANUAL

    companion object {
        const val SOURCE_MANUAL = "manual"
    }
}

enum class Sex(val label: String) { FEMALE("Kobieta"), MALE("Mężczyzna") }

enum class ActivityLevel(val label: String, val description: String, val factor: Double) {
    SEDENTARY("Bardzo niska", "Praca siedząca, brak treningów", 1.2),
    LIGHT("Niska", "Lekkie ćwiczenia 1–3 razy w tygodniu", 1.375),
    MODERATE("Umiarkowana", "Treningi 3–5 razy w tygodniu", 1.55),
    HIGH("Wysoka", "Intensywne treningi 6–7 razy w tygodniu", 1.725),
    VERY_HIGH("Bardzo wysoka", "Praca fizyczna i codzienne treningi", 1.9)
}

enum class GoalType(val label: String, val kcalModifier: Double, val proteinPerKg: Double) {
    LOSE("Redukcja", -0.20, 2.0),
    LOSE_SLOW("Łagodna redukcja", -0.10, 1.8),
    MAINTAIN("Utrzymanie wagi", 0.0, 1.6),
    GAIN("Budowa masy", 0.10, 1.8)
}

/** Profil użytkownika do obliczenia zapotrzebowania. */
data class UserProfile(
    val sex: Sex = Sex.FEMALE,
    val age: Int = 30,
    val heightCm: Int = 165,
    val weightKg: Double = 65.0,
    val activity: ActivityLevel = ActivityLevel.LIGHT,
    val goal: GoalType = GoalType.MAINTAIN,
    /** true = cele wpisane ręcznie zamiast wyliczonych. */
    val manualGoals: Boolean = false,
    val manualCalories: Int = 2000,
    val manualProtein: Int = 100,
    val manualCarbs: Int = 250,
    val manualFat: Int = 65
)

/** Dzienny cel: kalorie i makroskładniki w gramach. */
data class DailyGoal(
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int
)
