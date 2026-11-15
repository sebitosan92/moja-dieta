package com.example.mojadieta.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.mojadieta.DietApplication
import com.example.mojadieta.MainActivity
import com.example.mojadieta.R
import com.example.mojadieta.domain.model.MealType
import com.example.mojadieta.domain.settings.AppSettings
import com.example.mojadieta.update.UpdateNotifier
import com.example.mojadieta.ui.util.formatGrams
import com.example.mojadieta.ui.util.formatMinutes
import com.example.mojadieta.ui.util.formatThousands
import com.example.mojadieta.widget.DietWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Rodzaje przypomnień; [code] = identyfikator alarmu i powiadomienia. */
enum class ReminderKind(val code: Int, val mealType: MealType? = null) {
    WEIGH(1),
    WATER(2),
    MEAL_BREAKFAST(10, MealType.BREAKFAST),
    MEAL_SECOND_BREAKFAST(11, MealType.SECOND_BREAKFAST),
    MEAL_LUNCH(12, MealType.LUNCH),
    MEAL_AFTERNOON_SNACK(13, MealType.AFTERNOON_SNACK),
    MEAL_DINNER(14, MealType.DINNER),
    PLANNING(20),
    SUMMARY(21),
    FAST_START(30),
    FAST_END(31);

    companion object {
        fun forMeal(type: MealType) = entries.first { it.mealType == type }
        fun fromName(name: String?) = entries.firstOrNull { it.name == name }
    }
}

/** Akcje przekazywane do MainActivity (powiadomienia, skróty, widżet, kafelek). */
object AppActions {
    const val ADD_WATER = "com.example.mojadieta.action.ADD_WATER"
    const val SCAN = "com.example.mojadieta.action.SCAN"
    const val ADD_WEIGHT = "com.example.mojadieta.action.ADD_WEIGHT"
    const val ADD_ACTIVITY = "com.example.mojadieta.action.ADD_ACTIVITY"
    const val OPEN_DIARY = "com.example.mojadieta.action.OPEN_DIARY"
    const val OPEN_PLANNER = "com.example.mojadieta.action.OPEN_PLANNER"
    const val OPEN_STATS = "com.example.mojadieta.action.OPEN_STATS"
}

object ReminderScheduler {

    const val CHANNEL_REMINDERS = "reminders"
    const val CHANNEL_SUMMARY = "summary"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, "Przypomnienia", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Ważenie, woda, posiłki, planowanie i post przerywany"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_SUMMARY, "Podsumowanie tygodnia", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Niedzielne podsumowanie kalorii, wody, wagi i aktywności"
            }
        )
    }

    /** Ustawia (albo anuluje) alarmy dla wszystkich przypomnień zgodnie z ustawieniami. */
    fun rescheduleAll(context: Context, settings: AppSettings) {
        ReminderKind.entries.forEach { kind -> schedule(context, kind, settings) }
    }

    fun schedule(context: Context, kind: ReminderKind, settings: AppSettings, now: LocalDateTime = LocalDateTime.now()) {
        val next = nextTrigger(kind, settings, now)
        val pi = alarmIntent(context, kind)
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        if (next == null) {
            alarmManager.cancel(pi)
            return
        }
        val millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        setAlarm(alarmManager, millis, pi)
    }

    /** Dokładne alarmy, jeśli użytkownik na nie pozwolił; inaczej przybliżone (do kilkudziesięciu minut później). */
    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    private fun setAlarm(alarmManager: AlarmManager, millis: Long, pi: PendingIntent) {
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        try {
            if (exact) alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
            else alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        }
    }

    /** „Później” – jednorazowe przypomnienie za [minutes] minut (osobny alarm, nie zmienia harmonogramu). */
    fun snooze(context: Context, kind: ReminderKind, minutes: Int) {
        val millis = System.currentTimeMillis() + minutes * 60_000L
        setAlarm(context.getSystemService(AlarmManager::class.java), millis, alarmIntent(context, kind, snoozed = true))
    }

    private fun alarmIntent(context: Context, kind: ReminderKind, snoozed: Boolean = false): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(if (snoozed) ReminderReceiver.ACTION_SNOOZED else ReminderReceiver.ACTION_FIRE)
            .putExtra(ReminderReceiver.EXTRA_KIND, kind.name)
        return PendingIntent.getBroadcast(
            context,
            kind.code + if (snoozed) 500 else 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Najbliższy termin przypomnienia po [now] albo null, gdy wyłączone. */
    fun nextTrigger(kind: ReminderKind, s: AppSettings, now: LocalDateTime): LocalDateTime? {
        fun daily(time: LocalTime): LocalDateTime {
            val today = now.toLocalDate().atTime(time)
            return if (today.isAfter(now)) today else today.plusDays(1)
        }
        fun weekly(day: DayOfWeek, time: LocalTime): LocalDateTime {
            var candidate = now.toLocalDate().with(TemporalAdjusters.nextOrSame(day)).atTime(time)
            if (!candidate.isAfter(now)) candidate = candidate.plusWeeks(1)
            return candidate
        }
        return when (kind) {
            ReminderKind.WEIGH -> {
                if (!s.weighReminder || s.weighDays.isEmpty()) return null
                val time = LocalTime.of(s.weighHour, s.weighMinute)
                (0L..7L).map { now.toLocalDate().plusDays(it).atTime(time) }
                    .firstOrNull { it.isAfter(now) && it.dayOfWeek in s.weighDays }
            }
            ReminderKind.WATER -> {
                if (!s.waterReminder) return null
                val every = s.waterEveryHours.coerceIn(1, 6)
                val from = s.waterFromHour.coerceIn(0, 23)
                val to = s.waterToHour.coerceIn(from, 23)
                val slots = generateSequence(from) { it + every }.takeWhile { it <= to }.toList()
                val today = now.toLocalDate()
                slots.map { today.atTime(it, 0) }.firstOrNull { it.isAfter(now) }
                    ?: today.plusDays(1).atTime(from, 0)
            }
            ReminderKind.PLANNING ->
                if (s.planningReminder) weekly(s.planningDay, LocalTime.of(s.planningHour, 0)) else null
            ReminderKind.SUMMARY ->
                if (s.weeklySummary) weekly(DayOfWeek.SUNDAY, LocalTime.of(20, 0)) else null
            ReminderKind.FAST_START ->
                if (s.fastingEnabled) daily(LocalTime.of(s.eatingStartHour, s.eatingStartMinute)) else null
            ReminderKind.FAST_END ->
                if (s.fastingEnabled) daily(LocalTime.of(s.eatingStartHour, s.eatingStartMinute).plusHours(s.eatingWindowHours.toLong())) else null
            else -> {
                val type = kind.mealType ?: return null
                val reminder = s.mealReminders[type] ?: return null
                if (reminder.enabled) daily(LocalTime.of(reminder.hour, reminder.minute)) else null
            }
        }
    }
}

