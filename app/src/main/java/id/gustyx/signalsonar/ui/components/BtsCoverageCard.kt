package id.gustyx.signalsonar.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.gustyx.signalsonar.data.model.CellTowerInfo

/**
 * Material 3 BTS Coverage Radius and Distance Analyzer Card.
 * Visualizes the coverage zone of the connected cell tower, current distance,
 * and smartphone position within the tower's theoretical cell radius with
 * animated RF wave propagation ripples, live packet transmission, and pulse locator.
 */
@Composable
fun BtsCoverageCard(
    cellTowerInfo: CellTowerInfo,
    modifier: Modifier = Modifier,
    onTestBeacon: (() -> Unit)? = null
) {
    val zoneColor by animateColorAsState(
        targetValue = cellTowerInfo.coverageZone.color,
        animationSpec = tween(400),
        label = "coverageZoneColor"
    )

    // Dynamic animations for the BTS coverage radar
    val infiniteTransition = rememberInfiniteTransition(label = "btsRadarAnimation")

    // 1. Staggered concentric RF wavefront wave ripples emanating from the BTS tower
    val wave1Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "btsWave1"
    )
    val wave2Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing, delayMillis = 850),
            repeatMode = RepeatMode.Restart
        ),
        label = "btsWave2"
    )
    val wave3Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing, delayMillis = 1700),
            repeatMode = RepeatMode.Restart
        ),
        label = "btsWave3"
    )

    // 2. Tower core breathing aura pulse
    val towerPulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "towerPulse"
    )

    // 3. Smartphone beacon locator ripple
    val phoneBeaconScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 2.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phoneBeacon"
    )

    // 4. Traveling photon / live RF packet along transmission beam
    val photonProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "photonProgress"
    )

    // 5. Smooth animated smartphone distance fraction
    val animatedHpDistanceFraction by animateFloatAsState(
        targetValue = cellTowerInfo.coveragePercentage.coerceIn(0.08f, 0.95f),
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "hpDistanceAnimation"
    )

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Radar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Jangkauan Menara BTS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Radius cakupan seluler & posisi HP",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onTestBeacon != null) {
                        IconButton(
                            onClick = onTestBeacon,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = "Uji Suara Beacon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = cellTowerInfo.coverageZone.shortBadge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = zoneColor.copy(alpha = 0.15f),
                            labelColor = zoneColor
                        ),
                        border = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Animated Visual Coverage Diagram
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(155.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(270.dp, 145.dp)) {
                    val w = size.width
                    val h = size.height
                    val towerX = 40.dp.toPx()
                    val centerY = h / 2f
                    val maxRadiusPx = w - 75.dp.toPx()

                    // Static guide boundary rings
                    // 1. Full coverage boundary ring (100%)
                    drawCircle(
                        color = Color.Gray.copy(alpha = 0.18f),
                        radius = maxRadiusPx,
                        center = Offset(towerX, centerY),
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // 2. Mid-cell boundary (70%)
                    drawCircle(
                        color = Color(0xFF00B0FF).copy(alpha = 0.20f),
                        radius = maxRadiusPx * 0.70f,
                        center = Offset(towerX, centerY),
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // 3. Near-tower boundary (25%)
                    drawCircle(
                        color = Color(0xFF00C853).copy(alpha = 0.25f),
                        radius = maxRadiusPx * 0.25f,
                        center = Offset(towerX, centerY),
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // Staggered animated concentric RF wave ripples propagating from the BTS tower
                    val waveProgressList = listOf(wave1Progress, wave2Progress, wave3Progress)
                    waveProgressList.forEach { p ->
                        val waveRadius = p * maxRadiusPx
                        val waveAlpha = ((1f - p) * 0.38f).coerceIn(0f, 0.38f)
                        drawCircle(
                            color = zoneColor.copy(alpha = waveAlpha),
                            radius = waveRadius,
                            center = Offset(towerX, centerY),
                            style = Stroke(width = 1.8.dp.toPx())
                        )
                    }

                    // Position of Smartphone along the radius (smoothly animated)
                    val hpX = towerX + (maxRadiusPx * animatedHpDistanceFraction)

                    // Connecting RF Transmission Baseline (Gradient)
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF00C853).copy(alpha = 0.8f), zoneColor.copy(alpha = 0.8f)),
                            startX = towerX,
                            endX = hpX
                        ),
                        start = Offset(towerX, centerY),
                        end = Offset(hpX, centerY),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Live traveling packet / photon along the RF transmission beam
                    val photonX = towerX + (hpX - towerX) * photonProgress
                    drawCircle(
                        color = Color.White.copy(alpha = 0.5f),
                        radius = 4.5.dp.toPx(),
                        center = Offset(photonX, centerY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = Offset(photonX, centerY)
                    )

                    // BTS Tower marker: Breathing aura glow + solid center
                    drawCircle(
                        color = Color(0xFF00C853).copy(alpha = (0.35f / towerPulseScale).coerceIn(0.08f, 0.35f)),
                        radius = 8.dp.toPx() * towerPulseScale,
                        center = Offset(towerX, centerY)
                    )
                    drawCircle(
                        color = Color(0xFF00C853),
                        radius = 5.5.dp.toPx(),
                        center = Offset(towerX, centerY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.2.dp.toPx(),
                        center = Offset(towerX, centerY)
                    )

                    // Smartphone indicator pin: Animated beacon locator pulse ring + halo
                    val beaconAlpha = ((1f - (phoneBeaconScale - 1f) / 1.1f) * 0.45f).coerceIn(0f, 0.45f)
                    drawCircle(
                        color = zoneColor.copy(alpha = beaconAlpha),
                        radius = 11.dp.toPx() * phoneBeaconScale,
                        center = Offset(hpX, centerY),
                        style = Stroke(width = 1.8.dp.toPx())
                    )
                    drawCircle(
                        color = zoneColor.copy(alpha = 0.28f),
                        radius = 12.dp.toPx(),
                        center = Offset(hpX, centerY)
                    )
                    drawCircle(
                        color = zoneColor,
                        radius = 6.5.dp.toPx(),
                        center = Offset(hpX, centerY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = Offset(hpX, centerY)
                    )
                }

                // Legend labels on diagram
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🗼 Menara BTS (0m)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "📱 HP Anda (${cellTowerInfo.formattedDistance})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = zoneColor
                    )
                    Text(
                        text = "Batas: ${cellTowerInfo.formattedMaxCoverage}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Percentage Linear Progress with smooth animated value
            LinearProgressIndicator(
                progress = { animatedHpDistanceFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = zoneColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Distance & Status Detail Tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Jarak ke Menara",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = cellTowerInfo.formattedDistance,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (cellTowerInfo.timingAdvance != null) "TA: ${cellTowerInfo.timingAdvance}" else "Path Loss Model",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Maks Jangkauan Band",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = cellTowerInfo.formattedMaxCoverage,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${(cellTowerInfo.coveragePercentage * 100).toInt()}% dari radius sel",
                            style = MaterialTheme.typography.bodySmall,
                            color = zoneColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Explanation & Calculation Method Footer
            Text(
                text = cellTowerInfo.coverageZone.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Metode Pengukuran:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = cellTowerInfo.distanceCalculationMethod,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
