package id.gustyx.signalsonar.ui.components

import android.graphics.Paint
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.gustyx.signalsonar.data.model.SignalData

/**
 * Enhanced Hero Tactical Radar Visualizer.
 * Includes:
 * - Compass rose azimuth degrees (0° N, 90° E, 180° S, 270° W)
 * - Concentric radar range rings with distance tick marks
 * - Continuous 360° sweeping beam with neon trail
 * - Live expanding ripple waves synchronized with acoustic sonar beep intervals
 * - Center glassmorphic digital telemetry hub with live dBm, SINR, RSRQ, and quality status
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
        label = "radarQualityColor"
    )

    // Continuous Radar Sweep Animation
    val infiniteTransition = rememberInfiniteTransition(label = "radarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepAngle"
    )

    // Synchronized expanding ripple animations
    val rippleScale1 = remember { Animatable(0.35f) }
    val rippleAlpha1 = remember { Animatable(0.7f) }
    val rippleScale2 = remember { Animatable(0.2f) }
    val rippleAlpha2 = remember { Animatable(0.5f) }

    LaunchedEffect(pulseTriggerEpoch) {
        if (pulseTriggerEpoch > 0) {
            val animDuration = intervalMs.toInt().coerceIn(150, 1500)

            // Primary Ripple Wave
            rippleScale1.snapTo(0.35f)
            rippleAlpha1.snapTo(0.9f)
            rippleScale1.animateTo(
                targetValue = 1.05f,
                animationSpec = tween(animDuration, easing = FastOutSlowInEasing)
            )

            // Echo Wave
            rippleScale2.snapTo(0.25f)
            rippleAlpha2.snapTo(0.6f)
            rippleScale2.animateTo(
                targetValue = 0.95f,
                animationSpec = tween((animDuration * 0.9).toInt(), easing = FastOutSlowInEasing)
            )
        }
    }

    LaunchedEffect(pulseTriggerEpoch) {
        if (pulseTriggerEpoch > 0) {
            val animDuration = intervalMs.toInt().coerceIn(150, 1500)
            rippleAlpha1.animateTo(
                targetValue = 0f,
                animationSpec = tween(animDuration, easing = LinearEasing)
            )
            rippleAlpha2.animateTo(
                targetValue = 0f,
                animationSpec = tween((animDuration * 0.9).toInt(), easing = LinearEasing)
            )
        }
    }

    Box(
        modifier = modifier.size(330.dp),
        contentAlignment = Alignment.Center
    ) {
        // Tactical Radar Display Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.width / 2f - 18.dp.toPx()

            // Outer Azimuth Ring
            drawCircle(
                color = animatedQualityColor.copy(alpha = 0.35f),
                radius = maxRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Concentric Range Rings (25%, 50%, 75%, 100%)
            val ringFractions = floatArrayOf(0.32f, 0.54f, 0.77f, 1.0f)
            for (fraction in ringFractions) {
                drawCircle(
                    color = animatedQualityColor.copy(alpha = 0.12f),
                    radius = maxRadius * fraction,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // Radar Axis Crosshairs with Graduation Ticks
            val axisColor = animatedQualityColor.copy(alpha = 0.22f)
            drawLine(
                color = axisColor,
                start = Offset(center.x - maxRadius, center.y),
                end = Offset(center.x + maxRadius, center.y),
                strokeWidth = 1.2.dp.toPx()
            )
            drawLine(
                color = axisColor,
                start = Offset(center.x, center.y - maxRadius),
                end = Offset(center.x, center.y + maxRadius),
                strokeWidth = 1.2.dp.toPx()
            )

            // Distance ticks on the horizontal axis
            val tickCount = 6
            for (i in 1..tickCount) {
                val offset = (maxRadius / tickCount) * i
                // Right tick
                drawLine(
                    color = axisColor,
                    start = Offset(center.x + offset, center.y - 4.dp.toPx()),
                    end = Offset(center.x + offset, center.y + 4.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx()
                )
                // Left tick
                drawLine(
                    color = axisColor,
                    start = Offset(center.x - offset, center.y - 4.dp.toPx()),
                    end = Offset(center.x - offset, center.y + 4.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            // Animated Synchronized Ripple Waves
            if (rippleAlpha1.value > 0.01f) {
                drawCircle(
                    color = animatedQualityColor.copy(alpha = rippleAlpha1.value),
                    radius = maxRadius * rippleScale1.value,
                    center = center,
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
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
                    0.82f to Color.Transparent,
                    0.97f to animatedQualityColor.copy(alpha = 0.25f),
                    1.0f to animatedQualityColor.copy(alpha = 0.6f),
                    center = center
                )
                drawCircle(
                    brush = sweepBrush,
                    radius = maxRadius,
                    center = center
                )
                // Leading scanning edge beam
                drawLine(
                    color = animatedQualityColor.copy(alpha = 0.9f),
                    start = center,
                    end = Offset(center.x + maxRadius, center.y),
                    strokeWidth = 2.2.dp.toPx()
                )
            }

            // Compass Azimuth Markers (N, E, S, W)
            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    color = animatedQualityColor.copy(alpha = 0.85f).toArgb()
                    textSize = 10.dp.toPx()
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                }

                // N (North)
                canvas.nativeCanvas.drawText("0° N", center.x, center.y - maxRadius - 4.dp.toPx(), paint)
                // S (South)
                canvas.nativeCanvas.drawText("180° S", center.x, center.y + maxRadius + 12.dp.toPx(), paint)
                // E (East)
                paint.textAlign = Paint.Align.LEFT
                canvas.nativeCanvas.drawText("90° E", center.x + maxRadius + 4.dp.toPx(), center.y + 4.dp.toPx(), paint)
                // W (West)
                paint.textAlign = Paint.Align.RIGHT
                canvas.nativeCanvas.drawText("270° W", center.x - maxRadius - 4.dp.toPx(), center.y + 4.dp.toPx(), paint)
            }
        }

        // Center Hero Digital Readout Card
        Surface(
            modifier = Modifier
                .size(190.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = CircleShape,
                    spotColor = animatedQualityColor,
                    ambientColor = animatedQualityColor
                )
                .border(
                    width = 2.dp,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            animatedQualityColor.copy(alpha = 0.85f),
                            animatedQualityColor.copy(alpha = 0.25f)
                        )
                    ),
                    shape = CircleShape
                ),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // dBm Readout
                Text(
                    text = if (signalData.isSignalValid) "${signalData.dbm}" else "--",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = (-1.5).sp,
                    color = animatedQualityColor
                )

                Text(
                    text = "dBm (RSRP)",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Quality Badge Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(animatedQualityColor.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = quality.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = animatedQualityColor
                    )
                }

                // Live SINR / RSRQ mini footer
                if (signalData.sinr != null || signalData.rsrq != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SINR: ${signalData.formattedSinr}",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (signalData.rsrq != null) {
                            Text(
                                text = " • RSRQ: ${signalData.formattedRsrq}",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
