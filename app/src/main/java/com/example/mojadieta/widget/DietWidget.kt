package com.example.mojadieta.widget

import android.content.Context
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
import com.example.mojadieta.di.AppContainer
import com.example.mojadieta.domain.model.MealEntry
import com.example.mojadieta.domain.model.UserProfile
import com.example.mojadieta.domain.model.WaterIntake
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

private val Mint = Color(0xFF1F9D74)
private val MintDark = Color(0xFF0A5C42)
private val Water = Color(0xFF2E9FD8)
private val WaterLight = Color(0xFFDDF0FA)
private val Card = Color(0xFFFFFFFF)
private val TextDark = Color(0xFF17201C)
private val TextMuted = Color(0xFF55635D)

/** Widżet „Moja Dieta”: kalorie dnia i licznik wody z szybkim dodawaniem. */
class DietWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as DietApplication).container
        val today = LocalDate.now()
        provideContent {
            val water by container.waterRepository.observeWater(today).collectAsState(initial = null)
            val meals by container.diaryRepository.observeMeals(today).collectAsState(initial = emptyList())
            val profile by container.profileRepository.observeProfile().collectAsState(initial = null)
            WidgetContent(container, water, meals, profile)
        }
    }

    companion object {
        private val SMALL = DpSize(110.dp, 110.dp)
        private val WIDE = DpSize(250.dp, 110.dp)
    }
}

@Composable
private fun WidgetContent(
    container: AppContainer,
    water: WaterIntake?,
    meals: List<MealEntry>,
    profile: UserProfile?
) {
    val wide = LocalSize.current.width >= 250.dp
    val eaten = meals.filter { it.isEaten }.sumOf { it.calories }
    val goalKcal = profile?.let { container.calculateDailyGoal(it).goal.calories }
    val waterMl = water?.amountMl ?: 0
    val waterGoal = water?.goalMl ?: WaterIntake.DEFAULT_GOAL_ML

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Card))
            .cornerRadius(24.dp)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Text(
            "Moja Dieta",
            style = TextStyle(color = ColorProvider(MintDark), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(GlanceModifier.height(6.dp))
        if (wide) {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    CaloriesBlock(eaten, goalKcal)
                }
                Spacer(GlanceModifier.width(12.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    WaterBlock(waterMl, waterGoal)
                }
            }
        } else {
            WaterBlock(waterMl, waterGoal)
            Spacer(GlanceModifier.height(4.dp))
            Text(
                if (goalKcal != null) "$eaten / $goalKcal kcal" else "$eaten kcal",
                style = TextStyle(color = ColorProvider(TextMuted), fontSize = 11.sp)
            )
        }
    }
}

@Composable
private fun CaloriesBlock(eaten: Int, goal: Int?) {
    Text(
        "$eaten",
        style = TextStyle(color = ColorProvider(Mint), fontSize = 24.sp, fontWeight = FontWeight.Bold)
    )
    Text(
        if (goal != null) "z $goal kcal" else "kcal zjedzone",
        style = TextStyle(color = ColorProvider(TextMuted), fontSize = 12.sp)
    )
    Spacer(GlanceModifier.height(4.dp))
    LinearProgressIndicator(
        progress = if (goal != null && goal > 0) (eaten.toFloat() / goal).coerceIn(0f, 1f) else 0f,
        modifier = GlanceModifier.fillMaxWidth().height(6.dp),
        color = ColorProvider(Mint),
        backgroundColor = ColorProvider(Color(0xFFCDF3E3))
    )
    if (goal != null) {
        Spacer(GlanceModifier.height(4.dp))
        val left = goal - eaten
        Text(
            if (left >= 0) "zostało $left kcal" else "+${-left} kcal ponad cel",
            style = TextStyle(color = ColorProvider(TextDark), fontSize = 12.sp)
        )
    }
}

@Composable
private fun WaterBlock(waterMl: Int, goalMl: Int) {
    Text(
        "Woda: $waterMl / $goalMl ml",
        style = TextStyle(color = ColorProvider(TextDark), fontSize = 13.sp, fontWeight = FontWeight.Medium)
    )
    Spacer(GlanceModifier.height(4.dp))
    LinearProgressIndicator(
        progress = if (goalMl > 0) (waterMl.toFloat() / goalMl).coerceIn(0f, 1f) else 0f,
        modifier = GlanceModifier.fillMaxWidth().height(8.dp),
        color = ColorProvider(Water),
        backgroundColor = ColorProvider(WaterLight)
    )
    Spacer(GlanceModifier.height(8.dp))
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        WaterButton("+250", 250, GlanceModifier.defaultWeight())
        Spacer(GlanceModifier.width(6.dp))
        WaterButton("+500", 500, GlanceModifier.defaultWeight())
    }
}

@Composable
private fun WaterButton(label: String, amount: Int, modifier: GlanceModifier) {
    Box(
        modifier = modifier
            .background(ColorProvider(WaterLight))
            .cornerRadius(16.dp)
            .padding(vertical = 8.dp)
            .clickable(actionRunCallback<AddWaterAction>(actionParametersOf(AddWaterAction.AmountKey to amount))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "$label ml",
            style = TextStyle(color = ColorProvider(Water), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        )
    }
}

/** Kliknięcie „+250/+500 ml” na widżecie – dodaje wodę bez otwierania aplikacji. */
class AddWaterAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val container = (context.applicationContext as DietApplication).container
        val amount = parameters[AmountKey] ?: 250
        container.waterRepository.changeAmount(LocalDate.now(), amount)
        DietWidget().update(context, glanceId)
    }

    companion object {
        val AmountKey = ActionParameters.Key<Int>("amount")
    }
}

class DietWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DietWidget()
}

/** Odświeża widżet, gdy w aplikacji zmieni się woda, posiłki lub profil. */
object DietWidgetUpdater {
    fun start(context: Context, container: AppContainer, scope: CoroutineScope) {
        val app = context.applicationContext
        scope.launch {
            val today = LocalDate.now()
            combine(
                container.waterRepository.observeWater(today),
                container.diaryRepository.observeMeals(today),
                container.profileRepository.observeProfile()
            ) { _, _, _ -> Unit }.collect {
                runCatching { DietWidget().updateAll(app) }
            }
        }
    }
}
