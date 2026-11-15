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
    val imagePath: String? = null,
    val favorite: Boolean = false,
    /** Tagi, np. „szybkie”, „wege”. */
    val tags: List<String> = emptyList(),
    /** Na porcję; null = brak danych. */
    val fiber: Double? = null,
    val sugar: Double? = null,
    val salt: Double? = null
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
    val imagePath: String? = null,
    val fiber: Double? = null,
    val sugar: Double? = null,
    val salt: Double? = null
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
    val barcode: String? = null,
    /** Na 100 g; null = brak danych. */
    val fiber: Double? = null,
    val sugar: Double? = null,
    val salt: Double? = null,
    /** Dział sklepu do listy zakupów, np. „Warzywa”. */
    val group: String? = null
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
    val fat100: Double,
    val fiber100: Double? = null,
    val sugar100: Double? = null,
    val salt100: Double? = null
) {
    val calories: Double get() = kcal100 * grams / 100.0
    val protein: Double get() = protein100 * grams / 100.0
    val carbs: Double get() = carbs100 * grams / 100.0
    val fat: Double get() = fat100 * grams / 100.0
    val fiber: Double? get() = fiber100?.times(grams / 100.0)
    val sugar: Double? get() = sugar100?.times(grams / 100.0)
    val salt: Double? get() = salt100?.times(grams / 100.0)

    companion object {
        fun from(product: FoodProduct, grams: Double) = RecipeIngredient(
            name = product.name,
            grams = grams,
            kcal100 = product.kcal,
            protein100 = product.protein,
            carbs100 = product.carbs,
            fat100 = product.fat,
            fiber100 = product.fiber,
            sugar100 = product.sugar,
            salt100 = product.salt
        )
    }
}

data class NutritionSummary(
    val calories: Int = 0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    /** Suma znanych wartości; null = brak danych w żadnym wpisie. */
    val fiber: Double? = null,
    val sugar: Double? = null,
    val salt: Double? = null,
    /** true = część wpisów nie ma danych o błonniku/cukrach/soli. */
    val extrasIncomplete: Boolean = false
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

// ---------------- Aktywność ----------------

enum class Intensity(val label: String) { LIGHT("Lekka"), MODERATE("Umiarkowana"), VIGOROUS("Intensywna") }

/** Rodzaj aktywności z wartościami MET dla trzech intensywności. */
enum class ActivityType(
    val label: String,
    val metLight: Double,
    val metModerate: Double,
    val metVigorous: Double,
    val hasDistance: Boolean = false
) {
    CYCLING("Jazda na rowerze", 4.0, 6.8, 10.0, true),
    STATIONARY_BIKE("Rower stacjonarny", 3.5, 6.8, 8.8),
    WALKING("Spacer", 2.8, 3.5, 5.0, true),
    RUNNING("Bieg", 7.0, 9.8, 11.5, true),
    NORDIC_WALKING("Nordic walking", 4.8, 5.5, 6.8, true),
    HIKING("Wędrówka", 5.3, 6.0, 7.8, true),
    SWIMMING("Pływanie", 5.8, 7.0, 9.8, true),
    GYM("Siłownia", 3.5, 5.0, 6.0),
    FITNESS("Fitness / aerobik", 5.0, 7.3, 8.5),
    YOGA("Joga / stretching", 2.0, 2.5, 4.0),
    ROLLER_SKATING("Rolki", 5.0, 7.0, 9.8, true),
    DANCING("Taniec", 4.0, 5.5, 7.8),
    TENNIS("Tenis / badminton", 5.0, 7.0, 8.0),
    GARDENING("Prace w ogrodzie", 3.0, 4.0, 5.0),
    OTHER("Inna aktywność", 3.0, 4.5, 6.5);

    fun met(intensity: Intensity) = when (intensity) {
        Intensity.LIGHT -> metLight
        Intensity.MODERATE -> metModerate
        Intensity.VIGOROUS -> metVigorous
    }

    companion object {
        fun fromName(name: String?) = entries.firstOrNull { it.name == name } ?: OTHER
    }
}

data class ActivityEntry(
    val id: Long = 0,
    val date: LocalDate,
    val startMillis: Long,
    val type: ActivityType,
    /** Własna nazwa (np. z Health Connect albo „Inna aktywność”). */
    val title: String? = null,
    val durationMin: Int,
    val intensity: Intensity = Intensity.MODERATE,
    val distanceKm: Double? = null,
    val avgHeartRate: Int? = null,
    val watts: Int? = null,
    val calories: Int,
    val note: String = "",
    /** "manual" albo pakiet aplikacji źródłowej z Health Connect. */
    val source: String = SOURCE_MANUAL,
    val externalId: String? = null
) {
    val isManual: Boolean get() = source == SOURCE_MANUAL
    val displayName: String get() = title?.takeIf { it.isNotBlank() } ?: type.label

    companion object {
        const val SOURCE_MANUAL = "manual"
    }
}

/** Ulubiony trening do szybkiego dodawania. */
data class ActivityTemplate(
    val id: Long = 0,
    val name: String,
    val type: ActivityType,
    val durationMin: Int,
    val intensity: Intensity,
    val distanceKm: Double? = null
)

data class PlannedActivity(
    val id: Long = 0,
    val date: LocalDate,
    val type: ActivityType,
    val durationMin: Int,
    val intensity: Intensity,
    val note: String = ""
)

// ---------------- Lista zakupów ----------------

data class ShoppingItem(
    val id: Long = 0,
    val name: String,
    val quantity: String,
    val group: String,
    val checked: Boolean = false,
    /** true = dodana ręcznie (nie znika przy ponownym generowaniu z planera). */
    val manual: Boolean = false,
    val position: Int = 0
)

// ---------------- Skład ciała i obwody ----------------

enum class BodyMetricType(val label: String, val unit: String, val isComposition: Boolean) {
    BODY_FAT("Tkanka tłuszczowa", "%", true),
    LEAN_MASS("Masa beztłuszczowa", "kg", true),
    BODY_WATER("Woda w organizmie", "kg", true),
    BONE_MASS("Masa kostna", "kg", true),
    WAIST("Talia", "cm", false),
    HIPS("Biodra", "cm", false),
    CHEST("Klatka piersiowa", "cm", false),
    THIGH("Udo", "cm", false),
    ARM("Ramię", "cm", false);

    companion object {
        fun fromName(name: String?) = entries.firstOrNull { it.name == name } ?: WAIST
    }
}

data class BodyMetric(
    val id: Long = 0,
    val date: LocalDate,
    val timeMillis: Long,
    val type: BodyMetricType,
    val value: Double,
    val source: String = WeightEntry.SOURCE_MANUAL,
    val externalId: String? = null
)

/** Suma dnia do kropek w kalendarzu i statystyk. */
data class DayTotals(
    val date: LocalDate,
    val eatenCalories: Int,
    val eatenProtein: Double,
    val eatenCarbs: Double,
    val eatenFat: Double,
    val entries: Int,
    /** Wpisy jeszcze niezaznaczone jako zjedzone. */
    val notEaten: Int = 0
)

/** Kroki, aktywne kalorie i dystans z Health Connect. */
data class DailySteps(
    val date: LocalDate,
    val steps: Int,
    val activeCalories: Int,
    val distanceKm: Double
)

/** Statystyka użycia produktu (ostatnio używane, ulubione). */
data class ProductUsage(
    val name: String,
    val count: Int,
    val lastUsed: Long,
    val favorite: Boolean
)
