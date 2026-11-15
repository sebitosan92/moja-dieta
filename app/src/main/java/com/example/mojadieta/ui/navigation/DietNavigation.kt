package com.example.mojadieta.ui.navigation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mojadieta.DietApplication
import com.example.mojadieta.ui.AppViewModelProvider
import com.example.mojadieta.ui.importer.ImportScreen
import com.example.mojadieta.ui.profile.ProfileScreen
import com.example.mojadieta.ui.weight.WeightScreen
import com.example.mojadieta.ui.splash.SplashScreen
import com.example.mojadieta.ui.update.AboutScreen
import com.example.mojadieta.ui.update.UpdatePromptDialog
import com.example.mojadieta.ui.update.UpdateViewModel
import com.example.mojadieta.update.UpdateNotifier
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import com.example.mojadieta.ui.activity.ActivityScreen
import androidx.compose.runtime.remember
import com.example.mojadieta.MainActivity
import com.example.mojadieta.domain.settings.SplashMode
import com.example.mojadieta.reminders.AppActions
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mojadieta.ui.diary.DiaryScreen
import com.example.mojadieta.ui.notes.NoteEditScreen
import com.example.mojadieta.ui.notes.NotesScreen
import com.example.mojadieta.ui.planner.PlannerScreen
import com.example.mojadieta.ui.recipes.RecipeDetailScreen
import com.example.mojadieta.ui.recipes.RecipeEditScreen
import com.example.mojadieta.ui.recipes.RecipesScreen

object Routes {
    const val DIARY = "diary"
    const val RECIPES = "recipes"
    const val PLANNER = "planner"
    const val NOTES = "notes"
    const val ABOUT = "about"
    const val PROFILE = "profile"
    const val WEIGHT = "weight"
    const val IMPORT = "import"
    const val ACTIVITY = "activity"
    const val SHOPPING = "shopping"
    const val STATS = "stats"
    const val SETTINGS = "settings"

    const val ARG_RECIPE_ID = "recipeId"
    const val ARG_NOTE_ID = "noteId"

    const val RECIPE_DETAIL = "recipe/{$ARG_RECIPE_ID}"
    const val RECIPE_EDIT = "recipe_edit?$ARG_RECIPE_ID={$ARG_RECIPE_ID}"
    const val NOTE_EDIT = "note_edit?$ARG_NOTE_ID={$ARG_NOTE_ID}"

    fun recipeDetail(id: Long) = "recipe/$id"
    fun recipeEdit(id: Long? = null) = if (id == null) "recipe_edit" else "recipe_edit?$ARG_RECIPE_ID=$id"
    fun noteEdit(id: Long? = null) = if (id == null) "note_edit" else "note_edit?$ARG_NOTE_ID=$id"
}

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DIARY(Routes.DIARY, "Dziennik", Icons.Filled.Today, Icons.Outlined.Today),
    RECIPES(Routes.RECIPES, "Przepisy", Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
    PLANNER(Routes.PLANNER, "Planer", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    ACTIVITY(Routes.ACTIVITY, "Aktywność", Icons.AutoMirrored.Filled.DirectionsRun, Icons.AutoMirrored.Outlined.DirectionsRun),
    NOTES(Routes.NOTES, "Notatnik", Icons.Filled.Description, Icons.Outlined.Description)
}

/**
 * Korzeń aplikacji: najpierw animowany ekran ładowania, potem właściwa aplikacja.
 * [openUpdatesRequest] = true, gdy aplikację otwarto z powiadomienia o aktualizacji.
 */
@Composable
fun DietAppRoot(request: String?, onRequestHandled: () -> Unit) {
    val context = LocalContext.current
    // Rodzaj ekranu startowego ustalany raz na uruchomienie: 0 = brak, 1 = krótki, 2 = pełny.
    val splashKind = rememberSaveable {
        val settings = (context.applicationContext as DietApplication).container.settingsRepository
        if (request != null) 0 else when (settings.current.splashMode) {
            SplashMode.OFF -> 0
            SplashMode.SHORT -> 1
            SplashMode.ALWAYS -> 2
            SplashMode.DAILY -> if (settings.consumeDailyFullSplash()) 2 else 1
        }
    }
    var splashFinished by rememberSaveable { mutableStateOf(splashKind == 0) }
    Crossfade(targetState = splashFinished, animationSpec = tween(500), label = "splash") { finished ->
        if (finished) {
            MainContent(request, onRequestHandled)
        } else {
            SplashScreen(short = splashKind == 1, onFinished = { splashFinished = true })
        }
    }
}

@Composable
private fun MainContent(request: String?, onRequestHandled: () -> Unit) {
    val updateViewModel: UpdateViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val updateState by updateViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Prośba o zgodę na powiadomienia (Android 13+) – tylko raz, przy pierwszym wejściu.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !UpdateNotifier.canNotify(context)) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        updateViewModel.checkOnStartup()
        // Nowe pomiary wagi z Health Connect (jeśli jest połączony).
        runCatching { (context.applicationContext as DietApplication).container.syncWeightsFromHealthConnect() }
    }

    // Po powrocie z ustawień „Instaluj nieznane aplikacje” wznawiamy instalację.
    LifecycleResumeEffect(updateViewModel) {
        updateViewModel.onResumed(context)
        onPauseOrDispose { }
    }

    val navController = rememberNavController()

    val openRequests = remember { OpenRequests() }
    LaunchedEffect(request) {
        if (request == null) return@LaunchedEffect
        when (request) {
            MainActivity.REQUEST_UPDATES -> navController.navigate(Routes.ABOUT) { launchSingleTop = true }
            AppActions.SCAN -> {
                navController.navigateToTopLevel(Routes.DIARY)
                openRequests.scan = true
            }
            AppActions.ADD_WEIGHT -> {
                navController.navigate(Routes.WEIGHT) { launchSingleTop = true }
                openRequests.addWeight = true
            }
            AppActions.ADD_ACTIVITY -> {
                navController.navigateToTopLevel(Routes.ACTIVITY)
                openRequests.addActivity = true
            }
            AppActions.OPEN_PLANNER -> navController.navigateToTopLevel(Routes.PLANNER)
            AppActions.OPEN_STATS -> navController.navigate(Routes.STATS) { launchSingleTop = true }
            else -> navController.navigateToTopLevel(Routes.DIARY)
        }
        onRequestHandled()
    }

    updateState.promptRelease?.let { release ->
        UpdatePromptDialog(
            release = release,
            onUpdate = {
                updateViewModel.download(release)
                navController.navigate(Routes.ABOUT) { launchSingleTop = true }
            },
            onLater = updateViewModel::dismissPrompt
        )
    }

    AppScaffold(navController = navController, updateViewModel = updateViewModel, openRequests = openRequests)
}

