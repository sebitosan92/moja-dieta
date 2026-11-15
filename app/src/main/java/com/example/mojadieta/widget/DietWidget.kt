package com.example.mojadieta.widget

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.mojadieta.DietApplication
import com.example.mojadieta.MainActivity
import com.example.mojadieta.R
import com.example.mojadieta.di.AppContainer
import com.example.mojadieta.domain.model.DailySteps
import com.example.mojadieta.domain.model.MealEntry
import com.example.mojadieta.domain.model.UserProfile
import com.example.mojadieta.domain.model.WaterIntake
import com.example.mojadieta.domain.model.WeightEntry
import com.example.mojadieta.ui.util.formatGrams
import com.example.mojadieta.ui.util.formatThousands
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

// Kolory widżetu dla trybu jasnego i ciemnego.
private val Mint = ColorProvider(day = Color(0xFF1F9D74), night = Color(0xFF5FD3A8))
private val MintDark = ColorProvider(day = Color(0xFF0A5C42), night = Color(0xFF9BE8C9))
private val MintTrack = ColorProvider(day = Color(0xFFCDF3E3), night = Color(0xFF1E3A31))
private val Water = ColorProvider(day = Color(0xFF2E9FD8), night = Color(0xFF7CC8F0))
private val WaterLight = ColorProvider(day = Color(0xFFDDF0FA), night = Color(0xFF173446))
private val CardBg = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF1A2420))
private val TextDark = ColorProvider(day = Color(0xFF17201C), night = Color(0xFFE6EFEA))
private val TextMuted = ColorProvider(day = Color(0xFF55635D), night = Color(0xFFA7B6AF))

/** Widżet „Moja Dieta”: kalorie dnia, woda z szybkim dodawaniem, waga i kroki. */
class DietWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE, WIDE_TALL))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as DietApplication).container
        val today = LocalDate.now()
        provideContent {
            val water by container.waterRepository.observeWater(today).collectAsState(initial = null)
            val meals by container.diaryRepository.observeMeals(today).collectAsState(initial = emptyList())
            val profile by container.profileRepository.observeProfile().collectAsState(initial = null)
            val weight by container.weightRepository.observeLatest().collectAsState(initial = null)
            val steps by container.activityRepository.observeSteps(today).collectAsState(initial = null)
            WidgetContent(container, water, meals, profile, weight, steps)
        }
    }

    companion object {
        private val SMALL = DpSize(110.dp, 110.dp)
        private val WIDE = DpSize(250.dp, 110.dp)
        private val WIDE_TALL = DpSize(250.dp, 170.dp)
    }
}

@Composable
private fun WidgetContent(
    container: AppContainer,
    water: WaterIntake?,
    meals: List<MealEntry>,
    profile: UserProfile?,
    weight: WeightEntry?,
    steps: DailySteps?
) {
    val size = LocalSize.current
    val wide = size.width >= 250.dp
    val tall = size.height >= 170.dp
    val eaten = meals.filter { it.isEaten }.sumOf { it.calories }
    val goalKcal = profile?.let { container.calculateDailyGoal(it).goal.calories }
    val waterMl = water?.amountMl ?: 0
    val waterGoal = water?.goalMl ?: WaterIntake.DEFAULT_GOAL_ML
    val stepsGoal = container.settingsRepository.current.stepsGoal

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(CardBg)
            .cornerRadius(24.dp)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Text("Moja Dieta", style = TextStyle(color = MintDark, fontSize = 13.sp, fontWeight = FontWeight.Bold))
        Spacer(GlanceModifier.height(6.dp))
        if (wide) {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Column(modifier = GlanceModifier.defaultWeight()) { CaloriesBlock(eaten, goalKcal) }
                Spacer(GlanceModifier.width(12.dp))
                Column(modifier = GlanceModifier.defaultWeight()) { WaterBlock(waterMl, waterGoal) }
            }
            val extras = buildList {
                weight?.let { add("Waga ${formatGrams(it.weightKg)} kg") }
                steps?.takeIf { it.steps > 0 }?.let { add("Kroki ${formatThousands(it.steps)} / ${formatThousands(stepsGoal)}") }
            }
            if (extras.isNotEmpty()) {
                Spacer(GlanceModifier.height(if (tall) 10.dp else 6.dp))
                Text(extras.joinToString("  ·  "), style = TextStyle(color = TextMuted, fontSize = 12.sp), maxLines = 1)
                if (tall && steps != null && steps.steps > 0) {
                    Spacer(GlanceModifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = (steps.steps.toFloat() / stepsGoal.coerceAtLeast(1)).coerceIn(0f, 1f),
                        modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                        color = Mint,
                        backgroundColor = MintTrack
                    )
                }
            }
        } else {
            WaterBlock(waterMl, waterGoal)
            Spacer(GlanceModifier.height(4.dp))
            Text(
                if (goalKcal != null) "$eaten / $goalKcal kcal" else "$eaten kcal",
                style = TextStyle(color = TextMuted, fontSize = 11.sp)
            )
        }
    }
}

