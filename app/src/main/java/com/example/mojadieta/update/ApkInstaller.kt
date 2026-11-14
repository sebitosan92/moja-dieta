package com.example.mojadieta.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

/** Uruchamia systemowy instalator dla pobranego pliku APK. */
object ApkInstaller {

    enum class Result { STARTED, NEEDS_PERMISSION, FAILED }

    /** Czy aplikacja ma zgodę na instalowanie aplikacji z nieznanych źródeł. */
    fun canInstall(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    /** Otwiera ustawienia, w których użytkownik zezwala na instalowanie aktualizacji. */
    fun openInstallPermissionSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun install(context: Context, apk: File): Result {
        if (!canInstall(context)) {
            openInstallPermissionSettings(context)
            return Result.NEEDS_PERMISSION
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            Result.STARTED
        } catch (e: ActivityNotFoundException) {
            Result.FAILED
        }
    }
}
