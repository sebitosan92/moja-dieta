package com.example.mojadieta.data.settings

import android.content.Context
import androidx.core.content.edit
import com.example.mojadieta.domain.model.MealType
import com.example.mojadieta.domain.settings.AppSettings
import com.example.mojadieta.domain.settings.MealReminder
import com.example.mojadieta.domain.settings.SplashMode
import com.example.mojadieta.domain.settings.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.DayOfWeek
import java.time.LocalDate

/** Ustawienia aplikacji w SharedPreferences, udostępniane jako StateFlow. */
class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(load())

    val settings: StateFlow<AppSettings> = state.asStateFlow()

    val current: AppSettings get() = state.value

    fun update(transform: (AppSettings) -> AppSettings) {
        val updated = transform(state.value)
        save(updated)
        state.value = updated
    }

    /** Ekran startowy z cytatem raz dziennie: zwraca true, jeśli dziś jeszcze go nie było. */
    fun consumeDailyFullSplash(): Boolean {
        val today = LocalDate.now().toEpochDay()
        val last = prefs.getLong(KEY_LAST_FULL_SPLASH, -1)
        if (last == today) return false
        prefs.edit { putLong(KEY_LAST_FULL_SPLASH, today) }
        return true
    }

    private fun load(): AppSettings {
        val d = AppSettings()
        val meals = MealType.entries.associateWith { type ->
            val def = d.mealReminders.getValue(type)
            MealReminder(
                enabled = prefs.getBoolean("meal_${type.name}_on", def.enabled),
                hour = prefs.getInt("meal_${type.name}_h", def.hour),
                minute = prefs.getInt("meal_${type.name}_m", def.minute)
            )
        }
        return AppSettings(
            themeMode = enumOr(prefs.getString("theme", null), d.themeMode),
            splashMode = enumOr(prefs.getString("splash", null), d.splashMode),
            newEntriesEaten = prefs.getBoolean("new_eaten", d.newEntriesEaten),
            stepsGoal = prefs.getInt("steps_goal", d.stepsGoal),
            weeklyActivityGoalMin = prefs.getInt("activity_goal", d.weeklyActivityGoalMin),
            weighReminder = prefs.getBoolean("weigh_on", d.weighReminder),
            weighDays = prefs.getStringSet("weigh_days", null)
                ?.mapNotNull { name -> DayOfWeek.entries.firstOrNull { it.name == name } }?.toSet()
                ?: d.weighDays,
            weighHour = prefs.getInt("weigh_h", d.weighHour),
            weighMinute = prefs.getInt("weigh_m", d.weighMinute),
            waterReminder = prefs.getBoolean("water_on", d.waterReminder),
            waterEveryHours = prefs.getInt("water_every", d.waterEveryHours),
            waterFromHour = prefs.getInt("water_from", d.waterFromHour),
            waterToHour = prefs.getInt("water_to", d.waterToHour),
            mealReminders = meals,
            planningReminder = prefs.getBoolean("plan_on", d.planningReminder),
            planningDay = enumOr(prefs.getString("plan_day", null), d.planningDay),
            planningHour = prefs.getInt("plan_h", d.planningHour),
            weeklySummary = prefs.getBoolean("summary_on", d.weeklySummary),
            fastingEnabled = prefs.getBoolean("fast_on", d.fastingEnabled),
            fastingHours = prefs.getInt("fast_hours", d.fastingHours),
            eatingStartHour = prefs.getInt("eat_h", d.eatingStartHour),
            eatingStartMinute = prefs.getInt("eat_m", d.eatingStartMinute)
        )
    }

    private fun save(s: AppSettings) = prefs.edit {
        putString("theme", s.themeMode.name)
        putString("splash", s.splashMode.name)
        putBoolean("new_eaten", s.newEntriesEaten)
        putInt("steps_goal", s.stepsGoal)
        putInt("activity_goal", s.weeklyActivityGoalMin)
        putBoolean("weigh_on", s.weighReminder)
        putStringSet("weigh_days", s.weighDays.map { it.name }.toSet())
        putInt("weigh_h", s.weighHour)
        putInt("weigh_m", s.weighMinute)
        putBoolean("water_on", s.waterReminder)
        putInt("water_every", s.waterEveryHours)
        putInt("water_from", s.waterFromHour)
        putInt("water_to", s.waterToHour)
        s.mealReminders.forEach { (type, r) ->
            putBoolean("meal_${type.name}_on", r.enabled)
            putInt("meal_${type.name}_h", r.hour)
            putInt("meal_${type.name}_m", r.minute)
        }
        putBoolean("plan_on", s.planningReminder)
        putString("plan_day", s.planningDay.name)
        putInt("plan_h", s.planningHour)
        putBoolean("summary_on", s.weeklySummary)
        putBoolean("fast_on", s.fastingEnabled)
        putInt("fast_hours", s.fastingHours)
        putInt("eat_h", s.eatingStartHour)
        putInt("eat_m", s.eatingStartMinute)
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private companion object {
        const val KEY_LAST_FULL_SPLASH = "last_full_splash"
    }
}
