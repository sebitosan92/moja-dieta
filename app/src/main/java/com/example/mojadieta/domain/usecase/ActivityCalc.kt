package com.example.mojadieta.domain.usecase

import com.example.mojadieta.domain.model.ActivityEntry
import com.example.mojadieta.domain.model.ActivityType
import com.example.mojadieta.domain.model.Intensity
import java.time.LocalDate
import kotlin.math.roundToInt

/** Szacowanie spalonych kalorii. Wynik ma charakter informacyjny – nie zmienia budżetu diety. */
object ActivityCalc {

    /** kcal = MET × masa ciała [kg] × czas [h]. */
    fun metCalories(type: ActivityType, intensity: Intensity, weightKg: Double, minutes: Int): Int =
        (type.met(intensity) * weightKg * minutes / 60.0).roundToInt()

    /**
     * Rower stacjonarny z pomiarem mocy: praca [kJ] = W × s / 1000, sprawność człowieka ~24%,
     * 1 kcal = 4,184 kJ → kcal ≈ W × min × 0,06.
     */
    fun wattCalories(watts: Int, minutes: Int): Int = (watts * minutes * 0.06).roundToInt()

    /** Kalorie: wpisane ręcznie > z mocy (W) > z MET. */
    fun estimate(
        type: ActivityType,
        intensity: Intensity,
        weightKg: Double,
        minutes: Int,
        watts: Int?,
        manualKcal: Int?
    ): Int = when {
        manualKcal != null && manualKcal > 0 -> manualKcal
        watts != null && watts > 0 -> wattCalories(watts, minutes)
        else -> metCalories(type, intensity, weightKg, minutes)
    }

    /**
     * Seria: liczba kolejnych dni z aktywnością, licząc od dzisiaj (albo od wczoraj,
     * jeśli dziś jeszcze nic nie było – seria trwa do końca dnia).
     */
    fun streak(activeDays: Set<LocalDate>, today: LocalDate = LocalDate.now()): Int {
        var day = if (today in activeDays) today else today.minusDays(1)
        var count = 0
        while (day in activeDays) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    /** Minuty „liczone do celu WHO”: umiarkowane 1:1, intensywne ×2, lekkie się nie liczą. */
    fun whoMinutes(entries: List<ActivityEntry>): Int = entries.sumOf {
        when (it.intensity) {
            Intensity.LIGHT -> 0
            Intensity.MODERATE -> it.durationMin
            Intensity.VIGOROUS -> it.durationMin * 2
        }
    }
}
