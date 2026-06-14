package com.remlinder.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.remlinder.app.ui.theme.TimerAccent
import com.remlinder.app.ui.theme.TimerAccentDim
import com.remlinder.app.ui.theme.TimerTrack
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

    var dragAngle by remember { mutableFloatStateOf(0f) }

    Canvas(
        modifier = modifier
            .size(size)
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
                                dragAngle = calculateAngle(center, offset)
                            },
                            onDrag = { change, _ ->
                                val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
                                val newAngle = calculateAngle(center, change.position)
                                dragAngle = newAngle
                                val clamped = ((newAngle / 360f) % 1f).coerceIn(0f, 1f)
                                onProgressChange(clamped)
                            }
                        )
                    }
                } else {
                    Modifier
                }
            )
    ) {
        val stroke = strokeWidth.toPx()
        val radius = (size.toPx() - stroke) / 2f
        val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
        val topLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)

        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )

        val sweepAngle = animatedProgress * 360f

        val activeBrush = Brush.sweepGradient(
            colors = listOf(activeColorDim, activeColor, activeColor),
            center = center
        )

        drawArc(
            brush = activeBrush,
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )

        val thumbAngle = (animatedProgress * 360f) - 90f
        val thumbRad = Math.toRadians(thumbAngle.toDouble())
        val thumbX = center.x + radius * cos(thumbRad).toFloat()
        val thumbY = center.y + radius * sin(thumbRad).toFloat()

        drawCircle(
            color = activeColor,
            radius = stroke * 1.8f,
            center = Offset(thumbX, thumbY)
        )

        drawCircle(
            color = Color.White.copy(alpha = 0.3f),
            radius = stroke * 2.6f,
            center = Offset(thumbX, thumbY)
        )
    }
}

private fun calculateAngle(center: Offset, point: Offset): Float {
    val angle = Math.toDegrees(
        atan2(
            (point.y - center.y).toDouble(),
            (point.x - center.x).toDouble()
        )
    ).toFloat()
    val normalized = (angle + 360f) % 360f
    return (normalized + 90f) % 360f
}
