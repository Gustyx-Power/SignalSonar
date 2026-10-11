package id.gustyx.signalsonar.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.gustyx.signalsonar.data.model.SignalData

/**
 * Material 3 Expressive Hero Signal Visualizer.
 * Displays prominent real-time dBm readout, synchronized Material ripple waves,
 * and Material 3 telemetry chips.
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

    // Synchronized expanding ripple animations
    val rippleScale1 = remember { Animatable(0.4f) }
    val rippleAlpha1 = remember { Animatable(0.8f) }
    val rippleScale2 = remember { Animatable(0.3f) }
    val rippleAlpha2 = remember { Animatable(0.5f) }

    LaunchedEffect(pulseTriggerEpoch) {
        if (pulseTriggerEpoch > 0) {
            val animDuration = intervalMs.toInt().coerceIn(150, 1500)

            rippleScale1.snapTo(0.45f)
            rippleAlpha1.snapTo(0.85f)
            rippleScale1.animateTo(
                targetValue = 1.05f,
                animationSpec = tween(animDuration, easing = FastOutSlowInEasing)
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
        }
    }

    LaunchedEffect(pulseTriggerEpoch) {
        if (pulseTriggerEpoch > 0) {
            val animDuration = intervalMs.toInt().coerceIn(150, 1500)

            rippleScale2.snapTo(0.35f)
            rippleAlpha2.snapTo(0.55f)
            rippleScale2.animateTo(
                targetValue = 0.95f,
                animationSpec = tween((animDuration * 0.85).toInt(), easing = FastOutSlowInEasing)
            )
        }
    }

    LaunchedEffect(pulseTriggerEpoch) {
        if (pulseTriggerEpoch > 0) {
            val animDuration = intervalMs.toInt().coerceIn(150, 1500)

            rippleAlpha2.animateTo(
                targetValue = 0f,
                animationSpec = tween((animDuration * 0.85).toInt(), easing = LinearEasing)
            )
        }
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Signal Wave Centerpiece
            Box(
                modifier = Modifier.size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                // Material Ripple Animation Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val baseRadius = size.width / 2f - 16.dp.toPx()

                    // Background soft circular halo
                    drawCircle(
                        color = animatedQualityColor.copy(alpha = 0.08f),
                        radius = baseRadius,
                        center = center
                    )
                    drawCircle(
                        color = animatedQualityColor.copy(alpha = 0.22f),
                        radius = baseRadius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Secondary inner boundary
                    drawCircle(
                        color = animatedQualityColor.copy(alpha = 0.12f),
                        radius = baseRadius * 0.65f,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // Synchronized expanding ripples
                    if (rippleAlpha1.value > 0f) {
                        drawCircle(
                            color = animatedQualityColor.copy(alpha = rippleAlpha1.value * 0.4f),
                            radius = baseRadius * rippleScale1.value,
                            center = center,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }

                    if (rippleAlpha2.value > 0f) {
                        drawCircle(
                            color = animatedQualityColor.copy(alpha = rippleAlpha2.value * 0.3f),
                            radius = baseRadius * rippleScale2.value,
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                // Digital dBm Readout
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${signalData.dbm ?: "--"}",
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Bold,
                        color = animatedQualityColor
                    )
                    Text(
                        text = "dBm",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Material 3 Chips Info Bar
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = signalData.quality.label,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.SignalCellularAlt,
                            contentDescription = null,
                            tint = animatedQualityColor,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    border = null
                )

                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = signalData.networkType.displayName,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    border = null
                )

                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = "${intervalMs}ms",
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    border = null
                )
            }
        }
    }
}
