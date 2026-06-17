package com.remlinder.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.remlinder.app.ui.theme.GlowPurple
import com.remlinder.app.ui.theme.TimerAccent
import com.remlinder.app.ui.theme.TimerAccentDim
import com.remlinder.app.ui.theme.TimerTrack
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TimerWheel(
    modifier: Modifier = Modifier,
    size: Dp = 280.dp,
    progress: Float,
    onProgressChange: (Float) -> Unit,
    trackColor: Color = TimerTrack,
    activeColor: Color = TimerAccent,
    activeColorDim: Color = TimerAccentDim,
    strokeWidth: Dp = 12.dp,
    enabled: Boolean = true
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 400),
        label = "wheelProgress"
    )

    Canvas(
        modifier = modifier
            .size(size)
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val centerX = size.toPx() / 2f
                            val centerY = size.toPx() / 2f
                            val touchX = change.position.x
                            val touchY = change.position.y
                            val dx = touchX - centerX
                            val dy = touchY - centerY
                            var angle = atan2(dy, dx) + (PI / 2).toFloat()
                            if (angle < 0) angle += 2f * PI.toFloat()
                            val newProgress = (angle / (2f * PI.toFloat())).coerceIn(0f, 1f)
                            onProgressChange(newProgress)
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        val canvasSize = size.toPx()
        val center = Offset(canvasSize / 2f, canvasSize / 2f)
        val radius = (canvasSize - strokeWidth.toPx()) / 2f
        val stroke = strokeWidth.toPx()
        val sweepAngle = animatedProgress * 360f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    activeColor.copy(alpha = 0.05f),
                    Color.Transparent
                ),
                radius = radius + stroke
            ),
            radius = radius + stroke,
            center = center
        )

        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )

        val gradientBrush = Brush.sweepGradient(
            colors = listOf(
                activeColor,
                activeColorDim,
                GlowPurple,
                activeColor
            ),
            center = center
        )

        drawArc(
            brush = gradientBrush,
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )

        val glowWidth = stroke * 1.5f
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    activeColor.copy(alpha = 0.3f),
                    GlowPurple.copy(alpha = 0.1f),
                    activeColor.copy(alpha = 0.3f)
                ),
                center = center
            ),
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = glowWidth, cap = StrokeCap.Round)
        )

        if (sweepAngle > 1f) {
            val endAngle = (-90f + sweepAngle) * (PI.toFloat() / 180f)
            val dotX = center.x + radius * cos(endAngle)
            val dotY = center.y + radius * sin(endAngle)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.9f),
                        activeColor,
                        Color.Transparent
                    ),
                    radius = stroke * 1.8f
                ),
                radius = stroke * 1.5f,
                center = Offset(dotX, dotY)
            )
        }
    }
}
