package com.remlinder.app.ui.components

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.remlinder.app.ui.theme.WormAccent
import com.remlinder.app.ui.theme.WormGradientEnd
import com.remlinder.app.ui.theme.WormGradientStart
import kotlin.math.sin

@Composable
fun WigglyWormAnimation(
    modifier: Modifier = Modifier,
    wormColorStart: Color = WormGradientStart,
    wormColorEnd: Color = WormGradientEnd,
    accentColor: Color = WormAccent,
    speedMultiplier: Float = 1f,
    wormCount: Int = 3
) {
    val infiniteTransition = rememberInfiniteTransition(label = "worm")

    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * kotlin.math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (3000 / speedMultiplier).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * kotlin.math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (2200 / speedMultiplier).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (1500 / speedMultiplier).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val wormHeight = canvasHeight * 0.15f
        val centerY = canvasHeight / 2f
        val amplitude = wormHeight * pulse

        for (wormIndex in 0 until wormCount) {
            val alpha = 1f - (wormIndex * 0.15f)
            val verticalOffset = (wormIndex - wormCount / 2) * wormHeight * 0.6f
            val strokeWidth = (wormHeight * 0.25f) * (1f - wormIndex * 0.1f)
            val phaseOffset = wormIndex * 0.8f
            val speedPhase = if (wormIndex % 2 == 0) phase1 else phase2

            val path = Path().apply {
                val baseY = centerY + verticalOffset
                moveTo(0f, baseY)

                for (x in 0..canvasWidth.toInt() step 4) {
                    val normalizedX = x / canvasWidth
                    val wave = sin(normalizedX * 4f * kotlin.math.PI.toFloat() + speedPhase + phaseOffset)
                    val secondaryWave = sin(normalizedX * 6f * kotlin.math.PI.toFloat() + speedPhase * 1.3f + phaseOffset) * 0.3f
                    val y = baseY + (wave + secondaryWave) * amplitude

                    if (x == 0) {
                        moveTo(0f, y)
                    } else {
                        lineTo(x.toFloat(), y)
                    }
                }
            }

            val gradientStart = Offset(0f, 0f)
            val gradientEnd = Offset(canvasWidth, 0f)
            val gradientBrush = Brush.linearGradient(
                colors = listOf(
                    wormColorStart.copy(alpha = alpha),
                    accentColor.copy(alpha = alpha),
                    wormColorEnd.copy(alpha = alpha),
                    wormColorStart.copy(alpha = alpha)
                ),
                start = gradientStart,
                end = gradientEnd
            )

            drawPath(
                path = path,
                brush = gradientBrush,
                style = Stroke(
                    width = strokeWidth.coerceAtLeast(1f),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            val glowWidth = strokeWidth * 2.5f
            val glowBrush = Brush.linearGradient(
                colors = listOf(
                    wormColorStart.copy(alpha = alpha * 0.15f),
                    accentColor.copy(alpha = alpha * 0.15f),
                    wormColorEnd.copy(alpha = alpha * 0.15f),
                    wormColorStart.copy(alpha = alpha * 0.15f)
                ),
                start = gradientStart,
                end = gradientEnd
            )
            drawPath(
                path = path,
                brush = glowBrush,
                style = Stroke(
                    width = glowWidth.coerceAtLeast(1f),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}
