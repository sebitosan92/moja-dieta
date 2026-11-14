package com.example.mojadieta.ui.importer

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mojadieta.data.importer.TextExtractor
import com.example.mojadieta.domain.importer.ImportedRecipe
import com.example.mojadieta.domain.importer.RecipeTextParser
import com.example.mojadieta.domain.model.MealType
import com.example.mojadieta.domain.model.NutritionSummary
import com.example.mojadieta.domain.model.Recipe
import com.example.mojadieta.domain.model.RecipeCategory
import com.example.mojadieta.domain.repository.PlannerRepository
import com.example.mojadieta.domain.repository.ProductRepository
import com.example.mojadieta.domain.repository.RecipeRepository
import com.example.mojadieta.domain.usecase.CalculateRecipeNutritionUseCase
import com.example.mojadieta.ui.util.startOfWeek
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

sealed interface ImportPhase {
    data object Idle : ImportPhase
    data class Reading(val done: Int, val total: Int, val what: String) : ImportPhase
    data object Preview : ImportPhase
    data object Importing : ImportPhase
    data class Done(val count: Int, val planned: Int) : ImportPhase
    data class Error(val message: String) : ImportPhase
}

data class ImportItem(
    val recipe: ImportedRecipe,
    val selected: Boolean = true,
    val category: RecipeCategory = recipe.category,
    /** Wartości na porcję: z pliku albo wyliczone ze składników. */
    val perServing: NutritionSummary,
    val nutritionFromFile: Boolean
)

data class ImportUiState(
    val phase: ImportPhase = ImportPhase.Idle,
    val items: List<ImportItem> = emptyList(),
    val isMealPlan: Boolean = false,
    val addToPlanner: Boolean = true,
    val plannerStart: LocalDate = LocalDate.now().plusDays(7).startOfWeek(),
    val rawText: String = ""
) {
    val selectedCount: Int get() = items.count { it.selected }
}

class ImportViewModel(
    private val recipeRepository: RecipeRepository,
    private val plannerRepository: PlannerRepository,
    private val productRepository: ProductRepository,
    private val calculateRecipeNutrition: CalculateRecipeNutritionUseCase,
    private val textExtractor: TextExtractor
) : ViewModel() {

    private val _state = MutableStateFlow(ImportUiState())
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    fun importPdf(uri: Uri) = read("Odczytuję stronę") { progress -> textExtractor.fromPdf(uri, progress) }

    fun importImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        read("Odczytuję zdjęcie") { progress -> textExtractor.fromImages(uris, progress) }
    }

    private fun read(what: String, extract: suspend ((Int, Int) -> Unit) -> String) {
        _state.update { it.copy(phase = ImportPhase.Reading(0, 0, what)) }
        viewModelScope.launch {
            try {
                val text = extract { done, total ->
                    _state.update { s -> s.copy(phase = ImportPhase.Reading(done, total, what)) }
                }
                val products = productRepository.observeProducts().first()
                val result = withContext(Dispatchers.Default) { RecipeTextParser(products).parse(text) }
                val items = result.recipes.map { recipe ->
                    val fromFile = recipe.hasExplicitNutrition
                    val perServing = if (fromFile) {
                        NutritionSummary(
                            calories = recipe.calories ?: 0,
                            protein = recipe.protein ?: 0.0,
                            carbs = recipe.carbs ?: 0.0,
                            fat = recipe.fat ?: 0.0
                        )
                    } else {
                        calculateRecipeNutrition(recipe.ingredients, recipe.servings).perServing
                    }
                    ImportItem(recipe = recipe, perServing = perServing, nutritionFromFile = fromFile)
                }
                _state.update {
                    it.copy(
                        phase = ImportPhase.Preview,
                        items = items,
                        isMealPlan = result.isMealPlan,
                        addToPlanner = result.isMealPlan,
                        rawText = text
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(phase = ImportPhase.Error("Nie udało się odczytać pliku. ${e.message ?: ""}".trim()))
                }
            }
        }
    }

    fun toggle(index: Int) = _state.update { s ->
        s.copy(items = s.items.mapIndexed { i, item -> if (i == index) item.copy(selected = !item.selected) else item })
    }

    fun toggleAll(selected: Boolean) = _state.update { s -> s.copy(items = s.items.map { it.copy(selected = selected) }) }

    fun cycleCategory(index: Int) = _state.update { s ->
        s.copy(items = s.items.mapIndexed { i, item ->
            if (i == index) {
                val next = RecipeCategory.entries[(item.category.ordinal + 1) % RecipeCategory.entries.size]
                item.copy(category = next)
            } else {
                item
            }
        })
    }

    fun setAddToPlanner(enabled: Boolean) = _state.update { it.copy(addToPlanner = enabled) }

    fun setPlannerStart(date: LocalDate) = _state.update { it.copy(plannerStart = date) }

    fun confirmImport() {
        val current = _state.value
        val selected = current.items.filter { it.selected }
        if (selected.isEmpty()) return
        _state.update { it.copy(phase = ImportPhase.Importing) }
        viewModelScope.launch {
            var planned = 0
            selected.forEach { item ->
                val r = item.recipe
                val auto = !item.nutritionFromFile && r.ingredients.isNotEmpty()
                val id = recipeRepository.saveRecipeWithIngredients(
                    Recipe(
                        name = r.title,
                        category = item.category,
                        calories = item.perServing.calories,
                        protein = item.perServing.protein,
                        carbs = item.perServing.carbs,
                        fat = item.perServing.fat,
                        ingredients = r.otherIngredients.joinToString("\n"),
                        instructions = r.steps.joinToString("\n"),
                        servings = r.servings,
                        autoNutrition = auto
                    ),
                    r.ingredients
                )
                val day = r.dayIndex
                if (current.isMealPlan && current.addToPlanner && day != null) {
                    plannerRepository.addPlannedMeal(
                        current.plannerStart.plusDays((day - 1).toLong()),
                        r.mealType ?: MealType.LUNCH,
                        id
                    )
                    planned++
                }
            }
            _state.update { it.copy(phase = ImportPhase.Done(selected.size, planned)) }
        }
    }

    fun reset() {
        _state.value = ImportUiState()
    }
}
