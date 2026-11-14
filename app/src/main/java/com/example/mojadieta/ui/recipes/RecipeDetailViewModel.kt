package com.example.mojadieta.ui.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mojadieta.domain.model.MealType
import com.example.mojadieta.domain.model.Recipe
import com.example.mojadieta.domain.repository.PlannerRepository
import com.example.mojadieta.domain.repository.RecipeRepository
import com.example.mojadieta.domain.usecase.AddRecipeToDiaryUseCase
import com.example.mojadieta.ui.navigation.Routes
import com.example.mojadieta.ui.util.dayMonth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface RecipeDetailState {
    data object Loading : RecipeDetailState
    data object NotFound : RecipeDetailState
    data class Loaded(val recipe: Recipe) : RecipeDetailState
}

class RecipeDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository,
    private val plannerRepository: PlannerRepository,
    private val addRecipeToDiary: AddRecipeToDiaryUseCase
) : ViewModel() {

    private val recipeId: Long = savedStateHandle.get<Long>(Routes.ARG_RECIPE_ID) ?: -1L

    val state: StateFlow<RecipeDetailState> = recipeRepository.observeRecipe(recipeId)
        .map { recipe -> if (recipe == null) RecipeDetailState.NotFound else RecipeDetailState.Loaded(recipe) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipeDetailState.Loading)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun addToToday(mealType: MealType) {
        val recipe = (state.value as? RecipeDetailState.Loaded)?.recipe ?: return
        viewModelScope.launch {
            addRecipeToDiary(recipe, LocalDate.now(), mealType)
            _messages.emit("Dodano „${recipe.name}” do dzisiejszego dziennika (${mealType.label})")
        }
    }

    fun planRecipe(date: LocalDate, mealType: MealType) {
        val recipe = (state.value as? RecipeDetailState.Loaded)?.recipe ?: return
        viewModelScope.launch {
            plannerRepository.addPlannedMeal(date, mealType, recipe.id)
            _messages.emit("Zaplanowano na ${date.dayMonth()} (${mealType.label})")
        }
    }

    fun deleteRecipe(onDeleted: () -> Unit) {
        viewModelScope.launch {
            recipeRepository.deleteRecipe(recipeId)
            onDeleted()
        }
    }
}
