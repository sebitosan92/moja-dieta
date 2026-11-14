package com.example.mojadieta.data.update

import android.content.Context
import androidx.core.content.edit

/** Proste ustawienia dotyczące aktualizacji (SharedPreferences). */
class UpdatePreferences(context: Context) {

    private val prefs = context.getSharedPreferences("updates", Context.MODE_PRIVATE)

    /** Czy pokazywać powiadomienia o nowej wersji. */
    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS, true)
        set(value) = prefs.edit { putBoolean(KEY_NOTIFICATIONS, value) }

    /** Wersja, o której już wysłano powiadomienie – żeby nie powiadamiać kilka razy. */
    var lastNotifiedVersion: String?
        get() = prefs.getString(KEY_LAST_NOTIFIED, null)
        set(value) = prefs.edit { putString(KEY_LAST_NOTIFIED, value) }

    /** Wersja, dla której użytkownik kliknął „Później” w oknie w aplikacji. */
    var dismissedVersion: String?
        get() = prefs.getString(KEY_DISMISSED, null)
        set(value) = prefs.edit { putString(KEY_DISMISSED, value) }

    private companion object {
        const val KEY_NOTIFICATIONS = "notifications_enabled"
        const val KEY_LAST_NOTIFIED = "last_notified_version"
        const val KEY_DISMISSED = "dismissed_version"
    }
}
