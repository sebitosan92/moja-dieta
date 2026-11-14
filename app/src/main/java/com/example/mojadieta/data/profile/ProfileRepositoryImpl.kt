package com.example.mojadieta.data.profile

import android.content.Context
import androidx.core.content.edit
import com.example.mojadieta.domain.model.UserProfile
import com.example.mojadieta.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Profil i cele trzymane w SharedPreferences (jeden użytkownik na telefonie). */
class ProfileRepositoryImpl(context: Context) : ProfileRepository {

    private val prefs = context.getSharedPreferences("profile", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(load())

    override fun observeProfile(): Flow<UserProfile?> = state.asStateFlow()

    override suspend fun saveProfile(profile: UserProfile) {
        prefs.edit {
            putBoolean(KEY_SET, true)
            putString("sex", profile.sex.name)
            putInt("age", profile.age)
            putInt("height", profile.heightCm)
            putFloat("weight", profile.weightKg.toFloat())
            putString("activity", profile.activity.name)
            putString("goal", profile.goal.name)
            putBoolean("manual", profile.manualGoals)
            putInt("manualKcal", profile.manualCalories)
            putInt("manualProtein", profile.manualProtein)
            putInt("manualCarbs", profile.manualCarbs)
            putInt("manualFat", profile.manualFat)
        }
        state.value = profile
    }

    override suspend fun updateWeight(weightKg: Double) {
        val current = state.value ?: return // profil jeszcze nieuzupełniony – nic nie nadpisujemy
        if (current.weightKg != weightKg) saveProfile(current.copy(weightKg = weightKg))
    }

    private fun load(): UserProfile? {
        if (!prefs.getBoolean(KEY_SET, false)) return null
        val defaults = UserProfile()
        return UserProfile(
            sex = enumOr(prefs.getString("sex", null), defaults.sex),
            age = prefs.getInt("age", defaults.age),
            heightCm = prefs.getInt("height", defaults.heightCm),
            weightKg = prefs.getFloat("weight", defaults.weightKg.toFloat()).toDouble(),
            activity = enumOr(prefs.getString("activity", null), defaults.activity),
            goal = enumOr(prefs.getString("goal", null), defaults.goal),
            manualGoals = prefs.getBoolean("manual", false),
            manualCalories = prefs.getInt("manualKcal", defaults.manualCalories),
            manualProtein = prefs.getInt("manualProtein", defaults.manualProtein),
            manualCarbs = prefs.getInt("manualCarbs", defaults.manualCarbs),
            manualFat = prefs.getInt("manualFat", defaults.manualFat)
        )
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private companion object {
        const val KEY_SET = "profile_set"
    }
}
