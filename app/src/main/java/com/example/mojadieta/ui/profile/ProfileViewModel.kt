package com.example.mojadieta.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mojadieta.domain.model.ActivityLevel
import com.example.mojadieta.domain.model.GoalType
import com.example.mojadieta.domain.model.Sex
import com.example.mojadieta.domain.model.UserProfile
import com.example.mojadieta.domain.repository.ProfileRepository
import com.example.mojadieta.domain.repository.WeightRepository
import com.example.mojadieta.domain.usecase.CalculateDailyGoalUseCase
import com.example.mojadieta.ui.util.formatGrams
import com.example.mojadieta.ui.util.parseDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileFormState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val sex: Sex = Sex.FEMALE,
    val age: String = "",
    val height: String = "",
    val weight: String = "",
    val activity: ActivityLevel = ActivityLevel.LIGHT,
    val goal: GoalType = GoalType.MAINTAIN,
    val manualGoals: Boolean = false,
    val manualCalories: String = "",
    val manualProtein: String = "",
    val manualCarbs: String = "",
    val manualFat: String = "",
    val result: CalculateDailyGoalUseCase.Result? = null
) {
    val ageValue: Int? get() = age.trim().toIntOrNull()?.takeIf { it in 12..100 }
    val heightValue: Int? get() = height.trim().toIntOrNull()?.takeIf { it in 100..230 }
    val weightValue: Double? get() = parseDecimal(weight)?.takeIf { it in 30.0..300.0 }

    fun toProfile(): UserProfile? {
        val a = ageValue ?: return null
        val h = heightValue ?: return null
        val w = weightValue ?: return null
        return UserProfile(
            sex = sex,
            age = a,
            heightCm = h,
            weightKg = w,
            activity = activity,
            goal = goal,
            manualGoals = manualGoals,
            manualCalories = manualCalories.trim().toIntOrNull() ?: result?.goal?.calories ?: 2000,
            manualProtein = manualProtein.trim().toIntOrNull() ?: result?.goal?.protein ?: 100,
            manualCarbs = manualCarbs.trim().toIntOrNull() ?: result?.goal?.carbs ?: 250,
            manualFat = manualFat.trim().toIntOrNull() ?: result?.goal?.fat ?: 65
        )
    }
}

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val weightRepository: WeightRepository,
    private val calculateDailyGoal: CalculateDailyGoalUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileFormState())
    val state: StateFlow<ProfileFormState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = profileRepository.observeProfile().first()
            val latestWeight = weightRepository.observeLatest().first()?.weightKg
            val profile = saved ?: UserProfile(weightKg = latestWeight ?: UserProfile().weightKg)
            _state.value = ProfileFormState(
                isLoading = false,
                isNew = saved == null,
                sex = profile.sex,
                age = if (saved == null) "" else profile.age.toString(),
                height = if (saved == null) "" else profile.heightCm.toString(),
                weight = if (saved == null && latestWeight == null) "" else formatGrams(latestWeight ?: profile.weightKg),
                activity = profile.activity,
                goal = profile.goal,
                manualGoals = profile.manualGoals,
                manualCalories = profile.manualCalories.toString(),
                manualProtein = profile.manualProtein.toString(),
                manualCarbs = profile.manualCarbs.toString(),
                manualFat = profile.manualFat.toString()
            ).recalculated()
        }
    }

    private fun ProfileFormState.recalculated(): ProfileFormState {
        val profile = toProfile() ?: return copy(result = null)
        // Wynik pokazujemy zawsze dla wyliczenia automatycznego; cele ręczne są osobno.
        return copy(result = calculateDailyGoal(profile.copy(manualGoals = false)))
    }

    private fun edit(block: (ProfileFormState) -> ProfileFormState) = _state.update { block(it).recalculated() }

    fun onSexChange(value: Sex) = edit { it.copy(sex = value) }
    fun onAgeChange(value: String) = edit { it.copy(age = value.filter(Char::isDigit).take(3)) }
    fun onHeightChange(value: String) = edit { it.copy(height = value.filter(Char::isDigit).take(3)) }
    fun onWeightChange(value: String) =
        edit { it.copy(weight = value.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(5)) }
    fun onActivityChange(value: ActivityLevel) = edit { it.copy(activity = value) }
    fun onGoalChange(value: GoalType) = edit { it.copy(goal = value) }

    fun onManualGoalsChange(enabled: Boolean) = edit { current ->
        // Przy włączaniu trybu ręcznego wpisujemy wyliczone wartości jako punkt wyjścia.
        val calculated = current.result?.goal
        if (enabled && calculated != null) {
            current.copy(
                manualGoals = true,
                manualCalories = calculated.calories.toString(),
                manualProtein = calculated.protein.toString(),
                manualCarbs = calculated.carbs.toString(),
                manualFat = calculated.fat.toString()
            )
        } else {
            current.copy(manualGoals = enabled)
        }
    }

    fun onManualCaloriesChange(value: String) = edit { it.copy(manualCalories = value.filter(Char::isDigit).take(5)) }
    fun onManualProteinChange(value: String) = edit { it.copy(manualProtein = value.filter(Char::isDigit).take(4)) }
    fun onManualCarbsChange(value: String) = edit { it.copy(manualCarbs = value.filter(Char::isDigit).take(4)) }
    fun onManualFatChange(value: String) = edit { it.copy(manualFat = value.filter(Char::isDigit).take(4)) }

    fun save(onSaved: () -> Unit) {
        val profile = _state.value.toProfile() ?: return
        viewModelScope.launch {
            profileRepository.saveProfile(profile)
            onSaved()
        }
    }
}
