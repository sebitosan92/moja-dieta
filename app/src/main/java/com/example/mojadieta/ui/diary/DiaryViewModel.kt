package com.example.mojadieta.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mojadieta.data.food.ProductLookup
import com.example.mojadieta.data.images.ImageStorage
import com.example.mojadieta.data.repository.ActivityRepository
import com.example.mojadieta.data.repository.ProductUsageRepository
import com.example.mojadieta.data.settings.SettingsRepository
import com.example.mojadieta.data.stats.DayStatus
import com.example.mojadieta.data.stats.dayStatus
import com.example.mojadieta.domain.model.DailyGoal
import com.example.mojadieta.domain.model.FoodProduct
import com.example.mojadieta.domain.model.MealEntry
import com.example.mojadieta.domain.model.MealType
import com.example.mojadieta.domain.model.NutritionSummary
import com.example.mojadieta.domain.model.ProductUsage
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
import com.example.mojadieta.domain.settings.AppSettings
import com.example.mojadieta.domain.usecase.AddRecipeToDiaryUseCase
import com.example.mojadieta.domain.usecase.CalculateDailyGoalUseCase
import com.example.mojadieta.domain.usecase.CalculateNutritionSummaryUseCase
import com.example.mojadieta.domain.usecase.scaledTo
import com.example.mojadieta.ui.util.dayMonth
import com.example.mojadieta.ui.util.formatGrams
import com.example.mojadieta.ui.util.plural
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
    val latestWeight: WeightEntry? = null,
    /** Kropki w pasku tygodnia. */
    val dayStatuses: Map<LocalDate, DayStatus> = emptyMap(),
    val activityMinutes: Int = 0,
    val activityCalories: Int = 0,
    val settings: AppSettings = AppSettings()
)

private data class DayData(
    val date: LocalDate,
    val meals: List<MealEntry>,
    val water: WaterIntake,
    val activityMinutes: Int,
    val activityCalories: Int
)

/** Wartości wpisu ręcznego po edycji. */
data class ManualValues(val name: String, val calories: Int, val protein: Double, val carbs: Double, val fat: Double)

