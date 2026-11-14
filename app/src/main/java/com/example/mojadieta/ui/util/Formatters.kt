package com.example.mojadieta.ui.util

import java.text.Collator
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

val PolishLocale: Locale = Locale.forLanguageTag("pl-PL")

val PolishCollator: Collator = Collator.getInstance(PolishLocale).apply { strength = Collator.PRIMARY }

private val shortDayNames = listOf("Pon", "Wt", "Śr", "Czw", "Pt", "Sob", "Nd")
private val fullDayNames = listOf("Poniedziałek", "Wtorek", "Środa", "Czwartek", "Piątek", "Sobota", "Niedziela")
private val monthsGenitive = listOf(
    "stycznia", "lutego", "marca", "kwietnia", "maja", "czerwca",
    "lipca", "sierpnia", "września", "października", "listopada", "grudnia"
)
private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm", PolishLocale)

fun LocalDate.startOfWeek(): LocalDate = this.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

fun LocalDate.shortDayName(): String = shortDayNames[dayOfWeek.value - 1]

fun LocalDate.fullDayName(): String = fullDayNames[dayOfWeek.value - 1]

/** np. "14 listopada" */
fun LocalDate.dayMonth(): String = "$dayOfMonth ${monthsGenitive[monthValue - 1]}"

/** np. "Sobota, 14 listopada 2026" */
fun LocalDate.fullPolishDate(): String = "${fullDayName()}, ${dayMonth()} $year"

/** np. "10 – 16 listopada 2026" albo "28 października – 3 listopada 2026" */
fun weekRangeLabel(weekStart: LocalDate): String {
    val end = weekStart.plusDays(6)
    return when {
        weekStart.month == end.month -> "${weekStart.dayOfMonth} – ${end.dayMonth()} ${end.year}"
        weekStart.year == end.year -> "${weekStart.dayMonth()} – ${end.dayMonth()} ${end.year}"
        else -> "${weekStart.dayMonth()} ${weekStart.year} – ${end.dayMonth()} ${end.year}"
    }
}

fun formatTimestamp(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(dateTimeFormatter)

/** Formatuje gramy z polskim przecinkiem dziesiętnym, pomijając zbędne ",0". */
fun formatGrams(value: Double): String {
    val rounded = Math.round(value * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toLong().toString()
    } else {
        String.format(PolishLocale, "%.1f", rounded)
    }
}

/** Parsuje liczbę wpisaną przez użytkownika – akceptuje przecinek i kropkę. */
fun parseDecimal(input: String): Double? = input.trim().replace(',', '.').toDoubleOrNull()

fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun utcMillisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
