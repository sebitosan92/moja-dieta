package com.example.mojadieta.ui.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mojadieta.data.food.ProductLookup
import com.example.mojadieta.data.images.ImageStorage
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
    /** Ścieżka do zdjęcia przepisu (plik w pamięci aplikacji) albo null. */
    val imagePath: String? = null,
    /** Tagi wpisane po przecinku. */
    val tags: String = "",
    val favorite: Boolean = false,
    val fiber: String = "",
    val sugar: String = "",
    val salt: String = "",
    val showErrors: Boolean = false,
    val isSaving: Boolean = false,
    /** Stan treści w chwili otwarcia/zapisu – do wykrywania niezapisanych zmian. */
    val savedKey: List<Any?>? = null
) {
    fun contentKey(): List<Any?> = listOf(
        name, category, calories, protein, carbs, fat, ingredients, instructions,
        servings, autoNutrition, ingredientItems, imagePath, tags, fiber, sugar, salt
    )

    val isDirty: Boolean get() = savedKey != null && contentKey() != savedKey

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
    private val calculateRecipeNutrition: CalculateRecipeNutritionUseCase,
    val productLookup: ProductLookup,
    val imageStorage: ImageStorage
) : ViewModel() {

    private val recipeId: Long = savedStateHandle.get<Long>(Routes.ARG_RECIPE_ID) ?: -1L

    private val _state = MutableStateFlow(
        RecipeFormState(isEditing = recipeId > 0, isLoading = recipeId > 0).withNutrition().let {
            if (recipeId > 0) it else it.copy(savedKey = it.contentKey())
        }
    )

    /** Zdjęcie przepisu w chwili otwarcia – żeby po zapisie usunąć stary plik. */
    private var originalImagePath: String? = null
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
                            ingredientItems = items,
                            imagePath = recipe.imagePath,
                            tags = recipe.tags.joinToString(", "),
                            favorite = recipe.favorite,
                            fiber = recipe.fiber?.let { formatGrams(it) }.orEmpty(),
                            sugar = recipe.sugar?.let { formatGrams(it) }.orEmpty(),
                            salt = recipe.salt?.let { formatGrams(it) }.orEmpty()
                        ).withNutrition().let { loaded -> loaded.copy(savedKey = loaded.contentKey()) }
                            .also { originalImagePath = recipe.imagePath }
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
    fun onTagsChange(value: String) = _state.update { it.copy(tags = value.take(120)) }
    fun onFiberChange(value: String) = _state.update { it.copy(fiber = value) }
    fun onSugarChange(value: String) = _state.update { it.copy(sugar = value) }
    fun onSaltChange(value: String) = _state.update { it.copy(salt = value) }
    fun onInstructionsChange(value: String) = _state.update { it.copy(instructions = value) }

    /** Nowe zdjęcie (już przycięte i zapisane) albo null = usunięcie zdjęcia. */
    fun onImageChange(path: String?) {
        val previous = _state.value.imagePath
        // Usuwamy od razu tylko pliki dodane w tej sesji edycji (nie oryginalne zdjęcie przepisu).
        if (previous != null && previous != originalImagePath && previous != path) imageStorage.delete(previous)
        _state.update { it.copy(imagePath = path) }
    }

    /** Odrzucenie zmian – sprząta zdjęcia dodane w tej sesji. */
    fun discardChanges() {
        val current = _state.value.imagePath
        if (current != null && current != originalImagePath) imageStorage.delete(current)
    }

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
                fat = formatGrams(perServing.fat),
                fiber = perServing.fiber?.let { formatGrams(it) }.orEmpty(),
                sugar = perServing.sugar?.let { formatGrams(it) }.orEmpty(),
                salt = perServing.salt?.let { formatGrams(it) }.orEmpty()
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
                    calories = if (useAuto) perServing.calories else form.calories.trim().toInt(),
                    protein = if (useAuto) perServing.protein else parseDecimal(form.protein) ?: 0.0,
                    carbs = if (useAuto) perServing.carbs else parseDecimal(form.carbs) ?: 0.0,
                    fat = if (useAuto) perServing.fat else parseDecimal(form.fat) ?: 0.0,
                    ingredients = form.ingredients.trim(),
                    instructions = form.instructions.trim(),
                    servings = form.servings,
                    autoNutrition = form.autoNutrition,
                    imagePath = form.imagePath,
                    favorite = form.favorite,
                    tags = form.tags.split(',', '#').map { it.trim().lowercase() }.filter { it.isNotEmpty() }.distinct(),
                    fiber = if (useAuto) perServing.fiber else parseDecimal(form.fiber),
                    sugar = if (useAuto) perServing.sugar else parseDecimal(form.sugar),
                    salt = if (useAuto) perServing.salt else parseDecimal(form.salt)
                ),
                form.ingredientItems
            )
            val old = originalImagePath
            if (old != null && old != form.imagePath) imageStorage.delete(old)
            originalImagePath = form.imagePath
            _state.update { it.copy(isSaving = false, savedKey = it.contentKey()) }
            onSaved()
        }
    }
}
