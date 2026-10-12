package id.gustyx.signalsonar.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import id.gustyx.signalsonar.data.model.SignalData
import id.gustyx.signalsonar.data.model.SignalQuality
import id.gustyx.signalsonar.data.model.SignalSnapshot
import kotlin.math.roundToInt

/**
 * Available RF metrics to visualize on the real-time trend chart.
 */
enum class ChartMetricType(
    val title: String,
    val unit: String,
    val description: String
) {
    COMPOSITE(
        title = "Skor Gabungan",
        unit = "%",
        description = "Dihitung dari RSRP (35%) + RSRQ (35%) + SINR (30%)"
    ),
    RSRP(
        title = "Daya (RSRP)",
        unit = "dBm",
        description = "Kekuatan fisik gelombang radio yang diterima antena HP"
    ),
    RSRQ(
        title = "Kualitas (RSRQ)",
        unit = "dB",
        description = "Mengukur interferensi dan kepadatan beban menara BTS"
    ),
    SINR(
        title = "Kebersihan (SINR)",
        unit = "dB",
        description = "Rasio kemurnian sinyal terhadap gangguan derau (noise)"
    )
}

/**
 * Material 3 Real-time Multi-Factor Signal Quality and RF Metric Chart.
 * Visualizes a rolling buffer of 30 measurements influenced by RSRP, RSRQ, and SINR.
 */
