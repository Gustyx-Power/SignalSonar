package id.gustyx.signalsonar.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeMute
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.gustyx.signalsonar.data.model.SignalData
import id.gustyx.signalsonar.data.model.SimCardInfo
import id.gustyx.signalsonar.data.model.TrafficData

/**
 * Material 3 Top App Bar with Dual SIM Switcher & Network Mode Badges (4G+ CA, 5G NSA/SA).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernAppHeader(
    signalData: SignalData,
    trafficData: TrafficData,
    intervalMs: Long,
    isAudioEnabled: Boolean,
    onToggleAudio: () -> Unit,
    availableSims: List<SimCardInfo> = emptyList(),
    selectedSubId: Int? = null,
    onSelectSim: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Sensors,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "SignalSonar",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            // 4G+ / 5G Mode Badge
                            if (signalData.isCarrierAggregationActive) {
                                Spacer(modifier = Modifier.width(8.dp))
                                AssistChip(
                                    onClick = {},
                                    label = { Text("4G+ CA", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = Color(0xFF00B0FF).copy(alpha = 0.15f),
                                        labelColor = Color(0xFF00B0FF)
                                    ),
                                    border = null
                                )
                            } else if (signalData.nrMode != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                AssistChip(
                                    onClick = {},
                                    label = { Text(signalData.nrMode, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = Color(0xFF00C853).copy(alpha = 0.15f),
                                        labelColor = Color(0xFF00C853)
                                    ),
                                    border = null
                                )
                            }
                        }
                        Text(
                            text = "${signalData.operatorName} • SIM ${signalData.simSlotIndex + 1} (${signalData.networkType.displayName})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    FilledTonalIconButton(
                        onClick = onToggleAudio,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (isAudioEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = if (isAudioEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = if (isAudioEnabled) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeMute,
                            contentDescription = if (isAudioEnabled) "Matikan Suara Sonar" else "Nyalakan Suara Sonar"
                        )
                    }
                }
            )

            // Dual SIM Switcher Bar (Visible if device has multiple active SIMs)
            if (availableSims.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pilih SIM:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    availableSims.forEach { sim ->
                        val isSelected = (selectedSubId != null && sim.subscriptionId == selectedSubId) ||
                                (selectedSubId == null && sim.isDefaultData)

                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectSim(sim.subscriptionId) },
                            label = {
                                Text(
                                    text = "SIM ${sim.slotIndex + 1}: ${sim.carrierName.ifBlank { "Slot ${sim.slotIndex + 1}" }}",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }
    }
}
