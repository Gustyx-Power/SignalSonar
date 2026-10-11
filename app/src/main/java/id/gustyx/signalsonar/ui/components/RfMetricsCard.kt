package id.gustyx.signalsonar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.gustyx.signalsonar.data.model.SignalData

/**
 * Advanced Radio Frequency (RF) Engineering Metrics Card.
 * Displays RSRP, RSRQ, SINR, CQI, Timing Advance, and Cell Identity parameters.
 */
@Composable
fun RfMetricsCard(
    signalData: SignalData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "METRIK RF & KUALITAS SINYAL",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
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
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = signalData.band,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2x2 Grid of RF Telemetry Tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // RSRP Tile
                RfMetricTile(
                    label = "RSRP (Daya Sinyal)",
                    value = signalData.formattedRsrp,
                    description = getRsrpQualityText(signalData.rsrp),
                    color = getRsrpColor(signalData.rsrp),
                    modifier = Modifier.weight(1f)
                )

                // RSRQ Tile
                RfMetricTile(
                    label = "RSRQ (Kualitas)",
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
                // SINR Tile
                RfMetricTile(
                    label = "SINR / RSSNR (Noise)",
                    value = signalData.formattedSinr,
                    description = getSinrQualityText(signalData.sinr),
                    color = getSinrColor(signalData.sinr),
                    modifier = Modifier.weight(1f)
                )

                // Timing Advance & CQI Tile
                val taText = if (signalData.timingAdvance != null) {
                    "TA: ${signalData.timingAdvance} (~${signalData.timingAdvance * 78}m)"
                } else if (signalData.cqi != null) {
                    "CQI: ${signalData.cqi}"
                } else {
                    "--"
                }

                RfMetricTile(
                    label = "JARAK BTS / CQI",
                    value = taText,
                    description = if (signalData.timingAdvance != null) "Estimasi radius jangkauan" else "Indikator saluran",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Cell Identity Footer
            if (signalData.pci != null || signalData.tac != null || signalData.ci != null) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "IDENTITAS SEL:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        signalData.pci?.let {
                            Text(
                                text = "PCI: $it",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        signalData.tac?.let {
                            Text(
                                text = "TAC: $it",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        signalData.ci?.let {
                            Text(
                                text = "CID: $it",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f))
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                maxLines = 1
            )
        }
    }
}

private fun getRsrpColor(rsrp: Int?): Color {
    return when {
        rsrp == null -> Color.Gray
        rsrp >= -80 -> Color(0xFF00E676)
        rsrp in -95..-81 -> Color(0xFF00E5FF)
        rsrp in -105..-96 -> Color(0xFFFFB300)
        else -> Color(0xFFFF5252)
    }
}

private fun getRsrpQualityText(rsrp: Int?): String {
    return when {
        rsrp == null -> "Belum terukur"
        rsrp >= -80 -> "Sangat Baik"
        rsrp in -95..-81 -> "Baik"
        rsrp in -105..-96 -> "Cukup Lemah"
        else -> "Kritis"
    }
}

private fun getRsrqColor(rsrq: Int?): Color {
    return when {
        rsrq == null -> Color.Gray
        rsrq >= -9 -> Color(0xFF00E676)
        rsrq in -13..-10 -> Color(0xFF00E5FF)
        rsrq in -17..-14 -> Color(0xFFFFB300)
        else -> Color(0xFFFF5252)
    }
}

private fun getRsrqQualityText(rsrq: Int?): String {
    return when {
        rsrq == null -> "Belum terukur"
        rsrq >= -9 -> "Noise Minimal"
        rsrq in -13..-10 -> "Optimal"
        rsrq in -17..-14 -> "Ada Beban Sel"
        else -> "Interferensi Berat"
    }
}

private fun getSinrColor(sinr: Int?): Color {
    return when {
        sinr == null -> Color.Gray
        sinr >= 13 -> Color(0xFF00E676)
        sinr in 6..12 -> Color(0xFF00E5FF)
        sinr in 0..5 -> Color(0xFFFFB300)
        else -> Color(0xFFFF5252)
    }
}

private fun getSinrQualityText(sinr: Int?): String {
    return when {
        sinr == null -> "Belum terukur"
        sinr >= 13 -> "Paling Jernih"
        sinr in 6..12 -> "Jernih Normal"
        sinr in 0..5 -> "Noise Terasa"
        else -> "Tabrakan Sinyal"
    }
}
