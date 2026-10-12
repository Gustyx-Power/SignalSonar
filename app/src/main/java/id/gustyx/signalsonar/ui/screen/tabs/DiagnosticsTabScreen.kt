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
import id.gustyx.signalsonar.ui.components.LatencyTestCard
import id.gustyx.signalsonar.ui.components.ObstacleAnalysisCard
import id.gustyx.signalsonar.ui.components.RfMetricsCard
import id.gustyx.signalsonar.ui.viewmodel.MainUiState
import id.gustyx.signalsonar.ui.viewmodel.MainViewModel

/**
 * Tab 3: Diagnostics Screen.
 * Hosts physical obstacle penetration loss analysis, RF interference levels,
 * smartphone positioning tips, Google DNS latency test, and detailed RF engineering telemetry (RSRP, RSRQ, SINR, CQI).
 */
@Composable
fun DiagnosticsTabScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
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
        // 1. Google DNS Latency, Jitter & Packet Loss Test Card
        LatencyTestCard(
            latencyResult = uiState.latencyResult,
            isTesting = uiState.isLatencyTesting,
            onRunTest = { viewModel.runLatencyTest("8.8.8.8") }
        )

        // 2. Environmental & Obstacle Analysis Card
        ObstacleAnalysisCard(
            obstacleAnalysis = uiState.obstacleAnalysis
        )

        // 3. RF Engineering Telemetry Grid Card
        RfMetricsCard(
            signalData = uiState.currentSignal
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
