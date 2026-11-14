package com.example.mojadieta

import android.app.Application
import com.example.mojadieta.di.AppContainer
import com.example.mojadieta.update.UpdateCheckWorker
import com.example.mojadieta.update.UpdateNotifier

class DietApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        UpdateNotifier.createChannel(this)
        UpdateCheckWorker.schedule(this)
    }
}
