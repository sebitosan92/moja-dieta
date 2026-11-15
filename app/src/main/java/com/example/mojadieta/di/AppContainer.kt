package com.example.mojadieta.di

import android.content.Context
import com.example.mojadieta.BuildConfig
import com.example.mojadieta.data.food.OpenFoodFactsClient
import com.example.mojadieta.data.food.ProductLookup
import com.example.mojadieta.data.health.HealthConnectManager
import com.example.mojadieta.data.importer.TextExtractor
import com.example.mojadieta.data.images.ImageStorage
import com.example.mojadieta.data.local.DietDatabase
import com.example.mojadieta.data.profile.ProfileRepositoryImpl
import com.example.mojadieta.data.repository.WeightRepositoryImpl
import com.example.mojadieta.data.repository.ActivityRepository
import com.example.mojadieta.data.repository.BodyMetricRepository
import com.example.mojadieta.data.repository.ProductUsageRepository
import com.example.mojadieta.data.repository.ShoppingRepository
import com.example.mojadieta.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import com.example.mojadieta.domain.repository.ProfileRepository
import com.example.mojadieta.domain.repository.WeightRepository
import com.example.mojadieta.domain.usecase.CalculateDailyGoalUseCase
import com.example.mojadieta.data.update.GitHubUpdateRepository
import com.example.mojadieta.data.update.UpdatePreferences
import com.example.mojadieta.domain.update.CheckForUpdateUseCase
import com.example.mojadieta.domain.update.UpdateRepository
import com.example.mojadieta.data.repository.DiaryRepositoryImpl
import com.example.mojadieta.data.repository.NoteRepositoryImpl
import com.example.mojadieta.data.repository.PlannerRepositoryImpl
import com.example.mojadieta.data.repository.ProductRepositoryImpl
import com.example.mojadieta.data.repository.RecipeRepositoryImpl
import com.example.mojadieta.data.repository.WaterRepositoryImpl
import com.example.mojadieta.domain.repository.DiaryRepository
import com.example.mojadieta.domain.repository.NoteRepository
import com.example.mojadieta.domain.repository.PlannerRepository
import com.example.mojadieta.domain.repository.ProductRepository
import com.example.mojadieta.domain.repository.RecipeRepository
import com.example.mojadieta.domain.repository.WaterRepository
import com.example.mojadieta.domain.usecase.AddRecipeToDiaryUseCase
import com.example.mojadieta.domain.usecase.CalculateNutritionSummaryUseCase
import com.example.mojadieta.domain.usecase.CalculateRecipeNutritionUseCase
import com.example.mojadieta.domain.usecase.CopyPlannedMealsToDiaryUseCase

/** Ręczny kontener zależności (prosty odpowiednik DI) tworzony raz w klasie Application. */
class AppContainer(private val context: Context) {

    private val database = DietDatabase.getInstance(context)

    val recipeRepository: RecipeRepository = RecipeRepositoryImpl(database.recipeDao())
    val diaryRepository: DiaryRepository = DiaryRepositoryImpl(database.mealEntryDao())
    val plannerRepository: PlannerRepository = PlannerRepositoryImpl(database.plannedMealDao())
    val waterRepository: WaterRepository = WaterRepositoryImpl(database.waterDao())
    val noteRepository: NoteRepository = NoteRepositoryImpl(database.noteDao())
    val productRepository: ProductRepository = ProductRepositoryImpl(database.customProductDao())
    val profileRepository: ProfileRepository = ProfileRepositoryImpl(context)
    val weightRepository: WeightRepository = WeightRepositoryImpl(database.weightDao(), profileRepository)

    val activityRepository = ActivityRepository(context, database.activityDao())
    val bodyMetricRepository = BodyMetricRepository(database.bodyMetricDao())
    val productUsageRepository = ProductUsageRepository(database.productUsageDao())
    val shoppingRepository = ShoppingRepository(database.shoppingDao(), plannerRepository, recipeRepository)
    val settingsRepository = SettingsRepository(context)

    val statsCalculator = com.example.mojadieta.data.stats.StatsCalculator(
        diaryRepository, waterRepository, weightRepository, activityRepository,
        profileRepository, settingsRepository, CalculateDailyGoalUseCase()
    )

    val backupManager = com.example.mojadieta.data.backup.BackupManager(context)

    val imageStorage = ImageStorage(context)
    val productLookup = ProductLookup(productRepository, OpenFoodFactsClient())
    val healthConnect = HealthConnectManager(context)
    val textExtractor by lazy { TextExtractor(context, imageStorage) }

    val calculateNutritionSummary = CalculateNutritionSummaryUseCase()
    val calculateRecipeNutrition = CalculateRecipeNutritionUseCase()
    val calculateDailyGoal = CalculateDailyGoalUseCase()
    val addRecipeToDiary = AddRecipeToDiaryUseCase(diaryRepository)
    val copyPlannedMealsToDiary = CopyPlannedMealsToDiaryUseCase(plannerRepository, diaryRepository)

    // --- Aktualizacje z GitHuba ---
    val currentVersion: String = BuildConfig.VERSION_NAME
    val updatePreferences = UpdatePreferences(context)
    val updateRepository: UpdateRepository = GitHubUpdateRepository(context, BuildConfig.GITHUB_REPO)
    val checkForUpdate = CheckForUpdateUseCase(updateRepository, currentVersion)

    /**
     * Pobiera nowe pomiary wagi z Health Connect (jeśli jest dostępny i jest zgoda).
     * Zwraca liczbę nowych pomiarów albo null, gdy synchronizacja nie jest możliwa.
     */
    suspend fun syncWeightsFromHealthConnect(): Int? {
        if (healthConnect.availability() != HealthConnectManager.Availability.AVAILABLE) return null
        val granted = healthConnect.grantedPermissions()
        if (granted.none { it in healthConnect.weightPermissions || it in healthConnect.bodyPermissions }) return null
        val weights = weightRepository.importExternal(healthConnect.readWeights())
        val body = bodyMetricRepository.importExternal(healthConnect.readBodyComposition())
        return weights + body
    }

    /**
     * Treningi z ostatnich [days] dni oraz kroki/aktywne kalorie dzień po dniu.
     * Zwraca liczbę nowych treningów albo null, gdy brak Health Connect lub zgody.
     */
    suspend fun syncActivityFromHealthConnect(days: Long = 30): Int? {
        if (healthConnect.availability() != HealthConnectManager.Availability.AVAILABLE) return null
        if (!healthConnect.hasAnyActivityPermission()) return null
        val weight = profileRepository.observeProfile().first()?.weightKg ?: 70.0
        val added = activityRepository.importExternal(healthConnect.readExercises(days, weight))
        activityRepository.saveSteps(healthConnect.readDailySteps(days))
        return added
    }

    /** Usuwa pliki zdjęć, których nie używa już żaden przepis ani wpis w dzienniku. */
    suspend fun cleanupImages() {
        val used = (recipeRepository.imagePaths() + diaryRepository.imagePaths()).toSet()
        imageStorage.cleanup(used)
    }
}
