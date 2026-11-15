package com.example.mojadieta.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.mojadieta.domain.model.ActivityEntry
import com.example.mojadieta.domain.model.ActivityType
import com.example.mojadieta.domain.model.BodyMetric
import com.example.mojadieta.domain.model.BodyMetricType
import com.example.mojadieta.domain.model.DailySteps
import com.example.mojadieta.domain.model.Intensity
import com.example.mojadieta.domain.model.WeightEntry
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.reflect.KClass

/**
 * Odczyt danych z Health Connect (systemowa wymiana danych zdrowotnych Androida):
 * waga, skład ciała, treningi, kroki, aktywne kalorie, dystans i tętno.
 * Np. opaska Samsung → Samsung Health → Health Connect → Moja Dieta,
 * waga Huawei → Huawei Zdrowie → (Health Sync) → Health Connect → Moja Dieta.
 */
class HealthConnectManager(private val context: Context) {

    enum class Availability { AVAILABLE, NEEDS_UPDATE, NOT_SUPPORTED }

    private fun read(type: KClass<out Record>) = HealthPermission.getReadPermission(type)

    val weightPermissions: Set<String> = setOf(read(WeightRecord::class))

    val bodyPermissions: Set<String> = setOf(
        read(BodyFatRecord::class),
        read(LeanBodyMassRecord::class),
        read(BodyWaterMassRecord::class),
        read(BoneMassRecord::class)
    )

    val activityPermissions: Set<String> = setOf(
        read(ExerciseSessionRecord::class),
        read(StepsRecord::class),
        read(ActiveCaloriesBurnedRecord::class),
        read(DistanceRecord::class),
        read(HeartRateRecord::class)
    )

    /** Wszystkie uprawnienia, o które prosi aplikacja (użytkownik może zaznaczyć tylko część). */
    val permissions: Set<String> = weightPermissions + bodyPermissions + activityPermissions

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

    suspend fun grantedPermissions(): Set<String> {
        val c = client ?: return emptySet()
        return runCatching { c.permissionController.getGrantedPermissions() }.getOrDefault(emptySet())
    }

    /** Zgoda na odczyt wagi (zachowane dla zgodności z wersją 1.3). */
    suspend fun hasPermission(): Boolean = grantedPermissions().containsAll(weightPermissions)

    suspend fun hasAnyActivityPermission(): Boolean = grantedPermissions().any { it in activityPermissions }

    // ---------------- Waga ----------------

    /** Pomiary wagi z ostatnich [days] dni. */
    suspend fun readWeights(days: Long = 365): List<WeightEntry> {
        if (!grantedPermissions().containsAll(weightPermissions)) return emptyList()
        return readAll(WeightRecord::class, days).map { record ->
            WeightEntry(
                date = record.time.atZone(ZoneId.systemDefault()).toLocalDate(),
                timeMillis = record.time.toEpochMilli(),
                weightKg = Math.round(record.weight.inKilograms * 10.0) / 10.0,
                source = record.metadata.dataOrigin.packageName,
                externalId = record.metadata.id
            )
        }
    }

    // ---------------- Skład ciała ----------------

    suspend fun readBodyComposition(days: Long = 365): List<BodyMetric> {
        val granted = grantedPermissions()
        val result = mutableListOf<BodyMetric>()
        fun metric(type: BodyMetricType, time: Instant, value: Double, source: String, id: String) =
            BodyMetric(
                date = time.atZone(ZoneId.systemDefault()).toLocalDate(),
                timeMillis = time.toEpochMilli(),
                type = type,
                value = Math.round(value * 10.0) / 10.0,
                source = source,
                externalId = id
            )
        if (read(BodyFatRecord::class) in granted) {
            readAll(BodyFatRecord::class, days).forEach {
                result += metric(BodyMetricType.BODY_FAT, it.time, it.percentage.value, it.metadata.dataOrigin.packageName, it.metadata.id)
            }
        }
        if (read(LeanBodyMassRecord::class) in granted) {
            readAll(LeanBodyMassRecord::class, days).forEach {
                result += metric(BodyMetricType.LEAN_MASS, it.time, it.mass.inKilograms, it.metadata.dataOrigin.packageName, it.metadata.id)
            }
        }
        if (read(BodyWaterMassRecord::class) in granted) {
            readAll(BodyWaterMassRecord::class, days).forEach {
                result += metric(BodyMetricType.BODY_WATER, it.time, it.mass.inKilograms, it.metadata.dataOrigin.packageName, it.metadata.id)
            }
        }
        if (read(BoneMassRecord::class) in granted) {
            readAll(BoneMassRecord::class, days).forEach {
                result += metric(BodyMetricType.BONE_MASS, it.time, it.mass.inKilograms, it.metadata.dataOrigin.packageName, it.metadata.id)
            }
        }
        return result
    }

    // ---------------- Treningi ----------------