@Composable
fun SignalQualityChart(
    history: List<SignalSnapshot>,
    currentSignal: SignalData,
    modifier: Modifier = Modifier
) {
    var selectedMetric by remember { mutableStateOf(ChartMetricType.COMPOSITE) }

    val hasRsrqData = history.any { it.rsrq != null } || currentSignal.rsrq != null
    val hasSinrData = history.any { it.sinr != null } || currentSignal.sinr != null

    // Extract relevant data points for the selected metric
    val extractedValues: List<Float> = when (selectedMetric) {
        ChartMetricType.COMPOSITE -> history.map { it.compositeScore.toFloat() }
        ChartMetricType.RSRP -> history.mapNotNull { (it.rsrp ?: it.dbm)?.toFloat() }
        ChartMetricType.RSRQ -> history.mapNotNull { it.rsrq?.toFloat() }
        ChartMetricType.SINR -> history.mapNotNull { it.sinr?.toFloat() }
    }

    // Determine min/max scale and guidelines based on selected metric
    val (minScale, maxScale, guideLines) = when (selectedMetric) {
        ChartMetricType.COMPOSITE -> Triple(0f, 100f, listOf(25f, 50f, 75f))
        ChartMetricType.RSRP -> Triple(-125f, -65f, listOf(-110f, -90f, -75f))
        ChartMetricType.RSRQ -> Triple(-20f, -3f, listOf(-16f, -12f, -8f))
        ChartMetricType.SINR -> Triple(-10f, 30f, listOf(0f, 10f, 20f))
    }

    // Theme color based on active metric and current value
    val activeColor: Color = when (selectedMetric) {
        ChartMetricType.COMPOSITE -> {
            val score = currentSignal.compositeScore
            when {
                score >= 75 -> Color(0xFF00E676)
                score >= 50 -> Color(0xFF00E5FF)
                score >= 30 -> Color(0xFFFFB300)
                else -> Color(0xFFFF5252)
            }
        }
        ChartMetricType.RSRP -> currentSignal.quality.color
        ChartMetricType.RSRQ -> {
            val rsrq = currentSignal.rsrq ?: -15
            when {
                rsrq >= -9 -> Color(0xFF00E676)
                rsrq in -13..-10 -> Color(0xFF00E5FF)
                rsrq in -17..-14 -> Color(0xFFFFB300)
                else -> Color(0xFFFF5252)
            }
        }
        ChartMetricType.SINR -> {
            val sinr = currentSignal.sinr ?: 5
            when {
                sinr >= 13 -> Color(0xFF00E676)
                sinr in 6..12 -> Color(0xFF00E5FF)
                sinr in 0..5 -> Color(0xFFFFB300)
                else -> Color(0xFFFF5252)
            }
        }
    }

    val animatedColor by animateColorAsState(
        targetValue = activeColor,
        animationSpec = tween(350),
        label = "chartMetricColor"
    )

    // Calculate trend from recent samples
    val trend = when {
        extractedValues.size < 4 -> Triple("Stabil", Icons.AutoMirrored.Rounded.TrendingFlat, Color(0xFF00B0FF))
        else -> {
            val recent = extractedValues.takeLast(3).average()
            val older = extractedValues.dropLast(3).takeLast(3).average()
            val diff = (recent - older).toFloat()
            val threshold = when (selectedMetric) {
                ChartMetricType.COMPOSITE -> 3.0f
                ChartMetricType.RSRP -> 2.5f
                ChartMetricType.RSRQ -> 1.5f
                ChartMetricType.SINR -> 2.0f
            }
            when {
                diff >= threshold -> {
                    val formatted = if (selectedMetric == ChartMetricType.COMPOSITE) "+${diff.roundToInt()}%" else "+${diff.roundToInt()} dB"
                    Triple("Meningkat ($formatted)", Icons.AutoMirrored.Rounded.TrendingUp, Color(0xFF00C853))
                }
                diff <= -threshold -> {
                    val formatted = if (selectedMetric == ChartMetricType.COMPOSITE) "${diff.roundToInt()}%" else "${diff.roundToInt()} dB"
                    Triple("Menurun ($formatted)", Icons.AutoMirrored.Rounded.TrendingDown, Color(0xFFFF5252))
                }
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
                            text = "Grafik Kualitas Sinyal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${selectedMetric.title} (${extractedValues.size} sampel)",
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

            Spacer(modifier = Modifier.height(12.dp))

            // Metric Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChartMetricType.values().forEach { metric ->
                    val isEnabled = when (metric) {
                        ChartMetricType.COMPOSITE, ChartMetricType.RSRP -> true
                        ChartMetricType.RSRQ -> hasRsrqData
                        ChartMetricType.SINR -> hasSinrData
                    }
                    val isSelected = selectedMetric == metric

                    FilterChip(
                        selected = isSelected,
                        onClick = { if (isEnabled) selectedMetric = metric },
                        label = {
                            Text(
                                text = if (!isEnabled) "${metric.title} (N/A)" else metric.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        enabled = isEnabled,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            // Description of active formula / metric
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = selectedMetric.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Canvas Chart
            val chartHeight = 110.dp
            if (extractedValues.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Data ${selectedMetric.title} tidak dilaporkan oleh modem",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight)
                ) {
                    val w = size.width
                    val h = size.height
                    val range = maxScale - minScale

                    // Draw background horizontal guide grid lines
                    guideLines.forEach { guideVal ->
                        val y = h - ((guideVal - minScale) / range * h)
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.15f),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    if (extractedValues.size >= 2) {
                        val stepX = w / (extractedValues.size - 1)
                        val points = extractedValues.mapIndexed { index, value ->
                            val clamped = value.coerceIn(minScale, maxScale)
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
                                    animatedColor.copy(alpha = 0.35f),
                                    animatedColor.copy(alpha = 0.02f)
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
                            color = animatedColor,
                            style = Stroke(
                                width = 2.5.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // Highlight latest current sample point
                        val lastPoint = points.last()
                        drawCircle(
                            color = animatedColor.copy(alpha = 0.3f),
                            radius = 8.dp.toPx(),
                            center = lastPoint
                        )
                        drawCircle(
                            color = animatedColor,
                            radius = 4.5.dp.toPx(),
                            center = lastPoint
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stats Footer (Min, Avg, Max)
            if (extractedValues.isNotEmpty()) {
                val minVal = extractedValues.minOrNull() ?: 0f
                val maxVal = extractedValues.maxOrNull() ?: 0f
                val avgVal = extractedValues.average().toFloat()

                fun formatMetric(v: Float): String = when (selectedMetric) {
                    ChartMetricType.COMPOSITE -> "${v.roundToInt()}%"
                    ChartMetricType.RSRP -> "${v.roundToInt()} dBm"
                    ChartMetricType.RSRQ -> "${String.format("%.1f", v)} dB"
                    ChartMetricType.SINR -> "${if (v > 0) "+" else ""}${v.roundToInt()} dB"
                }

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
                            Text(text = formatMetric(minVal), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = "Rata-Rata", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = formatMetric(avgVal), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = "Tertinggi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = formatMetric(maxVal), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFF00C853))
                        }
                    }
                }
            }
        }
    }
}
