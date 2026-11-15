package com.example.mojadieta.ui.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.mojadieta.domain.settings.AppSettings
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/** Stan postu w danej chwili: okno jedzenia albo post, z czasem do końca. */
data class FastingStatus(val eating: Boolean, val remaining: Duration, val total: Duration, val endsAt: LocalTime) {
    val progress: Float get() = if (total.isZero) 0f else (1f - remaining.seconds.toFloat() / total.seconds).coerceIn(0f, 1f)
}

fun fastingStatus(s: AppSettings, now: LocalDateTime = LocalDateTime.now()): FastingStatus {
    val window = Duration.ofHours(s.eatingWindowHours.toLong())
    val fast = Duration.ofHours(s.fastingHours.toLong())
    val startToday = now.toLocalDate().atTime(s.eatingStartHour, s.eatingStartMinute)
    // Szukamy ostatniego początku okna jedzenia, który już minął.
    val lastStart = if (!startToday.isAfter(now)) startToday else startToday.minusDays(1)
    val windowEnd = lastStart.plus(window)
    return if (now.isBefore(windowEnd)) {
        FastingStatus(true, Duration.between(now, windowEnd), window, windowEnd.toLocalTime())
    } else {
        val nextStart = lastStart.plusDays(1)
        FastingStatus(false, Duration.between(now, nextStart), fast, nextStart.toLocalTime())
    }
}

private fun Duration.label(): String {
    val h = toHours()
    val m = toMinutes() % 60
    return if (h > 0) "$h h $m min" else "$m min"
}

@Composable
fun FastingCard(settings: AppSettings, modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = LocalDateTime.now()
        }
    }
    val status = fastingStatus(settings, now)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (status.eating) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (status.eating) Icons.Outlined.Restaurant else Icons.Outlined.Timer, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (status.eating) "Okno jedzenia" else "Post ${settings.fastingHours}:${settings.eatingWindowHours}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "do %02d:%02d".format(status.endsAt.hour, status.endsAt.minute),
                    style = MaterialTheme.typography.labelLarge
                )
            }
            LinearProgressIndicator(
                progress = { status.progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
            )
            Text(
                if (status.eating) "Zostało ${status.remaining.label()} na posiłki."
                else "Jeszcze ${status.remaining.label()} postu. Woda, czarna kawa i herbata są OK.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
