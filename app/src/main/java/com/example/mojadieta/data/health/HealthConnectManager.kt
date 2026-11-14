package com.example.mojadieta.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.mojadieta.domain.model.WeightEntry
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Odczyt pomiarów wagi z Health Connect (systemowa wymiana danych zdrowotnych Androida).
 * Np. waga Huawei → Huawei Zdrowie → (Health Sync) → Health Connect → Moja Dieta.
 */
class HealthConnectManager(private val context: Context) {

    enum class Availability { AVAILABLE, NEEDS_UPDATE, NOT_SUPPORTED }

    val permissions: Set<String> = setOf(HealthPermission.getReadPermission(WeightRecord::class))

    fun availability(): Availability = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> Availability.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> Availability.NEEDS_UPDATE
        else -> Availability.NOT_SUPPORTED
    }

    private val client: HealthConnectClient? by lazy {
        if (availability() == Availability.AVAILABLE) HealthConnectClient.getOrCreate(context) else null
    }

    /** Kontrakt do systemowego okna zgody Health Connect. */
    fun permissionContract() = PermissionController.createRequestPermissionResultContract()

    suspend fun hasPermission(): Boolean {
        val c = client ?: return false
        return c.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    /** Pomiary wagi z ostatnich [days] dni. */
    suspend fun readWeights(days: Long = 365): List<WeightEntry> {
        val c = client ?: return emptyList()
        val start = Instant.now().minus(days, ChronoUnit.DAYS)
        val result = mutableListOf<WeightEntry>()
        var pageToken: String? = null
        do {
            val response = c.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.after(start),
                    pageToken = pageToken
                )
            )
            response.records.forEach { record ->
                result += WeightEntry(
                    date = record.time.atZone(ZoneId.systemDefault()).toLocalDate(),
                    timeMillis = record.time.toEpochMilli(),
                    weightKg = Math.round(record.weight.inKilograms * 10.0) / 10.0,
                    source = record.metadata.dataOrigin.packageName,
                    externalId = record.metadata.id
                )
            }
            pageToken = response.pageToken
        } while (!pageToken.isNullOrEmpty())
        return result
    }

    companion object {
        /** Czytelna nazwa aplikacji źródłowej. */
        fun sourceLabel(source: String): String = when {
            source == WeightEntry.SOURCE_MANUAL -> "Wpis ręczny"
            source.startsWith("com.huawei.health") -> "Huawei Zdrowie"
            source.startsWith("nl.appyhapps.healthsync") -> "Health Sync"
            source.startsWith("com.google.android.apps.fitness") -> "Google Fit"
            source.startsWith("com.sec.android.app.shealth") -> "Samsung Health"
            source.contains("xiaomi", ignoreCase = true) || source.contains("mi.health") -> "Mi Fitness"
            else -> "Health Connect"
        }
    }
}
