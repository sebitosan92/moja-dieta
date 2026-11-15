package com.example.mojadieta

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.widget.Toast
import com.example.mojadieta.reminders.AppActions
import com.example.mojadieta.widget.DietWidgetUpdater
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.example.mojadieta.ui.navigation.DietAppRoot
import com.example.mojadieta.ui.theme.MojaDietaTheme
import com.example.mojadieta.ui.util.PolishLocale

class MainActivity : ComponentActivity() {

    /** Akcja do wykonania po otwarciu (powiadomienie, skrót, widżet); null = brak. */
    private val pendingRequest = mutableStateOf<String?>(null)

    /**
     * Aplikacja jest w całości po polsku – wymuszamy polską lokalizację niezależnie od języka
     * systemu, aby komponenty Material (np. kalendarz DatePicker) też były po polsku
     * i tydzień zaczynał się od poniedziałku.
     */
    override fun attachBaseContext(newBase: Context) {
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(PolishLocale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            val settings by (application as DietApplication).container.settingsRepository.settings.collectAsState()
            val dark = when (settings.themeMode) {
                com.example.mojadieta.domain.settings.ThemeMode.LIGHT -> false
                com.example.mojadieta.domain.settings.ThemeMode.DARK -> true
                com.example.mojadieta.domain.settings.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            // Ikony paska stanu i nawigacji zgodne z motywem aplikacji (nie tylko telefonu).
            androidx.compose.runtime.LaunchedEffect(dark) {
                val style = androidx.activity.SystemBarStyle.auto(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                ) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            MojaDietaTheme(darkTheme = dark) {
                DietAppRoot(
                    request = pendingRequest.value,
                    onRequestHandled = { pendingRequest.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        if (intent.getBooleanExtra(EXTRA_OPEN_UPDATES, false)) {
            pendingRequest.value = REQUEST_UPDATES
            intent.removeExtra(EXTRA_OPEN_UPDATES)
            return
        }
        val action = intent.action ?: return
        if (action == Intent.ACTION_MAIN) return
        // Obsłużona akcja nie może wrócić po obróceniu ekranu.
        intent.action = Intent.ACTION_MAIN
        if (action == AppActions.ADD_WATER) {
            val app = application as DietApplication
            app.appScope.launch {
                app.container.waterRepository.changeAmount(LocalDate.now(), 250)
                DietWidgetUpdater.requestUpdate(app)
            }
            Toast.makeText(this, "Dodano 250 ml wody", Toast.LENGTH_SHORT).show()
            pendingRequest.value = AppActions.OPEN_DIARY
        } else {
            pendingRequest.value = action
        }
    }

    companion object {
        const val EXTRA_OPEN_UPDATES = "open_updates"
        const val REQUEST_UPDATES = "updates"
    }
}
