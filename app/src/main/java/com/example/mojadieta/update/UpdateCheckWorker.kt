package com.example.mojadieta.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.mojadieta.DietApplication
import java.util.concurrent.TimeUnit

/**
 * Zadanie w tle uruchamiane co 12 godzin: sprawdza GitHub i wysyła powiadomienie,
 * gdy pojawi się nowa wersja (tylko raz dla każdej wersji).
 */
class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as DietApplication).container
        val prefs = container.updatePreferences
        if (!prefs.notificationsEnabled) return Result.success()
        return try {
            val release = container.checkForUpdate()
            if (release != null && prefs.lastNotifiedVersion != release.versionName) {
                UpdateNotifier.showUpdateAvailable(applicationContext, release)
                prefs.lastNotifiedVersion = release.versionName
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "update_check"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(12, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setInitialDelay(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
