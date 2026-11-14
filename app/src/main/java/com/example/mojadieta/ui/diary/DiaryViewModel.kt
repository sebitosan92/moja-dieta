package com.example.mojadieta.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mojadieta.domain.model.MealEntry
import com.example.mojadieta.domain.model.MealType
import com.example.mojadieta.domain.model.NutritionSummary
import com.example.mojadieta.domain.model.Recipe
import com.example.mojadieta.domain.model.WaterIntake
import com.example.mojadieta.domain.repository.DiaryRepository
import com.example.mojadieta.domain.repository.RecipeRepository
import com.example.mojadieta.domain.repository.WaterRepository
import com.example.mojadieta.domain.usecase.AddRecipeToDiaryUseCase
import com.example.mojadieta.domain.usecase.CalculateNutritionSummaryUseCase
import com.example.mojadieta.ui.util.startOfWeek
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DiaryUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val weekStart: LocalDate = LocalDate.now().startOfWeek(),
    val mealsByType: Map<MealType, List<MealEntry>> = emptyMap(),
    val eatenSummary: NutritionSummary = NutritionSummary(),
    val totalSummary: NutritionSummary = NutritionSummary(),
    val water: WaterIntake = WaterIntake(LocalDate.now(), 0, WaterIntake.DEFAULT_GOAL_ML),
    val recipes: List<Recipe> = emptyList()
)

private data class DayData(
    val date: LocalDate,
    val meals: List<MealEntry>,
    val water: WaterIntake
)

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryViewModel(
    private val diaryRepository: DiaryRepository,
    private val waterRepository: WaterRepository,
    recipeRepository: RecipeRepository,
    private val addRecipeToDiary: AddRecipeToDiaryUseCase,
    private val calculateNutritionSummary: CalculateNutritionSummaryUseCase
) : ViewModel() {

    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val weekStart = MutableStateFlow(LocalDate.now().startOfWeek())

    private val dayData = selectedDate.flatMapLatest { date ->
        combine(
            diaryRepository.observeMeals(date),
            waterRepository.observeWater(date)
        ) { meals, water -> DayData(date, meals, water) }
    }

    val uiState: StateFlow<DiaryUiState> = combine(
        dayData,
        weekStart,
        recipeRepository.observeRecipes()
    ) { day, week, recipes ->
        DiaryUiState(
            selectedDate = day.date,
            weekStart = week,
            mealsByType = day.meals.groupBy { it.mealType },
            eatenSummary = calculateNutritionSummary(day.meals, onlyEaten = true),
            totalSummary = calculateNutritionSummary(day.meals, onlyEaten = false),
            water = day.water,
            recipes = recipes
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DiaryUiState()
    )

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
        weekStart.value = date.startOfWeek()
    }

    fun previousWeek() = shiftWeek(-7)

    fun nextWeek() = shiftWeek(7)

    fun goToToday() = selectDate(LocalDate.now())

    private fun shiftWeek(days: Long) {
        weekStart.update { it.plusDays(days) }
        selectedDate.update { it.plusDays(days) }
    }

    fun addRecipeMeal(recipe: Recipe, mealType: MealType) {
        viewModelScope.launch {
            addRecipeToDiary(recipe, selectedDate.value, mealType)
        }
    }

    fun addManualMeal(
        name: String,
        mealType: MealType,
        calories: Int,
        protein: Double,
        carbs: Double,
        fat: Double
    ) {
        viewModelScope.launch {
            diaryRepository.addMeal(
                MealEntry(
                    date = selectedDate.value,
                    mealType = mealType,
                    name = name.trim(),
                    calories = calories,
                    protein = protein,
                    carbs = carbs,
                    fat = fat
                )
            )
        }
    }

    fun toggleEaten(entry: MealEntry) {
        viewModelScope.launch { diaryRepository.setEaten(entry.id, !entry.isEaten) }
    }

    fun deleteMeal(entry: MealEntry) {
        viewModelScope.launch { diaryRepository.deleteMeal(entry.id) }
    }

    fun changeWater(deltaMl: Int) {
        viewModelScope.launch { waterRepository.changeAmount(selectedDate.value, deltaMl) }
    }

    fun setWaterGoal(goalMl: Int) {
        if (goalMl <= 0) return
        viewModelScope.launch { waterRepository.setGoal(selectedDate.value, goalMl) }
    }
}
