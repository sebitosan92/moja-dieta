package com.example.mojadieta.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mojadieta.data.food.ProductLookup
import com.example.mojadieta.data.images.ImageStorage
import com.example.mojadieta.domain.model.DailyGoal
import com.example.mojadieta.domain.model.FoodProduct
import com.example.mojadieta.domain.model.MealEntry
import com.example.mojadieta.domain.model.MealType
import com.example.mojadieta.domain.model.NutritionSummary
import com.example.mojadieta.domain.model.Recipe
import com.example.mojadieta.domain.model.RecipeIngredient
import com.example.mojadieta.domain.model.WaterIntake
import com.example.mojadieta.domain.model.WeightEntry
import com.example.mojadieta.domain.repository.DiaryRepository
import com.example.mojadieta.domain.repository.ProductRepository
import com.example.mojadieta.domain.repository.ProfileRepository
import com.example.mojadieta.domain.repository.RecipeRepository
import com.example.mojadieta.domain.repository.WaterRepository
import com.example.mojadieta.domain.repository.WeightRepository
import com.example.mojadieta.domain.usecase.AddRecipeToDiaryUseCase
import com.example.mojadieta.domain.usecase.CalculateDailyGoalUseCase
import com.example.mojadieta.domain.usecase.CalculateNutritionSummaryUseCase
import com.example.mojadieta.domain.usecase.scaledTo
import com.example.mojadieta.ui.util.formatGrams
import com.example.mojadieta.ui.util.startOfWeek
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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
    val recipes: List<Recipe> = emptyList(),
    /** Dzienny cel z profilu albo null, gdy profil nie jest uzupełniony. */
    val goal: DailyGoal? = null,
    val latestWeight: WeightEntry? = null
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
    productRepository: ProductRepository,
    profileRepository: ProfileRepository,
    weightRepository: WeightRepository,
    private val addRecipeToDiary: AddRecipeToDiaryUseCase,
    private val calculateNutritionSummary: CalculateNutritionSummaryUseCase,
    calculateDailyGoal: CalculateDailyGoalUseCase,
    val productLookup: ProductLookup,
    val imageStorage: ImageStorage
) : ViewModel() {

    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val weekStart = MutableStateFlow(LocalDate.now().startOfWeek())

    private val dayData = selectedDate.flatMapLatest { date ->
        combine(
            diaryRepository.observeMeals(date),
            waterRepository.observeWater(date)
        ) { meals, water -> DayData(date, meals, water) }
    }

    private val goal = profileRepository.observeProfile().map { profile ->
        profile?.let { calculateDailyGoal(it).goal }
    }

    val uiState: StateFlow<DiaryUiState> = combine(
        dayData,
        weekStart,
        recipeRepository.observeRecipes(),
        goal,
        weightRepository.observeLatest()
    ) { day, week, recipes, dailyGoal, weight ->
        DiaryUiState(
            selectedDate = day.date,
            weekStart = week,
            mealsByType = day.meals.groupBy { it.mealType },
            eatenSummary = calculateNutritionSummary(day.meals, onlyEaten = true),
            totalSummary = calculateNutritionSummary(day.meals, onlyEaten = false),
            water = day.water,
            recipes = recipes,
            goal = dailyGoal,
            latestWeight = weight
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DiaryUiState()
    )

    /** Produkty do okna „Dodaj produkt” (baza wbudowana + moje produkty). */
    val products: StateFlow<List<FoodProduct>> = productRepository.observeProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    fun addRecipeMeal(recipe: Recipe, mealType: MealType, servings: Double = 1.0) {
        viewModelScope.launch {
            addRecipeToDiary(recipe, selectedDate.value, mealType, servings = servings)
        }
    }

    /** Pojedynczy produkt z gramaturą (z bazy, skanu kodu albo Open Food Facts). */
    fun addProductMeal(product: FoodProduct, grams: Double, mealType: MealType) {
        val item = RecipeIngredient.from(product, grams)
        viewModelScope.launch {
            diaryRepository.addMeal(
                MealEntry(
                    date = selectedDate.value,
                    mealType = mealType,
                    name = "${product.name} (${formatGrams(grams)} g)",
                    calories = Math.round(item.calories).toInt(),
                    protein = Math.round(item.protein * 10.0) / 10.0,
                    carbs = Math.round(item.carbs * 10.0) / 10.0,
                    fat = Math.round(item.fat * 10.0) / 10.0
                )
            )
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

    /** Zmiana porcji, pory posiłku i zdjęcia wpisu. */
    fun updateEntry(entry: MealEntry, servings: Double, mealType: MealType, imagePath: String?) {
        if (entry.imagePath != null && entry.imagePath != imagePath) imageStorage.delete(entry.imagePath)
        viewModelScope.launch {
            diaryRepository.updateMeal(entry.scaledTo(servings).copy(mealType = mealType, imagePath = imagePath))
        }
    }

    fun toggleEaten(entry: MealEntry) {
        viewModelScope.launch { diaryRepository.setEaten(entry.id, !entry.isEaten) }
    }

    fun deleteMeal(entry: MealEntry) {
        imageStorage.delete(entry.imagePath)
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
