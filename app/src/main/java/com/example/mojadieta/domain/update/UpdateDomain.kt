package com.example.mojadieta.domain.update

import java.io.File

/** Opis wydania aplikacji opublikowanego na GitHubie. */
data class AppRelease(
    val versionName: String,
    val title: String,
    val notes: String,
    val apkUrl: String,
    val apkSizeBytes: Long,
    val pageUrl: String
)

interface UpdateRepository {
    /** Pobiera informacje o najnowszym wydaniu albo null, gdy wydania nie ma lub brak w nim pliku APK. */
    suspend fun fetchLatestRelease(): AppRelease?

    /** Pobiera plik APK wydania, raportując postęp w zakresie 0..1. */
    suspend fun downloadApk(release: AppRelease, onProgress: (Float) -> Unit): File
}

object VersionComparator {
    /** Zamienia „v1.2.3” / „1.2” na listę liczb [1, 2, 3]. */
    fun parse(version: String): List<Int> =
        version.trim().removePrefix("v").removePrefix("V")
            .substringBefore('-')
            .split('.')
            .map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }

    /** true, gdy [candidate] jest nowsza niż [current]. */
    fun isNewer(candidate: String, current: String): Boolean {
        val a = parse(candidate)
        val b = parse(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}

/** Zwraca nowsze wydanie niż zainstalowana wersja albo null. */
class CheckForUpdateUseCase(
    private val repository: UpdateRepository,
    private val currentVersion: String
) {
    suspend operator fun invoke(): AppRelease? {
        val latest = repository.fetchLatestRelease() ?: return null
        return if (VersionComparator.isNewer(latest.versionName, currentVersion)) latest else null
    }
}
