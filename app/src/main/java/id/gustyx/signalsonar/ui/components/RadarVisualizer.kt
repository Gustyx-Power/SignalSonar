package id.gustyx.signalsonar.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.gustyx.signalsonar.data.model.SignalData

/**
 * Hero Visualizer Component.
 * Features:
 * - Large, expressive dBm readout.
 * - Material You styled concentric radar range rings.
 * - Smooth sweeping radar scanner beam.
 * - Multi-stage expanding ripple waves synchronized dynamically with acoustic sonar beep intervals.
 */
@Composable
fun RadarVisualizer(
    signalData: SignalData,
    pulseTriggerEpoch: Long,
    intervalMs: Long,
    modifier: Modifier = Modifier
) {
    val quality = signalData.quality
    val animatedQualityColor by animateColorAsState(
        targetValue = quality.color,
        animationSpec = tween(durationMillis = 350),
        label = "qualityColor"
    )

    // Sweep rotation transition
    val infiniteTransition = rememberInfiniteTransition(label = "radarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepAngle"
    )

    // Synchronized expanding ripple animations
    val rippleScale1 = remember { Animatable(0.3f) }
    val rippleAlpha1 = remember { Animatable(0.7f) }
    val rippleScale2 = remember { Animatable(0.3f) }
    val rippleAlpha2 = remember { Animatable(0.5f) }

    LaunchedEffect(pulseTriggerEpoch) {
        if (pulseTriggerEpoch > 0) {
            val animDuration = intervalMs.toInt().coerceIn(200, 1500)

            // Trigger Primary Wave
            rippleScale1.snapTo(0.35f)
            rippleAlpha1.snapTo(0.85f)
            rippleScale1.animateTo(
                targetValue = 1.05f,
                animationSpec = tween(animDuration, easing = FastOutSlowInEasing)
            )

            // Trigger Echo Wave
            rippleScale2.snapTo(0.2f)
            rippleAlpha2.snapTo(0.55f)
            rippleScale2.animateTo(
                targetValue = 0.9f,
                animationSpec = tween((animDuration * 0.9).toInt(), easing = FastOutSlowInEasing)
            )
        }
    }

    LaunchedEffect(pulseTriggerEpoch) {
        if (pulseTriggerEpoch > 0) {
            val animDuration = intervalMs.toInt().coerceIn(200, 1500)
            rippleAlpha1.animateTo(
                targetValue = 0f,
                animationSpec = tween(animDuration, easing = LinearEasing)
            )
        }
    }

    Box(
        modifier = modifier.size(310.dp),
        contentAlignment = Alignment.Center
    ) {
        // Radar Background & Sweep Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.width / 2f - 12.dp.toPx()

            // Concentric Range Rings
            val ringFractions = floatArrayOf(0.35f, 0.6f, 0.85f, 1.0f)
            for (fraction in ringFractions) {
                drawCircle(
                    color = animatedQualityColor.copy(alpha = 0.12f),
                    radius = maxRadius * fraction,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // Radar Axis Crosshairs
            val axisColor = animatedQualityColor.copy(alpha = 0.16f)
            drawLine(
                color = axisColor,
                start = Offset(center.x - maxRadius, center.y),
                end = Offset(center.x + maxRadius, center.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = axisColor,
                start = Offset(center.x, center.y - maxRadius),
                end = Offset(center.x, center.y + maxRadius),
                strokeWidth = 1.dp.toPx()
            )

            // Animated Synchronized Ripple Waves
            if (rippleAlpha1.value > 0.01f) {
                drawCircle(
                    color = animatedQualityColor.copy(alpha = rippleAlpha1.value),
                    radius = maxRadius * rippleScale1.value,
                    center = center,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            if (rippleAlpha2.value > 0.01f) {
                drawCircle(
                    color = animatedQualityColor.copy(alpha = rippleAlpha2.value * 0.7f),
                    radius = maxRadius * rippleScale2.value,
                    center = center,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Rotating Radar Scanner Beam
            rotate(degrees = sweepAngle, pivot = center) {
                val sweepBrush = Brush.sweepGradient(
                    0.0f to Color.Transparent,
                    0.85f to Color.Transparent,
                    0.99f to animatedQualityColor.copy(alpha = 0.22f),
                    1.0f to animatedQualityColor.copy(alpha = 0.5f),
                    center = center
                )
                drawCircle(
                    brush = sweepBrush,
                    radius = maxRadius,
                    center = center
                )
                // Leading scanning edge line
                drawLine(
                    color = animatedQualityColor.copy(alpha = 0.85f),
                    start = center,
                    end = Offset(center.x + maxRadius, center.y),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        // Center Hero Digital Readout Card
        Surface(
            modifier = Modifier
                .size(175.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    spotColor = animatedQualityColor,
                    ambientColor = animatedQualityColor
                )
                .border(
                    width = 2.dp,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            animatedQualityColor.copy(alpha = 0.8f),
                            animatedQualityColor.copy(alpha = 0.2f)
                        )
                    ),
                    shape = CircleShape
                ),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // dBm Readout
                Text(
                    text = if (signalData.isSignalValid) "${signalData.dbm}" else "--",
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = (-1).sp,
                    color = animatedQualityColor
                )

                Text(
                    text = "dBm",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Quality Badge Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(animatedQualityColor.copy(alpha = 0.16f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = quality.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = animatedQualityColor
                    )
                }
            }
        }
    }
}
