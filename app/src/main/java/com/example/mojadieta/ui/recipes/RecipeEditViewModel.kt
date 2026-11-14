package com.example.mojadieta.ui.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mojadieta.domain.model.FoodProduct
import com.example.mojadieta.domain.model.Recipe
import com.example.mojadieta.domain.model.RecipeCategory
import com.example.mojadieta.domain.model.RecipeIngredient
import com.example.mojadieta.domain.repository.ProductRepository
import com.example.mojadieta.domain.repository.RecipeRepository
import com.example.mojadieta.domain.usecase.CalculateRecipeNutritionUseCase
import com.example.mojadieta.domain.usecase.RecipeNutrition
import com.example.mojadieta.ui.navigation.Routes
import com.example.mojadieta.ui.util.formatGrams
import com.example.mojadieta.ui.util.parseDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecipeFormState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val name: String = "",
    val category: RecipeCategory = RecipeCategory.BREAKFAST,
    val calories: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fat: String = "",
    val ingredients: String = "",
    val instructions: String = "",
    val servings: Int = 1,
    /** Nowe przepisy domyślnie liczą wartości ze składników. */
    val autoNutrition: Boolean = true,
    val ingredientItems: List<RecipeIngredient> = emptyList(),
    val nutrition: RecipeNutrition? = null,
    val showErrors: Boolean = false,
    val isSaving: Boolean = false
) {
    val nameError: String? get() = if (name.isBlank()) "Podaj nazwę dania" else null
    val caloriesError: String? get() = when {
        autoNutrition -> null
        calories.isBlank() -> "Podaj kalorie"
        calories.trim().toIntOrNull() == null -> "Nieprawidłowa liczba"
        else -> null
    }
    val proteinError: String? get() = if (autoNutrition) null else decimalError(protein)
    val carbsError: String? get() = if (autoNutrition) null else decimalError(carbs)
    val fatError: String? get() = if (autoNutrition) null else decimalError(fat)
    val ingredientItemsError: String?
        get() = if (autoNutrition && ingredientItems.isEmpty()) {
            "Dodaj składniki z gramaturą albo wyłącz automatyczne liczenie"
        } else {
            null
        }

    val isValid: Boolean
        get() = listOf(nameError, caloriesError, proteinError, carbsError, fatError, ingredientItemsError)
            .all { it == null }

    private fun decimalError(value: String): String? = when {
        value.isBlank() -> null
        parseDecimal(value) == null -> "Nieprawidłowa liczba"
        else -> null
    }
}

class RecipeEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository,
    private val productRepository: ProductRepository,
    private val calculateRecipeNutrition: CalculateRecipeNutritionUseCase
) : ViewModel() {

    private val recipeId: Long = savedStateHandle.get<Long>(Routes.ARG_RECIPE_ID) ?: -1L

    private val _state = MutableStateFlow(
        RecipeFormState(isEditing = recipeId > 0, isLoading = recipeId > 0).withNutrition()
    )
    val state: StateFlow<RecipeFormState> = _state.asStateFlow()

    /** Wbudowana baza + własne produkty do wyszukiwarki składników. */
    val products: StateFlow<List<FoodProduct>> = productRepository.observeProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (recipeId > 0) {
            viewModelScope.launch {
                val recipe = recipeRepository.getRecipe(recipeId)
                val items = recipeRepository.getIngredients(recipeId)
                _state.update { current ->
                    if (recipe == null) {
                        current.copy(isLoading = false, isEditing = false)
                    } else {
                        current.copy(
                            isLoading = false,
                            name = recipe.name,
                            category = recipe.category,
                            calories = recipe.calories.toString(),
                            protein = formatGrams(recipe.protein),
                            carbs = formatGrams(recipe.carbs),
                            fat = formatGrams(recipe.fat),
                            ingredients = recipe.ingredients,
                            instructions = recipe.instructions,
                            servings = recipe.servings,
                            autoNutrition = recipe.autoNutrition,
                            ingredientItems = items
                        ).withNutrition()
                    }
                }
            }
        }
    }

    private fun RecipeFormState.withNutrition(): RecipeFormState =
        copy(nutrition = calculateRecipeNutrition(ingredientItems, servings))

    fun onNameChange(value: String) = _state.update { it.copy(name = value) }
    fun onCategoryChange(value: RecipeCategory) = _state.update { it.copy(category = value) }
    fun onCaloriesChange(value: String) = _state.update { it.copy(calories = value) }
    fun onProteinChange(value: String) = _state.update { it.copy(protein = value) }
    fun onCarbsChange(value: String) = _state.update { it.copy(carbs = value) }
    fun onFatChange(value: String) = _state.update { it.copy(fat = value) }
    fun onIngredientsChange(value: String) = _state.update { it.copy(ingredients = value) }
    fun onInstructionsChange(value: String) = _state.update { it.copy(instructions = value) }

    fun onServingsChange(value: Int) =
        _state.update { it.copy(servings = value.coerceIn(1, 50)).withNutrition() }

    /** Włączenie/wyłączenie automatycznego liczenia. Przy wyłączaniu przepisuje wyliczone wartości do pól. */
    fun onAutoNutritionChange(enabled: Boolean) = _state.update { current ->
        val perServing = current.nutrition?.perServing
        if (!enabled && current.autoNutrition && perServing != null && current.ingredientItems.isNotEmpty()) {
            current.copy(
                autoNutrition = false,
                calories = perServing.calories.toString(),
                protein = formatGrams(perServing.protein),
                carbs = formatGrams(perServing.carbs),
                fat = formatGrams(perServing.fat)
            )
        } else {
            current.copy(autoNutrition = enabled)
        }
    }

    fun addIngredient(product: FoodProduct, grams: Double) = _state.update {
        it.copy(ingredientItems = it.ingredientItems + RecipeIngredient.from(product, grams)).withNutrition()
    }

    fun updateIngredientGrams(index: Int, grams: Double) = _state.update { current ->
        val items = current.ingredientItems.toMutableList()
        if (index in items.indices) items[index] = items[index].copy(grams = grams)
        current.copy(ingredientItems = items).withNutrition()
    }

    fun removeIngredient(index: Int) = _state.update { current ->
        current.copy(ingredientItems = current.ingredientItems.filterIndexed { i, _ -> i != index }).withNutrition()
    }

    /** Zapisuje własny produkt w bazie i zwraca go (z nadanym id) przez [onSaved]. */
    fun addCustomProduct(product: FoodProduct, onSaved: (FoodProduct) -> Unit) {
        viewModelScope.launch { onSaved(productRepository.addCustomProduct(product)) }
    }

    fun save(onSaved: () -> Unit) {
        val form = _state.value
        if (!form.isValid) {
            _state.update { it.copy(showErrors = true) }
            return
        }
        if (form.isSaving) return
        _state.update { it.copy(isSaving = true) }
        val perServing = form.nutrition?.perServing
        val useAuto = form.autoNutrition && perServing != null
        viewModelScope.launch {
            recipeRepository.saveRecipeWithIngredients(
                Recipe(
                    id = if (form.isEditing) recipeId else 0L,
                    name = form.name.trim(),
                    category = form.category,
                    calories = if (useAuto) perServing!!.calories else form.calories.trim().toInt(),
                    protein = if (useAuto) perServing!!.protein else parseDecimal(form.protein) ?: 0.0,
                    carbs = if (useAuto) perServing!!.carbs else parseDecimal(form.carbs) ?: 0.0,
                    fat = if (useAuto) perServing!!.fat else parseDecimal(form.fat) ?: 0.0,
                    ingredients = form.ingredients.trim(),
                    instructions = form.instructions.trim(),
                    servings = form.servings,
                    autoNutrition = form.autoNutrition
                ),
                form.ingredientItems
            )
            _state.update { it.copy(isSaving = false) }
            onSaved()
        }
    }
}
