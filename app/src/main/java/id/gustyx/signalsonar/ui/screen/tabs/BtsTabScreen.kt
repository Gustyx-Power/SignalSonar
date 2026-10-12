package id.gustyx.signalsonar.ui.screen.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.gustyx.signalsonar.ui.components.BtsCoverageCard
import id.gustyx.signalsonar.ui.components.BtsIdentityCard
import id.gustyx.signalsonar.ui.components.BtsNeighborCellsCard
import id.gustyx.signalsonar.ui.viewmodel.MainUiState

/**
 * Tab 2: Cell Tower (BTS) Telemetry & Coverage Screen.
 * Hosts the BTS coverage range visualizer, eNodeB/gNodeB breakdown, and neighbor cells.
 */
@Composable
fun BtsTabScreen(
    uiState: MainUiState,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val towerInfo = uiState.cellTowerInfo

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. BTS Coverage Radius & Distance Visualizer
        BtsCoverageCard(
            cellTowerInfo = towerInfo
        )

        // 2. BTS Identity (eNodeB ID, Sector ID, PCI, TAC, Band)
        BtsIdentityCard(
            cellTowerInfo = towerInfo,
            operatorName = uiState.currentSignal.operatorName
        )

        // 3. Neighbor Cells Detected by Radio
        BtsNeighborCellsCard(
            neighborCells = towerInfo.neighborCells,
            servingPci = towerInfo.pci
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
