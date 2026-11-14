package com.example.mojadieta

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.mojadieta.ui.navigation.DietAppRoot
import com.example.mojadieta.ui.theme.MojaDietaTheme
import com.example.mojadieta.ui.util.PolishLocale

class MainActivity : ComponentActivity() {

    /** true, gdy aplikację otwarto z powiadomienia o aktualizacji. */
    private val openUpdatesRequest = mutableStateOf(false)

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
            MojaDietaTheme {
                DietAppRoot(
                    openUpdatesRequest = openUpdatesRequest.value,
                    onOpenUpdatesHandled = { openUpdatesRequest.value = false }
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
        if (intent?.getBooleanExtra(EXTRA_OPEN_UPDATES, false) == true) {
            openUpdatesRequest.value = true
            intent.removeExtra(EXTRA_OPEN_UPDATES)
        }
    }

    companion object {
        const val EXTRA_OPEN_UPDATES = "open_updates"
    }
}
