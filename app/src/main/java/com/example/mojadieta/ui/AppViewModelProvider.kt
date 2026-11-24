package com.example.mojadieta.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mojadieta.DietApplication
import com.example.mojadieta.di.AppContainer
import com.example.mojadieta.ui.diary.DiaryViewModel
import com.example.mojadieta.ui.notes.NoteEditViewModel
import com.example.mojadieta.ui.notes.NotesViewModel
import com.example.mojadieta.ui.planner.PlannerViewModel
import com.example.mojadieta.ui.recipes.RecipeDetailViewModel
import com.example.mojadieta.ui.recipes.RecipeEditViewModel
import com.example.mojadieta.ui.recipes.RecipesViewModel
import com.example.mojadieta.ui.update.UpdateViewModel
import com.example.mojadieta.ui.importer.ImportViewModel
import com.example.mojadieta.ui.profile.ProfileViewModel
import com.example.mojadieta.ui.weight.WeightViewModel
import com.example.mojadieta.ui.activity.ActivityViewModel

/** Fabryka wszystkich ViewModeli aplikacji – zależności pochodzą z [AppContainer]. */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            val c = container()
            DiaryViewModel(
                diaryRepository = c.diaryRepository,
                waterRepository = c.waterRepository,
                recipeRepository = c.recipeRepository,
                productRepository = c.productRepository,
                profileRepository = c.profileRepository,
                weightRepository = c.weightRepository,
                addRecipeToDiary = c.addRecipeToDiary,
                calculateNutritionSummary = c.calculateNutritionSummary,
                calculateDailyGoal = c.calculateDailyGoal,
                productLookup = c.productLookup,
                imageStorage = c.imageStorage,
                settingsRepository = c.settingsRepository,
                productUsageRepository = c.productUsageRepository,
                activityRepository = c.activityRepository,
                mealVisionRecognizer = c.mealVisionRecognizer
            )
        }
        initializer {
            val c = container()
            RecipesViewModel(c.recipeRepository, c.settingsRepository, c.mealVisionRecognizer)
        }
        initializer {
            val c = container()
            RecipeDetailViewModel(
                savedStateHandle = createSavedStateHandle(),
                recipeRepository = c.recipeRepository,
                plannerRepository = c.plannerRepository,
                addRecipeToDiary = c.addRecipeToDiary
            )
        }
        initializer {
            val c = container()
            RecipeEditViewModel(
                savedStateHandle = createSavedStateHandle(),
                recipeRepository = c.recipeRepository,
                productRepository = c.productRepository,
                calculateRecipeNutrition = c.calculateRecipeNutrition,
                productLookup = c.productLookup,
                imageStorage = c.imageStorage
            )
        }
        initializer {
            val c = container()
            PlannerViewModel(
                plannerRepository = c.plannerRepository,
                recipeRepository = c.recipeRepository,
                copyPlannedMealsToDiary = c.copyPlannedMealsToDiary,
                activityRepository = c.activityRepository,
                profileRepository = c.profileRepository
            )
        }
        initializer {
            NotesViewModel(container().noteRepository)
        }
        initializer {
            NoteEditViewModel(createSavedStateHandle(), container().noteRepository)
        }
        initializer {
            val c = container()
            ProfileViewModel(c.profileRepository, c.weightRepository, c.calculateDailyGoal)
        }
        initializer {
            val c = container()
            WeightViewModel(c.weightRepository, c.healthConnect, c, c.bodyMetricRepository)
        }
        initializer {
            val c = container()
            ActivityViewModel(c.activityRepository, c.profileRepository, c.settingsRepository, c.healthConnect, c)
        }
        initializer { com.example.mojadieta.ui.shopping.ShoppingViewModel(container().shoppingRepository) }
        initializer { com.example.mojadieta.ui.stats.StatsViewModel(container().statsCalculator) }
        initializer {
            val c = container()
            com.example.mojadieta.ui.settings.SettingsViewModel(c.settingsRepository, c.backupManager)
        }
        initializer {
            val c = container()
            ImportViewModel(c.recipeRepository, c.plannerRepository, c.productRepository, c.calculateRecipeNutrition, c.textExtractor)
        }
        initializer {
            val c = container()
            UpdateViewModel(
                checkForUpdate = c.checkForUpdate,
                repository = c.updateRepository,
                preferences = c.updatePreferences,
                currentVersion = c.currentVersion
            )
        }
    }
}

private fun CreationExtras.container(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as DietApplication).container