@Composable
private fun CaloriesBlock(eaten: Int, goal: Int?) {
    Text("$eaten", style = TextStyle(color = Mint, fontSize = 24.sp, fontWeight = FontWeight.Bold))
    Text(if (goal != null) "z $goal kcal" else "kcal zjedzone", style = TextStyle(color = TextMuted, fontSize = 12.sp))
    Spacer(GlanceModifier.height(4.dp))
    LinearProgressIndicator(
        progress = if (goal != null && goal > 0) (eaten.toFloat() / goal).coerceIn(0f, 1f) else 0f,
        modifier = GlanceModifier.fillMaxWidth().height(6.dp),
        color = Mint,
        backgroundColor = MintTrack
    )
    if (goal != null) {
        Spacer(GlanceModifier.height(4.dp))
        val left = goal - eaten
        Text(
            if (left >= 0) "zostało $left kcal" else "+${-left} kcal ponad cel",
            style = TextStyle(color = TextDark, fontSize = 12.sp)
        )
    }
}

@Composable
private fun WaterBlock(waterMl: Int, goalMl: Int, compact: Boolean = false) {
    Text(
        if (compact) "$waterMl / $goalMl ml" else "Woda: $waterMl / $goalMl ml",
        style = TextStyle(color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.Medium),
        maxLines = 1
    )
    Spacer(GlanceModifier.height(4.dp))
    LinearProgressIndicator(
        progress = if (goalMl > 0) (waterMl.toFloat() / goalMl).coerceIn(0f, 1f) else 0f,
        modifier = GlanceModifier.fillMaxWidth().height(8.dp),
        color = Water,
        backgroundColor = WaterLight
    )
    Spacer(GlanceModifier.height(8.dp))
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        WaterButton("+250", 250, GlanceModifier.defaultWeight())
        if (!compact) {
            Spacer(GlanceModifier.width(6.dp))
            WaterButton("+500", 500, GlanceModifier.defaultWeight())
        }
    }
}

@Composable
private fun WaterButton(label: String, amount: Int, modifier: GlanceModifier) {
    Box(
        modifier = modifier
            .background(WaterLight)
            .cornerRadius(16.dp)
            .padding(vertical = 8.dp)
            .clickable(actionRunCallback<AddWaterAction>(actionParametersOf(AddWaterAction.AmountKey to amount))),
        contentAlignment = Alignment.Center
    ) {
        Text("$label ml", style = TextStyle(color = Water, fontSize = 13.sp, fontWeight = FontWeight.Bold))
    }
}

/** Mały widżet: tylko woda i +250 ml. */
class WaterWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as DietApplication).container
        val today = LocalDate.now()
        provideContent {
            val water by container.waterRepository.observeWater(today).collectAsState(initial = null)
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(CardBg)
                    .cornerRadius(20.dp)
                    .padding(10.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WaterBlock(water?.amountMl ?: 0, water?.goalMl ?: WaterIntake.DEFAULT_GOAL_ML, compact = true)
            }
        }
    }
}

/** Kliknięcie „+250/+500 ml” na widżecie – dodaje wodę bez otwierania aplikacji. */
class AddWaterAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val container = (context.applicationContext as DietApplication).container
        val amount = parameters[AmountKey] ?: 250
        container.waterRepository.changeAmount(LocalDate.now(), amount)
        DietWidgetUpdater.requestUpdate(context)
    }

    companion object {
        val AmountKey = ActionParameters.Key<Int>("amount")
    }
}

class DietWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DietWidget()
}

class WaterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WaterWidget()
}

/** Kafelek w szybkich ustawieniach: +250 ml wody jednym dotknięciem. */
class WaterTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refresh()
    }

    override fun onClick() {
        super.onClick()
        val container = (applicationContext as DietApplication).container
        scope.launch {
            container.waterRepository.changeAmount(LocalDate.now(), 250)
            DietWidgetUpdater.requestUpdate(applicationContext)
            refresh()
        }
        Toast.makeText(this, "Dodano 250 ml wody", Toast.LENGTH_SHORT).show()
    }

    private fun refresh() {
        val container = (applicationContext as DietApplication).container
        scope.launch {
            val water = container.waterRepository.getWater(LocalDate.now())
            qsTile?.apply {
                label = "Woda +250 ml"
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    subtitle = "${water.amountMl} / ${water.goalMl} ml"
                }
                state = Tile.STATE_ACTIVE
                icon = Icon.createWithResource(this@WaterTileService, R.drawable.ic_water_tile)
                updateTile()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        fun requestRefresh(context: Context) {
            runCatching { requestListeningState(context, ComponentName(context, WaterTileService::class.java)) }
        }
    }
}

/** Odświeża widżety, gdy w aplikacji zmieni się woda, posiłki, waga, kroki lub profil. */
object DietWidgetUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start(context: Context, container: AppContainer, scope: CoroutineScope) {
        val app = context.applicationContext
        scope.launch {
            val today = LocalDate.now()
            combine(
                container.waterRepository.observeWater(today),
                container.diaryRepository.observeMeals(today),
                container.profileRepository.observeProfile(),
                container.weightRepository.observeLatest(),
                container.activityRepository.observeSteps(today)
            ) { _, _, _, _, _ -> Unit }.collect {
                updateNow(app)
            }
        }
    }

    fun requestUpdate(context: Context) {
        val app = context.applicationContext
        scope.launch { updateNow(app) }
    }

    private suspend fun updateNow(app: Context) {
        runCatching { DietWidget().updateAll(app) }
        runCatching { WaterWidget().updateAll(app) }
        WaterTileService.requestRefresh(app)
    }
}