    /**
     * Sesje treningowe z ostatnich [days] dni. Kalorie z Health Connect (aktywne), a gdy ich brak –
     * szacunek MET × masa ciała × czas. Tętno i dystans z rekordów w czasie trwania sesji.
     */
    suspend fun readExercises(days: Long, weightKg: Double): List<ActivityEntry> {
        val c = client ?: return emptyList()
        val granted = grantedPermissions()
        if (read(ExerciseSessionRecord::class) !in granted) return emptyList()
        val metrics = buildSet {
            if (read(ActiveCaloriesBurnedRecord::class) in granted) add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
            if (read(DistanceRecord::class) in granted) add(DistanceRecord.DISTANCE_TOTAL)
            if (read(HeartRateRecord::class) in granted) add(HeartRateRecord.BPM_AVG)
        }
        return readAll(ExerciseSessionRecord::class, days).mapNotNull { session ->
            val minutes = Duration.between(session.startTime, session.endTime).toMinutes().toInt()
            if (minutes <= 0) return@mapNotNull null
            val type = mapExerciseType(session.exerciseType)
            val aggregated = if (metrics.isEmpty()) null else runCatching {
                c.aggregate(AggregateRequest(metrics, TimeRangeFilter.between(session.startTime, session.endTime)))
            }.getOrNull()
            val kcal = aggregated?.get(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)?.inKilocalories
            val distance = aggregated?.get(DistanceRecord.DISTANCE_TOTAL)?.inKilometers
            val hr = aggregated?.get(HeartRateRecord.BPM_AVG)
            val calories = kcal?.takeIf { it > 0 }?.let { Math.round(it).toInt() }
                ?: Math.round(type.met(Intensity.MODERATE) * weightKg * minutes / 60.0).toInt()
            ActivityEntry(
                date = session.startTime.atZone(ZoneId.systemDefault()).toLocalDate(),
                startMillis = session.startTime.toEpochMilli(),
                type = type,
                title = session.title?.takeIf { it.isNotBlank() },
                durationMin = minutes,
                intensity = Intensity.MODERATE,
                distanceKm = distance?.takeIf { it > 0.01 }?.let { Math.round(it * 100.0) / 100.0 },
                avgHeartRate = hr?.toInt()?.takeIf { it > 0 },
                calories = calories,
                source = session.metadata.dataOrigin.packageName,
                externalId = session.metadata.id
            )
        }
    }

    /** Kroki, aktywne kalorie i dystans dzień po dniu (ostatnie [days] dni łącznie z dzisiaj). */
    suspend fun readDailySteps(days: Long): List<DailySteps> {
        val c = client ?: return emptyList()
        val granted = grantedPermissions()
        val metrics = buildSet {
            if (read(StepsRecord::class) in granted) add(StepsRecord.COUNT_TOTAL)
            if (read(ActiveCaloriesBurnedRecord::class) in granted) add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
            if (read(DistanceRecord::class) in granted) add(DistanceRecord.DISTANCE_TOTAL)
        }
        if (metrics.isEmpty()) return emptyList()
        val today = LocalDate.now()
        val start = today.minusDays(days - 1).atStartOfDay()
        val end = today.plusDays(1).atStartOfDay()
        val groups = runCatching {
            c.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = metrics,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    timeRangeSlicer = Period.ofDays(1)
                )
            )
        }.getOrElse { return emptyList() }
        return groups.map { group ->
            DailySteps(
                date = group.startTime.toLocalDate(),
                steps = group.result[StepsRecord.COUNT_TOTAL]?.toInt() ?: 0,
                activeCalories = group.result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
                    ?.inKilocalories?.let { Math.round(it).toInt() } ?: 0,
                distanceKm = group.result[DistanceRecord.DISTANCE_TOTAL]?.inKilometers
                    ?.let { Math.round(it * 100.0) / 100.0 } ?: 0.0
            )
        }
    }

    private suspend fun <T : Record> readAll(type: KClass<T>, days: Long): List<T> {
        val c = client ?: return emptyList()
        val start = Instant.now().minus(days, ChronoUnit.DAYS)
        val result = mutableListOf<T>()
        var pageToken: String? = null
        try {
            do {
                val response = c.readRecords(
                    ReadRecordsRequest(
                        recordType = type,
                        timeRangeFilter = TimeRangeFilter.after(start),
                        pageToken = pageToken
                    )
                )
                result += response.records
                pageToken = response.pageToken
            } while (!pageToken.isNullOrEmpty())
        } catch (_: SecurityException) {
            // Brak zgody na ten typ danych – zwracamy to, co udało się odczytać.
        }
        return result
    }

    private fun mapExerciseType(type: Int): ActivityType = when (type) {
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> ActivityType.CYCLING
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> ActivityType.STATIONARY_BIKE
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> ActivityType.WALKING
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> ActivityType.RUNNING
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> ActivityType.HIKING
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> ActivityType.SWIMMING
        ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
        ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING -> ActivityType.GYM
        ExerciseSessionRecord.EXERCISE_TYPE_YOGA,
        ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING,
        ExerciseSessionRecord.EXERCISE_TYPE_PILATES -> ActivityType.YOGA
        ExerciseSessionRecord.EXERCISE_TYPE_DANCING -> ActivityType.DANCING
        ExerciseSessionRecord.EXERCISE_TYPE_TENNIS,
        ExerciseSessionRecord.EXERCISE_TYPE_BADMINTON,
        ExerciseSessionRecord.EXERCISE_TYPE_TABLE_TENNIS -> ActivityType.TENNIS
        ExerciseSessionRecord.EXERCISE_TYPE_SKATING -> ActivityType.ROLLER_SKATING
        ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
        ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS,
        ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS,
        ExerciseSessionRecord.EXERCISE_TYPE_BOOT_CAMP,
        ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL -> ActivityType.FITNESS
        else -> ActivityType.OTHER
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
            source.contains("garmin", ignoreCase = true) -> "Garmin Connect"
            source.contains("strava", ignoreCase = true) -> "Strava"
            else -> "Health Connect"
        }
    }
}
