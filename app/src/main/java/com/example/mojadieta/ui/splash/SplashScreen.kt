package com.example.mojadieta.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mojadieta.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SplashTop = Color(0xFF1F9D74)
private val SplashBottom = Color(0xFF0A5C42)
private val SplashAccent = Color(0xFFCDF7E5)

/**
 * Animowany ekran ładowania „Moja Dieta – by Tala”.
 * Kolor górnej części gradientu jest taki sam jak tło systemowego splasha,
 * dzięki czemu przejście jest płynne.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val currentOnFinished by rememberUpdatedState(onFinished)

    val logoScale = remember { Animatable(0.6f) }
    val logoAlpha = remember { Animatable(0f) }
    val titleProgress = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val footerAlpha = remember { Animatable(0f) }
    val quoteAlpha = remember { Animatable(0f) }
    val context = LocalContext.current
    val quote = rememberSaveable { MotivationalQuotes.next(context) }

    LaunchedEffect(Unit) {
        launch {
            logoScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
        }
        launch { logoAlpha.animateTo(1f, tween(400)) }
        delay(300)
        launch { titleProgress.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
        delay(350)
        launch { subtitleAlpha.animateTo(1f, tween(600)) }
        delay(250)
        launch { quoteAlpha.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
        delay(300)
        launch { footerAlpha.animateTo(1f, tween(500)) }
        delay(2600)
        currentOnFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SplashTop, SplashBottom)))
            // Dotknięcie ekranu pomija ekran ładowania.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { currentOnFinished() }
            )
    ) {
        // Dekoracyjne, rozmyte koła w tle.
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.07f),
                radius = size.minDimension * 0.55f,
                center = Offset(size.width * 0.95f, size.height * 0.12f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = size.minDimension * 0.45f,
                center = Offset(size.width * 0.05f, size.height * 0.88f)
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.06f),
                radius = size.minDimension * 0.25f,
                center = Offset(size.width * 0.15f, size.height * 0.3f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Spacer(Modifier.weight(1.1f))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
                    .size(168.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    // Grafika ma duży margines bezpieczeństwa (ikona adaptacyjna), więc ją powiększamy.
                    modifier = Modifier.requiredSize(250.dp)
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Moja Dieta",
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = titleProgress.value
                    translationY = (1f - titleProgress.value) * 40f
                }
            )

            Spacer(Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.alpha(subtitleAlpha.value)
            ) {
                Box(
                    Modifier
                        .width(32.dp)
                        .height(1.5.dp)
                        .background(SplashAccent.copy(alpha = 0.7f), RoundedCornerShape(1.dp))
                )
                Text(
                    text = "by Tala",
                    color = SplashAccent,
                    fontSize = 28.sp,
                    fontFamily = FontFamily.Cursive,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Box(
                    Modifier
                        .width(32.dp)
                        .height(1.5.dp)
                        .background(SplashAccent.copy(alpha = 0.7f), RoundedCornerShape(1.dp))
                )
            }
        }

        Spacer(Modifier.weight(1f))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 24.dp, bottom = 40.dp)
        ) {
            // Cytat motywujący – inny przy każdym uruchomieniu.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = 36.dp)
                    .graphicsLayer {
                        alpha = quoteAlpha.value
                        translationY = (1f - quoteAlpha.value) * 24f
                    }
            ) {
                Text(
                    text = "„$quote”",
                    color = Color.White,
                    fontSize = 17.sp,
                    lineHeight = 24.sp,
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily.Serif,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(36.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(footerAlpha.value)
            ) {
            LoadingDots()
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Zdrowe nawyki, dzień po dniu",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                letterSpacing = 0.3.sp
            )
            }
        }
        }
    }
}

/** Trzy pulsujące kropki jako wskaźnik ładowania. */
@Composable
private fun LoadingDots() {
    val transition = rememberInfiniteTransition(label = "dots")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(3) { index ->
            val scale by transition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 900, delayMillis = index * 180, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        alpha = 0.4f + 0.6f * scale
                    }
                    .background(Color.White, CircleShape)
            )
        }
    }
}
