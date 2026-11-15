package com.example.mojadieta.domain.settings

import com.example.mojadieta.domain.model.MealType
import java.time.DayOfWeek

enum class ThemeMode(val label: String) {
    SYSTEM("Jak w telefonie"),
    LIGHT("Jasny"),
    DARK("Ciemny")
}

enum class SplashMode(val label: String, val description: String) {
    DAILY("Pełny raz dziennie", "Cytat przy pierwszym uruchomieniu w danym dniu, potem krótko"),
    ALWAYS("Zawsze pełny", "Cytat przy każdym uruchomieniu"),
    SHORT("Zawsze krótki", "Samo logo, bez cytatu"),
    OFF("Wyłączony", "Aplikacja otwiera się od razu")
}

/** Godzina przypomnienia o posiłku. */
data class MealReminder(val enabled: Boolean, val hour: Int, val minute: Int)

/** Schematy postu przerywanego: godziny postu. */
val FASTING_SCHEMES = listOf(12, 14, 16, 18, 20)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val splashMode: SplashMode = SplashMode.DAILY,
    /** Nowe wpisy w dzienniku od razu zaznaczone jako zjedzone. */
    val newEntriesEaten: Boolean = false,
    val stepsGoal: Int = 8000,
    val weeklyActivityGoalMin: Int = 150,

    val weighReminder: Boolean = false,
    val weighDays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val weighHour: Int = 7,
    val weighMinute: Int = 30,

    val waterReminder: Boolean = false,
    val waterEveryHours: Int = 2,
    val waterFromHour: Int = 9,
    val waterToHour: Int = 21,

    val mealReminders: Map<MealType, MealReminder> = DEFAULT_MEAL_REMINDERS,

    val planningReminder: Boolean = false,
    val planningDay: DayOfWeek = DayOfWeek.SUNDAY,
    val planningHour: Int = 19,

    val weeklySummary: Boolean = true,

    val fastingEnabled: Boolean = false,
    val fastingHours: Int = 16,
    val eatingStartHour: Int = 12,
    val eatingStartMinute: Int = 0
) {
    val eatingWindowHours: Int get() = 24 - fastingHours

    companion object {
        val DEFAULT_MEAL_REMINDERS: Map<MealType, MealReminder> = mapOf(
            MealType.BREAKFAST to MealReminder(false, 8, 0),
            MealType.SECOND_BREAKFAST to MealReminder(false, 11, 0),
            MealType.LUNCH to MealReminder(false, 14, 0),
            MealType.AFTERNOON_SNACK to MealReminder(false, 17, 0),
            MealType.DINNER to MealReminder(false, 19, 30)
        )
    }
}
