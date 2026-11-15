package com.example.mojadieta

import android.app.Application
import com.example.mojadieta.di.AppContainer
import com.example.mojadieta.widget.DietWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.example.mojadieta.update.UpdateCheckWorker
import com.example.mojadieta.update.UpdateNotifier
import com.example.mojadieta.reminders.ReminderScheduler

class DietApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        UpdateNotifier.createChannel(this)
        ReminderScheduler.createChannels(this)
        UpdateCheckWorker.schedule(this)
        // Przypomnienia ustawiane od nowa przy każdej zmianie ustawień (i przy starcie).
        appScope.launch {
            container.settingsRepository.settings.collect { ReminderScheduler.rescheduleAll(this@DietApplication, it) }
        }
        appScope.launch { runCatching { container.cleanupImages() } }
        DietWidgetUpdater.start(this, container, appScope)
    }

    /** Zakres korutyn na czas życia procesu (sprzątanie, odświeżanie widżetu). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