/** Jednorazowe prośby z powiadomień i skrótów: otwórz skaner / dodawanie wagi / treningu. */
class OpenRequests {
    var scan by mutableStateOf(false)
    var addWeight by mutableStateOf(false)
    var addActivity by mutableStateOf(false)
}

@Composable
private fun AppScaffold(navController: NavHostController, updateViewModel: UpdateViewModel, openRequests: OpenRequests) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = TopLevelDestination.entries.any { it.route == currentDestination?.route }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTopLevel(destination.route) },
                            icon = {
                                Icon(
                                    if (selected) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = destination.label
                                )
                            },
                            label = { Text(destination.label) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        DietNavHost(
            navController = navController,
            updateViewModel = updateViewModel,
            openRequests = openRequests,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        )
    }
}

private fun NavHostController.navigateToTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun DietNavHost(
    navController: NavHostController,
    updateViewModel: UpdateViewModel,
    openRequests: OpenRequests,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.DIARY,
        modifier = modifier
    ) {
        composable(Routes.DIARY) {
            DiaryScreen(
                onOpenAbout = { navController.navigate(Routes.ABOUT) },
                onOpenProfile = { navController.navigate(Routes.PROFILE) },
                onOpenWeight = { navController.navigate(Routes.WEIGHT) },
                onOpenStats = { navController.navigate(Routes.STATS) },
                onOpenShopping = { navController.navigate(Routes.SHOPPING) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                openScanRequest = openRequests.scan,
                onScanRequestHandled = { openRequests.scan = false }
            )
        }
        composable(Routes.SHOPPING) {
            com.example.mojadieta.ui.shopping.ShoppingScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.STATS) {
            com.example.mojadieta.ui.stats.StatsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            com.example.mojadieta.ui.settings.SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onOpenWeight = { navController.navigate(Routes.WEIGHT) }
            )
        }
        composable(Routes.WEIGHT) {
            WeightScreen(
                onBack = { navController.popBackStack() },
                openAddRequest = openRequests.addWeight,
                onAddRequestHandled = { openRequests.addWeight = false }
            )
        }
        composable(Routes.IMPORT) {
            ImportScreen(
                onBack = { navController.popBackStack() },
                onOpenRecipe = { id ->
                    navController.popBackStack()
                    navController.navigate(Routes.recipeDetail(id))
                }
            )
        }
        composable(Routes.ABOUT) {
            AboutScreen(viewModel = updateViewModel, onBack = { navController.popBackStack() })
        }
        composable(Routes.RECIPES) {
            RecipesScreen(
                onRecipeClick = { navController.navigate(Routes.recipeDetail(it)) },
                onAddRecipe = { navController.navigate(Routes.recipeEdit()) },
                onImport = { navController.navigate(Routes.IMPORT) }
            )
        }
        composable(
            route = Routes.RECIPE_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_RECIPE_ID) { type = NavType.LongType })
        ) {
            RecipeDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.recipeEdit(it)) }
            )
        }
        composable(
            route = Routes.RECIPE_EDIT,
            arguments = listOf(navArgument(Routes.ARG_RECIPE_ID) {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) {
            RecipeEditScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PLANNER) {
            PlannerScreen(onOpenShopping = { navController.navigate(Routes.SHOPPING) })
        }
        composable(Routes.ACTIVITY) {
            ActivityScreen(
                openAddRequest = openRequests.addActivity,
                onAddRequestHandled = { openRequests.addActivity = false }
            )
        }
        composable(Routes.NOTES) {
            NotesScreen(
                onNoteClick = { navController.navigate(Routes.noteEdit(it)) },
                onAddNote = { navController.navigate(Routes.noteEdit()) }
            )
        }
        composable(
            route = Routes.NOTE_EDIT,
            arguments = listOf(navArgument(Routes.ARG_NOTE_ID) {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) {
            NoteEditScreen(onBack = { navController.popBackStack() })
        }
    }
}
