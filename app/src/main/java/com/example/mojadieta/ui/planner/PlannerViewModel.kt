package com.example.mojadieta.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mojadieta.domain.model.MealType
import com.example.mojadieta.domain.model.PlannedMeal
import com.example.mojadieta.domain.model.Recipe
import com.example.mojadieta.domain.repository.PlannerRepository
import com.example.mojadieta.domain.repository.RecipeRepository
import com.example.mojadieta.domain.usecase.CopyPlannedMealsToDiaryUseCase
import com.example.mojadieta.ui.util.dayMonth
import com.example.mojadieta.ui.util.startOfWeek
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PlannerDay(
    val date: LocalDate,
    val mealsByType: Map<MealType, List<PlannedMeal>>
) {
    val totalCalories: Int get() = mealsByType.values.flatten().sumOf { it.recipe.calories }
    val isEmpty: Boolean get() = mealsByType.values.all { it.isEmpty() }
}

data class PlannerUiState(
    val weekStart: LocalDate = LocalDate.now().startOfWeek(),
    val days: List<PlannerDay> = emptyList(),
    val recipes: List<Recipe> = emptyList()
) {
    val hasAnyPlanned: Boolean get() = days.any { !it.isEmpty }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PlannerViewModel(
    private val plannerRepository: PlannerRepository,
    recipeRepository: RecipeRepository,
    private val copyPlannedMealsToDiary: CopyPlannedMealsToDiaryUseCase
) : ViewModel() {

    private val weekStart = MutableStateFlow(LocalDate.now().startOfWeek())

    private val weekDays = weekStart.flatMapLatest { start ->
        plannerRepository.observePlannedMeals(start, start.plusDays(6)).map { planned ->
            val byDate = planned.groupBy { it.date }
            (0L..6L).map { offset ->
                val date = start.plusDays(offset)
                PlannerDay(
                    date = date,
                    mealsByType = byDate[date].orEmpty().groupBy { it.mealType }
                )
            }
        }.map { days -> start to days }
    }

    val uiState: StateFlow<PlannerUiState> = combine(
        weekDays,
        recipeRepository.observeRecipes()
    ) { (start, days), recipes ->
        PlannerUiState(weekStart = start, days = days, recipes = recipes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlannerUiState())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun previousWeek() = weekStart.update { it.minusDays(7) }

    fun nextWeek() = weekStart.update { it.plusDays(7) }

    fun goToWeekOf(date: LocalDate) {
        weekStart.value = date.startOfWeek()
    }

    fun currentWeek() {
        weekStart.value = LocalDate.now().startOfWeek()
    }

    fun addPlannedMeal(date: LocalDate, mealType: MealType, recipe: Recipe) {
        viewModelScope.launch {
            plannerRepository.addPlannedMeal(date, mealType, recipe.id)
        }
    }

    fun removePlannedMeal(plannedMeal: PlannedMeal) {
        viewModelScope.launch { plannerRepository.deletePlannedMeal(plannedMeal.id) }
    }

    fun copyDayToDiary(date: LocalDate) {
        viewModelScope.launch {
            val count = copyPlannedMealsToDiary(date, date)
            _messages.emit(
                if (count == 0) "Posiłki z ${date.dayMonth()} są już w dzienniku"
                else "Skopiowano do dziennika: ${count.mealsLabel()} (${date.dayMonth()})"
            )
        }
    }

    fun copyWeekToDiary() {
        val start = weekStart.value
        viewModelScope.launch {
            val count = copyPlannedMealsToDiary(start, start.plusDays(6))
            _messages.emit(
                if (count == 0) "Wszystkie zaplanowane posiłki z tego tygodnia są już w dzienniku"
                else "Skopiowano do dziennika: ${count.mealsLabel()} z całego tygodnia"
            )
        }
    }
}

/** Polska odmiana: 1 posiłek, 2–4 posiłki, 5+ posiłków (z wyjątkami 12–14). */
private fun Int.mealsLabel(): String {
    val lastTwo = this % 100
    val last = this % 10
    val word = when {
        this == 1 -> "posiłek"
        last in 2..4 && lastTwo !in 12..14 -> "posiłki"
        else -> "posiłków"
    }
    return "$this $word"
}
