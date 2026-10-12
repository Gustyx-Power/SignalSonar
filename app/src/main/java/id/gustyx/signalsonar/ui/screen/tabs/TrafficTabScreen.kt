package id.gustyx.signalsonar.ui.screen.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.gustyx.signalsonar.ui.components.TrafficMonitorCard
import id.gustyx.signalsonar.ui.viewmodel.MainUiState

/**
 * Tab 4: Traffic & Network Details Screen.
 * Hosts real-time throughput speedometers (Download/Upload), session data usage,
 * and cellular subscription registration details.
 */
@Composable
fun TrafficTabScreen(
    uiState: MainUiState,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Real-time Network Throughput Traffic (Rx/Tx)
        TrafficMonitorCard(
            trafficData = uiState.trafficData
        )

        // 2. Cellular Connection & Subscription Details Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.NetworkCheck,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Status Koneksi Seluler",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Konfigurasi kartu SIM & jaringan data",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = if (uiState.currentSignal.isConnected) "Terhubung" else "Mencari",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        border = null
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                ConnectionInfoRow(
                    label = "Operator Aktif",
                    value = uiState.currentSignal.operatorName
                )
                ConnectionInfoRow(
                    label = "Slot SIM",
                    value = "SIM ${uiState.currentSignal.simSlotIndex + 1} (Default Data)"
                )
                ConnectionInfoRow(
                    label = "Tipe Jaringan",
                    value = uiState.currentSignal.networkType.displayName
                )
                ConnectionInfoRow(
                    label = "Kekuatan Sinyal",
                    value = uiState.currentSignal.formattedDbm
                )

                if (uiState.currentSignal.asu != null) {
                    ConnectionInfoRow(
                        label = "Arbitrary Strength Unit (ASU)",
                        value = "${uiState.currentSignal.asu}"
                    )
                }

                if (uiState.currentSignal.band != null) {
                    ConnectionInfoRow(
                        label = "Pita Frekuensi (Band)",
                        value = uiState.currentSignal.band
                    )
                }

                ConnectionInfoRow(
                    label = "Status Roaming",
                    value = if (uiState.currentSignal.isRoaming) "Roaming Aktif" else "Jaringan Asal (Home)"
                )

                if (uiState.currentSignal.countryIso.isNotBlank()) {
                    ConnectionInfoRow(
                        label = "Wilayah / Negara (ISO)",
                        value = uiState.currentSignal.countryIso
                    )
                }

                ConnectionInfoRow(
                    label = "Carrier Aggregation (4G+)",
                    value = if (uiState.currentSignal.isCarrierAggregationActive) "Aktif (LTE-A)" else "Single Carrier"
                )

                if (uiState.cellularIpv4 != null) {
                    ConnectionInfoRow(
                        label = "Alamat IP Seluler (IPv4)",
                        value = uiState.cellularIpv4
                    )
                }

                if (uiState.cellularIpv6 != null) {
                    ConnectionInfoRow(
                        label = "Alamat IP Seluler (IPv6)",
                        value = uiState.cellularIpv6.take(24) + "..."
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Akses Jaringan:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Data Seluler Langsung",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ConnectionInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
