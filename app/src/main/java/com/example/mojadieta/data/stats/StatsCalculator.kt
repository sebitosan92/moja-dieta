package com.example.mojadieta.data.stats

import com.example.mojadieta.data.repository.ActivityRepository
import com.example.mojadieta.data.settings.SettingsRepository
import com.example.mojadieta.domain.model.DayTotals
import com.example.mojadieta.domain.repository.DiaryRepository
import com.example.mojadieta.domain.repository.ProfileRepository
import com.example.mojadieta.domain.repository.WaterRepository
import com.example.mojadieta.domain.repository.WeightRepository
import com.example.mojadieta.domain.usecase.ActivityCalc
import com.example.mojadieta.domain.usecase.CalculateDailyGoalUseCase
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/** Stan dnia względem celu kalorii – kropki w kalendarzu i statystyki. */
enum class DayStatus { NONE, PLANNED_ONLY, IN_GOAL, OVER }

/** Tolerancja: do 5% ponad cel liczymy jako „w celu”. */
fun dayStatus(totals: DayTotals?, goalCalories: Int?): DayStatus = when {
    totals == null || totals.entries == 0 -> DayStatus.NONE
    totals.notEaten == totals.entries -> DayStatus.PLANNED_ONLY
    goalCalories == null || goalCalories <= 0 -> DayStatus.IN_GOAL
    totals.eatenCalories > goalCalories * 1.05 -> DayStatus.OVER
    else -> DayStatus.IN_GOAL
}

data class DayStat(val date: LocalDate, val calories: Int, val status: DayStatus, val waterMl: Int, val steps: Int, val activityMin: Int)

data class PeriodStats(
    val start: LocalDate,
    val end: LocalDate,
    val goalCalories: Int?,
    val loggedDays: Int,
    val avgCalories: Int,
    val avgProtein: Double,
    val avgCarbs: Double,
    val avgFat: Double,
    val daysInGoal: Int,
    val daysOver: Int,
    val avgWaterMl: Int,
    val waterGoalDays: Int,
    val weightStart: Double?,
    val weightEnd: Double?,
    val activityCount: Int,
    val activityMinutes: Int,
    val activityCalories: Int,
    val whoMinutes: Int,
    val avgSteps: Int,
    val stepsGoalDays: Int,
    val stepsGoal: Int,
    val days: List<DayStat>
) {
    val length: Int get() = days.size
    val weightChange: Double?
        get() = if (weightStart != null && weightEnd != null) Math.round((weightEnd - weightStart) * 10.0) / 10.0 else null
}

/** Liczy podsumowanie okresu z dziennika, wody, wagi, aktywności i kroków. */
class StatsCalculator(
    private val diaryRepository: DiaryRepository,
    private val waterRepository: WaterRepository,
    private val weightRepository: WeightRepository,
    private val activityRepository: ActivityRepository,
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository,
    private val calculateDailyGoal: CalculateDailyGoalUseCase
) {
    suspend fun compute(days: Int, end: LocalDate = LocalDate.now()): PeriodStats {
        val start = end.minusDays(days - 1L)
        val totals = diaryRepository.getTotals(start, end).associateBy { it.date }
        val water = waterRepository.getRange(start, end).associateBy { it.date }
        val weights = weightRepository.getBetween(start.minusDays(7), end).sortedBy { it.timeMillis }
        val activities = activityRepository.getBetween(start, end)
        val steps = activityRepository.getStepsBetween(start, end).associateBy { it.date }
        val goal = profileRepository.observeProfile().first()?.let { calculateDailyGoal(it).goal.calories }
        val stepsGoal = settingsRepository.current.stepsGoal

        val dayStats = (0 until days).map { offset ->
            val date = start.plusDays(offset.toLong())
            val t = totals[date]
            DayStat(
                date = date,
                calories = t?.eatenCalories ?: 0,
                status = dayStatus(t, goal),
                waterMl = water[date]?.amountMl ?: 0,
                steps = steps[date]?.steps ?: 0,
                activityMin = activities.filter { it.date == date }.sumOf { it.durationMin }
            )
        }
        val logged = totals.values.filter { it.entries > it.notEaten && it.eatenCalories > 0 }
        val waterDays = water.values.filter { it.amountMl > 0 }
        val stepDays = steps.values.filter { it.steps > 0 }
        // Waga na początku: ostatni pomiar przed okresem lub pierwszy w okresie.
        val weightStart = weights.lastOrNull { it.date.isBefore(start) }?.weightKg
            ?: weights.firstOrNull { !it.date.isBefore(start) }?.weightKg
        val weightEnd = weights.lastOrNull { !it.date.isAfter(end) && !it.date.isBefore(start) }?.weightKg

        fun avg(values: List<Double>) = if (values.isEmpty()) 0.0 else Math.round(values.average() * 10.0) / 10.0

        return PeriodStats(
            start = start,
            end = end,
            goalCalories = goal,
            loggedDays = logged.size,
            avgCalories = if (logged.isEmpty()) 0 else logged.map { it.eatenCalories }.average().toInt(),
            avgProtein = avg(logged.map { it.eatenProtein }),
            avgCarbs = avg(logged.map { it.eatenCarbs }),
            avgFat = avg(logged.map { it.eatenFat }),
            daysInGoal = dayStats.count { it.status == DayStatus.IN_GOAL },
            daysOver = dayStats.count { it.status == DayStatus.OVER },
            avgWaterMl = if (waterDays.isEmpty()) 0 else waterDays.map { it.amountMl }.average().toInt(),
            waterGoalDays = water.values.count { it.goalMl > 0 && it.amountMl >= it.goalMl },
            weightStart = weightStart,
            weightEnd = weightEnd,
            activityCount = activities.size,
            activityMinutes = activities.sumOf { it.durationMin },
            activityCalories = activities.sumOf { it.calories },
            whoMinutes = ActivityCalc.whoMinutes(activities),
            avgSteps = if (stepDays.isEmpty()) 0 else stepDays.map { it.steps }.average().toInt(),
            stepsGoalDays = stepDays.count { it.steps >= stepsGoal },
            stepsGoal = stepsGoal,
            days = dayStats
        )
    }
}