/** Odbiera alarmy przypomnień, sprawdza warunki („czy już zważono” itp.) i pokazuje powiadomienie. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as DietApplication
        val pending = goAsync()
        scope.launch {
            try {
                handle(app, intent)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handle(app: DietApplication, intent: Intent) {
        val container = app.container
        val settings = container.settingsRepository.current
        val kind = ReminderKind.fromName(intent.getStringExtra(EXTRA_KIND))
        when (intent.action) {
            ACTION_LATER -> {
                kind ?: return
                NotificationManagerCompat.from(app).cancel(NOTIFICATION_BASE + kind.code)
                ReminderScheduler.snooze(app, kind, if (kind == ReminderKind.WEIGH) 60 else 30)
                return
            }
            ACTION_WATER_250 -> {
                container.waterRepository.changeAmount(LocalDate.now(), 250)
                NotificationManagerCompat.from(app).cancel(NOTIFICATION_BASE + ReminderKind.WATER.code)
                DietWidgetUpdater.requestUpdate(app)
                return
            }
        }
        kind ?: return
        // Zwykły alarm: od razu planujemy kolejny termin.
        if (intent.action == ACTION_FIRE) ReminderScheduler.schedule(app, kind, settings)

        val today = LocalDate.now()
        when (kind) {
            ReminderKind.WEIGH -> {
                runCatching { container.syncWeightsFromHealthConnect() }
                if (container.weightRepository.hasEntryOn(today)) return
                notify(
                    app, kind,
                    title = "Czas na ważenie",
                    text = "Zważ się rano, na czczo – dla porównywalnych wyników.",
                    action = AppActions.ADD_WEIGHT,
                    buttons = listOf(
                        button(app, "Dodaj pomiar", openApp(app, AppActions.ADD_WEIGHT, kind.code + 900)),
                        button(app, "Później", receiverIntent(app, ACTION_LATER, kind))
                    )
                )
            }
            ReminderKind.WATER -> {
                val water = container.waterRepository.getWater(today)
                if (water.amountMl >= water.goalMl) return
                notify(
                    app, kind,
                    title = "Napij się wody",
                    text = "Dziś ${formatThousands(water.amountMl)} z ${formatThousands(water.goalMl)} ml.",
                    action = AppActions.OPEN_DIARY,
                    buttons = listOf(button(app, "+250 ml", receiverIntent(app, ACTION_WATER_250, kind)))
                )
            }
            ReminderKind.PLANNING -> {
                val nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                if (container.plannerRepository.countPlanned(nextMonday, nextMonday.plusDays(6)) >= 7) return
                notify(
                    app, kind,
                    title = "Zaplanuj nowy tydzień",
                    text = "Kilka minut z planerem i lista zakupów zrobi się sama.",
                    action = AppActions.OPEN_PLANNER
                )
            }
            ReminderKind.SUMMARY -> {
                val stats = container.statsCalculator.compute(7, today)
                if (stats.loggedDays == 0 && stats.activityCount == 0 && stats.weightEnd == null) return
                val lines = buildList {
                    if (stats.loggedDays > 0) {
                        add("Średnio ${stats.avgCalories} kcal/dzień" + (stats.goalCalories?.let { " (cel $it)" } ?: ""))
                        add("Dni w celu: ${stats.daysInGoal}/7")
                    }
                    if (stats.avgWaterMl > 0) add("Woda: średnio ${formatThousands(stats.avgWaterMl)} ml")
                    stats.weightChange?.let { add("Waga: ${if (it > 0) "+" else ""}${formatGrams(it)} kg") }
                    if (stats.activityMinutes > 0) add("Aktywność: ${formatMinutes(stats.activityMinutes)}")
                    if (stats.avgSteps > 0) add("Kroki: średnio ${formatThousands(stats.avgSteps)}")
                }
                notify(
                    app, kind,
                    title = "Podsumowanie tygodnia",
                    text = lines.joinToString("\n"),
                    action = AppActions.OPEN_STATS,
                    channel = ReminderScheduler.CHANNEL_SUMMARY
                )
            }
            ReminderKind.FAST_START -> {
                val end = LocalTime.of(settings.eatingStartHour, settings.eatingStartMinute)
                    .plusHours(settings.eatingWindowHours.toLong())
                notify(
                    app, kind,
                    title = "Okno jedzenia otwarte",
                    text = "Post ${settings.fastingHours}:${settings.eatingWindowHours} – możesz jeść do %02d:%02d.".format(end.hour, end.minute),
                    action = AppActions.OPEN_DIARY
                )
            }
            ReminderKind.FAST_END -> {
                val start = LocalTime.of(settings.eatingStartHour, settings.eatingStartMinute)
                notify(
                    app, kind,
                    title = "Koniec okna jedzenia",
                    text = "Zaczyna się post. Kolejny posiłek od %02d:%02d. Woda i niesłodzona herbata są OK.".format(start.hour, start.minute),
                    action = AppActions.OPEN_DIARY
                )
            }
            else -> {
                val type = kind.mealType ?: return
                if (container.diaryRepository.hasEntries(today, type)) return
                notify(
                    app, kind,
                    title = "${type.label} – pamiętasz?",
                    text = "Dodaj posiłek do dziennika, żeby bilans dnia się zgadzał.",
                    action = AppActions.OPEN_DIARY,
                    buttons = listOf(button(app, "Później", receiverIntent(app, ACTION_LATER, kind)))
                )
            }
        }
    }

    private fun notify(
        context: Context,
        kind: ReminderKind,
        title: String,
        text: String,
        action: String,
        buttons: List<NotificationCompat.Action> = emptyList(),
        channel: String = ReminderScheduler.CHANNEL_REMINDERS
    ) {
        if (!UpdateNotifier.canNotify(context)) return
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF1F9D74.toInt())
            .setContentTitle(title)
            .setContentText(text.lineSequence().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp(context, action, kind.code))
            .setAutoCancel(true)
        buttons.forEach { builder.addAction(it) }
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_BASE + kind.code, builder.build())
        } catch (_: SecurityException) {
        }
    }

    private fun button(context: Context, label: String, intent: PendingIntent) =
        NotificationCompat.Action.Builder(0, label, intent).build()

    private fun openApp(context: Context, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode + 3000,
            Intent(context, MainActivity::class.java)
                .setAction(action)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun receiverIntent(context: Context, action: String, kind: ReminderKind): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            kind.code + 1000 + action.hashCode() % 100,
            Intent(context, ReminderReceiver::class.java).setAction(action).putExtra(EXTRA_KIND, kind.name),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    companion object {
        const val ACTION_FIRE = "com.example.mojadieta.reminder.FIRE"
        const val ACTION_SNOOZED = "com.example.mojadieta.reminder.SNOOZED"
        const val ACTION_LATER = "com.example.mojadieta.reminder.LATER"
        const val ACTION_WATER_250 = "com.example.mojadieta.reminder.WATER_250"
        const val EXTRA_KIND = "kind"
        private const val NOTIFICATION_BASE = 2000
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}

/** Po restarcie telefonu, aktualizacji aplikacji lub zmianie czasu alarmy trzeba ustawić od nowa. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as DietApplication
        ReminderScheduler.rescheduleAll(app, app.container.settingsRepository.current)
    }
}
