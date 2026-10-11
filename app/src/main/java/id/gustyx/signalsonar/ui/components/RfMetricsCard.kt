package id.gustyx.signalsonar.ui.components

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
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.gustyx.signalsonar.data.model.SignalData

/**
 * Material 3 RF (Radio Frequency) Metrics Card.
 * Displays RSRP, RSRQ, SINR, Timing Advance, and Cell Identity parameters.
 */
@Composable
fun RfMetricsCard(
    signalData: SignalData,
    modifier: Modifier = Modifier
) {
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
                        imageVector = Icons.Rounded.Analytics,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Metrik RF Seluler",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "RSRP • RSRQ • SINR • Timing Advance",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (signalData.band != null) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = signalData.band,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        border = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2x2 Grid of Material 3 Metric Tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RfMetricTile(
                    label = "RSRP (Daya Sinyal)",
                    value = signalData.formattedRsrp,
                    description = getRsrpQualityText(signalData.rsrp),
                    color = getRsrpColor(signalData.rsrp),
                    modifier = Modifier.weight(1f)
                )

                RfMetricTile(
                    label = "RSRQ (Kualitas Sinyal)",
                    value = signalData.formattedRsrq,
                    description = getRsrqQualityText(signalData.rsrq),
                    color = getRsrqColor(signalData.rsrq),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RfMetricTile(
                    label = "SINR / RSSNR (Noise)",
                    value = signalData.formattedSinr,
                    description = getSinrQualityText(signalData.sinr),
                    color = getSinrColor(signalData.sinr),
                    modifier = Modifier.weight(1f)
                )

                RfMetricTile(
                    label = "Estimasi Jarak BTS",
                    value = if (signalData.estimatedDistanceMeters != null) {
                        "~${signalData.estimatedDistanceMeters} m"
                    } else if (signalData.timingAdvance != null) {
                        "TA: ${signalData.timingAdvance}"
                    } else {
                        "--"
                    },
                    description = if (signalData.timingAdvance != null) {
                        "TA Step: ${signalData.timingAdvance}"
                    } else {
                        "Belum tersedia"
                    },
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Cell Identity Footer
            val hasCellInfo = signalData.pci != null || signalData.tac != null || signalData.ci != null
            if (hasCellInfo) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Identitas Seluler (Cell ID):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = buildString {
                            if (signalData.pci != null) append("PCI ${signalData.pci} ")
                            if (signalData.tac != null) append("• TAC ${signalData.tac} ")
                            if (signalData.ci != null) append("• CID ${signalData.ci}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun RfMetricTile(
    label: String,
    value: String,
    description: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getRsrpColor(rsrp: Int?): Color {
    if (rsrp == null) return Color.Gray
    return when {
        rsrp >= -80 -> Color(0xFF00C853)
        rsrp in -95..-81 -> Color(0xFF00B0FF)
        rsrp in -105..-96 -> Color(0xFFFFB300)
        else -> Color(0xFFFF5252)
    }
}

private fun getRsrpQualityText(rsrp: Int?): String {
    if (rsrp == null) return "--"
    return when {
        rsrp >= -80 -> "Sangat Kuat"
        rsrp in -95..-81 -> "Bagus"
        rsrp in -105..-96 -> "Cukup / Lemah"
        else -> "Sangat Buruk"
    }
}

private fun getRsrqColor(rsrq: Int?): Color {
    if (rsrq == null) return Color.Gray
    return when {
        rsrq >= -9 -> Color(0xFF00C853)
        rsrq in -13..-10 -> Color(0xFF00B0FF)
        rsrq in -17..-14 -> Color(0xFFFFB300)
        else -> Color(0xFFFF5252)
    }
}

private fun getRsrqQualityText(rsrq: Int?): String {
    if (rsrq == null) return "--"
    return when {
        rsrq >= -9 -> "Optimal"
        rsrq in -13..-10 -> "Standar Baik"
        rsrq in -17..-14 -> "Sedang"
        else -> "Banyak Derau"
    }
}

private fun getSinrColor(sinr: Int?): Color {
    if (sinr == null) return Color.Gray
    return when {
        sinr >= 13 -> Color(0xFF00C853)
        sinr in 6..12 -> Color(0xFF00B0FF)
        sinr in 0..5 -> Color(0xFFFFB300)
        else -> Color(0xFFFF5252)
    }
}

private fun getSinrQualityText(sinr: Int?): String {
    if (sinr == null) return "--"
    return when {
        sinr >= 13 -> "Bersih"
        sinr in 6..12 -> "Wajar"
        sinr in 0..5 -> "Noise Tinggi"
        else -> "Kritis"
    }
}