sealed interface DiaryMessage {
    data class Text(val text: String) : DiaryMessage
    data class Deleted(val entry: MealEntry) : DiaryMessage
}

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
    val imageStorage: ImageStorage,
    private val settingsRepository: SettingsRepository,
    private val productUsageRepository: ProductUsageRepository,
    activityRepository: ActivityRepository
) : ViewModel() {

    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val weekStart = MutableStateFlow(LocalDate.now().startOfWeek())

    private val _messages = MutableSharedFlow<DiaryMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<DiaryMessage> = _messages.asSharedFlow()

    private val dayData = selectedDate.flatMapLatest { date ->
        combine(
            diaryRepository.observeMeals(date),
            waterRepository.observeWater(date),
            activityRepository.observeDay(date)
        ) { meals, water, activities ->
            DayData(date, meals, water, activities.sumOf { it.durationMin }, activities.sumOf { it.calories })
        }
    }

    private val goal = profileRepository.observeProfile().map { profile ->
        profile?.let { calculateDailyGoal(it).goal }
    }

    private val weekTotals = weekStart.flatMapLatest { start ->
        diaryRepository.observeTotals(start, start.plusDays(6)).map { start to it }
    }

    private val baseState = combine(
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
            latestWeight = weight,
            activityMinutes = day.activityMinutes,
            activityCalories = day.activityCalories
        )
    }

    val uiState: StateFlow<DiaryUiState> = combine(baseState, weekTotals, settingsRepository.settings) { base, totals, settings ->
        val (_, rows) = totals
        val byDate = rows.associateBy { it.date }
        base.copy(
            dayStatuses = (0L..6L).associate { offset ->
                val date = base.weekStart.plusDays(offset)
                date to dayStatus(byDate[date], base.goal?.calories)
            },
            settings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DiaryUiState()
    )

    /** Produkty do okna „Dodaj produkt” (baza wbudowana + moje produkty). */
    val products: StateFlow<List<FoodProduct>> = productRepository.observeProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentProducts: StateFlow<List<ProductUsage>> = productUsageRepository.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoriteProducts: StateFlow<List<ProductUsage>> = productUsageRepository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val newEaten: Boolean get() = settingsRepository.current.newEntriesEaten

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
            addRecipeToDiary(recipe, selectedDate.value, mealType, eaten = newEaten, servings = servings)
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
                    fat = Math.round(item.fat * 10.0) / 10.0,
                    isEaten = newEaten,
                    fiber = item.fiber?.let { Math.round(it * 10.0) / 10.0 },
                    sugar = item.sugar?.let { Math.round(it * 10.0) / 10.0 },
                    salt = item.salt?.let { Math.round(it * 100.0) / 100.0 }
                )
            )
            productUsageRepository.recordUse(product.name)
        }
    }

    fun toggleFavoriteProduct(name: String) {
        viewModelScope.launch { productUsageRepository.toggleFavorite(name) }
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
                    fat = fat,
                    isEaten = newEaten
                )
            )
        }
    }

    /** Zmiana porcji, pory posiłku i zdjęcia wpisu (a dla wpisów ręcznych także nazwy i wartości). */
    fun updateEntry(entry: MealEntry, servings: Double, mealType: MealType, imagePath: String?, manual: ManualValues? = null) {
        if (entry.imagePath != null && entry.imagePath != imagePath) imageStorage.delete(entry.imagePath)
        val base = if (manual != null) {
            entry.copy(
                name = manual.name.trim().ifEmpty { entry.name },
                calories = manual.calories,
                protein = manual.protein,
                carbs = manual.carbs,
                fat = manual.fat
            )
        } else entry
        viewModelScope.launch {
            diaryRepository.updateMeal(base.scaledTo(servings).copy(mealType = mealType, imagePath = imagePath))
        }
    }

    fun toggleEaten(entry: MealEntry) {
        viewModelScope.launch { diaryRepository.setEaten(entry.id, !entry.isEaten) }
    }

    fun setEaten(entry: MealEntry, eaten: Boolean) {
        if (entry.isEaten == eaten) return
        viewModelScope.launch { diaryRepository.setEaten(entry.id, eaten) }
    }

    /** „Zaznacz wszystkie” – a gdy wszystkie są już zaznaczone, odznacza. */
    fun toggleAllEaten(mealType: MealType) {
        val entries = uiState.value.mealsByType[mealType].orEmpty()
        if (entries.isEmpty()) return
        val target = !entries.all { it.isEaten }
        viewModelScope.launch { diaryRepository.setEatenForMeal(selectedDate.value, mealType, target) }
    }

    /** Usunięcie z możliwością cofnięcia (zdjęcie zostaje do czasu sprzątania przy następnym starcie). */
    fun deleteMeal(entry: MealEntry) {
        viewModelScope.launch {
            diaryRepository.deleteMeal(entry.id)
            _messages.emit(DiaryMessage.Deleted(entry))
        }
    }

    fun undoDelete(entry: MealEntry) {
        viewModelScope.launch { diaryRepository.addMeal(entry) }
    }

    /** Kopiuje wpisy z dnia poprzedniego (całego albo jednej pory) na wybrany dzień. */
    fun repeatYesterday(mealType: MealType? = null) {
        val target = selectedDate.value
        val source = target.minusDays(1)
        viewModelScope.launch {
            val entries = if (mealType == null) diaryRepository.getMeals(source) else diaryRepository.getMeals(source, mealType)
            if (entries.isEmpty()) {
                _messages.emit(DiaryMessage.Text("Brak wpisów z ${source.dayMonth()}" + (mealType?.let { " (${it.label.lowercase()})" } ?: "")))
                return@launch
            }
            diaryRepository.addMeals(entries.map { it.copy(id = 0, date = target, isEaten = newEaten, imagePath = null) })
            _messages.emit(
                DiaryMessage.Text("Skopiowano ${entries.size} ${plural(entries.size, "wpis", "wpisy", "wpisów")} z ${source.dayMonth()}")
            )
        }
    }

    fun copyEntry(entry: MealEntry, date: LocalDate, mealType: MealType) {
        viewModelScope.launch {
            diaryRepository.addMeal(entry.copy(id = 0, date = date, mealType = mealType, isEaten = newEaten, imagePath = null))
            _messages.emit(DiaryMessage.Text("Skopiowano na ${date.dayMonth()} · ${mealType.label}"))
        }
    }

    fun changeWater(deltaMl: Int) {
        viewModelScope.launch { waterRepository.changeAmount(selectedDate.value, deltaMl) }
    }

    fun setWaterGoal(goalMl: Int) {
        if (goalMl <= 0) return
        viewModelScope.launch { waterRepository.setGoal(selectedDate.value, goalMl) }
    }
}
