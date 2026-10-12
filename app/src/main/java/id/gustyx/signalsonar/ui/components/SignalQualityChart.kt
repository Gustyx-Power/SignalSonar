package id.gustyx.signalsonar.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.gustyx.signalsonar.data.model.SignalQuality

/**
 * Material 3 Real-time Signal Strength (dBm) History and Trend Graph.
 * Visualizes a rolling buffer of 30 recent signal measurements with dynamic gradient area fills.
 */
@Composable
fun SignalQualityChart(
    historyDbm: List<Int>,
    currentDbm: Int?,
    modifier: Modifier = Modifier
) {
    val quality = SignalQuality.fromDbm(currentDbm)
    val animatedQualityColor by animateColorAsState(
        targetValue = quality.color,
        animationSpec = tween(350),
        label = "chartQualityColor"
    )

    // Calculate trend from the last few samples
    val trend = when {
        historyDbm.size < 4 -> Triple("Stabil", Icons.AutoMirrored.Rounded.TrendingFlat, Color(0xFF00B0FF))
        else -> {
            val recent = historyDbm.takeLast(3).average()
            val older = historyDbm.dropLast(3).takeLast(3).average()
            val diff = recent - older
            when {
                diff >= 2.5 -> Triple("Meningkat (+${diff.toInt()} dB)", Icons.AutoMirrored.Rounded.TrendingUp, Color(0xFF00C853))
                diff <= -2.5 -> Triple("Menurun (${diff.toInt()} dB)", Icons.AutoMirrored.Rounded.TrendingDown, Color(0xFFFF5252))
                else -> Triple("Stabil", Icons.AutoMirrored.Rounded.TrendingFlat, Color(0xFF00B0FF))
            }
        }
    }

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
                        imageVector = Icons.AutoMirrored.Rounded.ShowChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Grafik Tren Sinyal Real-Time",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Riwayat fluktuasi saat bergerak (${historyDbm.size} sampel)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = trend.first,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = trend.second,
                            contentDescription = null,
                            tint = trend.third,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = trend.third.copy(alpha = 0.15f),
                        labelColor = trend.third
                    ),
                    border = null
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Line Chart
            val chartHeight = 110.dp
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
            ) {
                val w = size.width
                val h = size.height

                // Baseline scale: -125 dBm (bottom) to -65 dBm (top)
                val minScale = -125f
                val maxScale = -65f
                val range = maxScale - minScale

                // Draw background horizontal guide grid lines
                val guideLines = listOf(-110f, -90f, -75f)
                guideLines.forEach { dbmGuide ->
                    val y = h - ((dbmGuide - minScale) / range * h)
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.15f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                if (historyDbm.size >= 2) {
                    val stepX = w / (historyDbm.size - 1)
                    val points = historyDbm.mapIndexed { index, dbmVal ->
                        val clamped = dbmVal.toFloat().coerceIn(minScale, maxScale)
                        val y = h - ((clamped - minScale) / range * h)
                        Offset(index * stepX, y)
                    }

                    // Build area fill path
                    val areaPath = Path().apply {
                        moveTo(points.first().x, h)
                        points.forEach { lineTo(it.x, it.y) }
                        lineTo(points.last().x, h)
                        close()
                    }

                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                animatedQualityColor.copy(alpha = 0.35f),
                                animatedQualityColor.copy(alpha = 0.02f)
                            )
                        )
                    )

                    // Build stroke line path
                    val strokePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }

                    drawPath(
                        path = strokePath,
                        color = animatedQualityColor,
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Highlight latest current sample point
                    val lastPoint = points.last()
                    drawCircle(
                        color = animatedQualityColor.copy(alpha = 0.3f),
                        radius = 8.dp.toPx(),
                        center = lastPoint
                    )
                    drawCircle(
                        color = animatedQualityColor,
                        radius = 4.5.dp.toPx(),
                        center = lastPoint
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stats Footer
            if (historyDbm.isNotEmpty()) {
                val minDbm = historyDbm.minOrNull() ?: 0
                val maxDbm = historyDbm.maxOrNull() ?: 0
                val avgDbm = historyDbm.average().toInt()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = "Terendah", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "$minDbm dBm", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = "Rata-Rata", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "$avgDbm dBm", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = "Tertinggi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "$maxDbm dBm", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFF00C853))
                        }
                    }
                }
            }
        }
    }
}
