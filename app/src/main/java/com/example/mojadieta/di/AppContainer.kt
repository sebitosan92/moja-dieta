package com.example.mojadieta.di

import android.content.Context
import com.example.mojadieta.BuildConfig
import com.example.mojadieta.data.local.DietDatabase
import com.example.mojadieta.data.update.GitHubUpdateRepository
import com.example.mojadieta.data.update.UpdatePreferences
import com.example.mojadieta.domain.update.CheckForUpdateUseCase
import com.example.mojadieta.domain.update.UpdateRepository
import com.example.mojadieta.data.repository.DiaryRepositoryImpl
import com.example.mojadieta.data.repository.NoteRepositoryImpl
import com.example.mojadieta.data.repository.PlannerRepositoryImpl
import com.example.mojadieta.data.repository.RecipeRepositoryImpl
import com.example.mojadieta.data.repository.WaterRepositoryImpl
import com.example.mojadieta.domain.repository.DiaryRepository
import com.example.mojadieta.domain.repository.NoteRepository
import com.example.mojadieta.domain.repository.PlannerRepository
import com.example.mojadieta.domain.repository.RecipeRepository
import com.example.mojadieta.domain.repository.WaterRepository
import com.example.mojadieta.domain.usecase.AddRecipeToDiaryUseCase
import com.example.mojadieta.domain.usecase.CalculateNutritionSummaryUseCase
import com.example.mojadieta.domain.usecase.CopyPlannedMealsToDiaryUseCase

/** Ręczny kontener zależności (prosty odpowiednik DI) tworzony raz w klasie Application. */
class AppContainer(context: Context) {

    private val database = DietDatabase.getInstance(context)

    val recipeRepository: RecipeRepository = RecipeRepositoryImpl(database.recipeDao())
    val diaryRepository: DiaryRepository = DiaryRepositoryImpl(database.mealEntryDao())
    val plannerRepository: PlannerRepository = PlannerRepositoryImpl(database.plannedMealDao())
    val waterRepository: WaterRepository = WaterRepositoryImpl(database.waterDao())
    val noteRepository: NoteRepository = NoteRepositoryImpl(database.noteDao())

    val calculateNutritionSummary = CalculateNutritionSummaryUseCase()
    val addRecipeToDiary = AddRecipeToDiaryUseCase(diaryRepository)
    val copyPlannedMealsToDiary = CopyPlannedMealsToDiaryUseCase(plannerRepository, diaryRepository)

    // --- Aktualizacje z GitHuba ---
    val currentVersion: String = BuildConfig.VERSION_NAME
    val updatePreferences = UpdatePreferences(context)
    val updateRepository: UpdateRepository = GitHubUpdateRepository(context, BuildConfig.GITHUB_REPO)
    val checkForUpdate = CheckForUpdateUseCase(updateRepository, currentVersion)
}
