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
import id.gustyx.signalsonar.ui.components.PeakSignalCard
import id.gustyx.signalsonar.ui.components.RadarVisualizer
import id.gustyx.signalsonar.ui.components.SignalQualityChart
import id.gustyx.signalsonar.ui.components.SignalStatsCard
import id.gustyx.signalsonar.ui.components.SonarControlPanel
import id.gustyx.signalsonar.ui.viewmodel.MainUiState
import id.gustyx.signalsonar.ui.viewmodel.MainViewModel

/**
 * Tab 1: Primary Sonar Radar Screen.
 * Hosts Hero Signal Visualizer, Real-time 30-Sample Trend Chart, Peak Hold, Sonar Audio, and Operator stats.
 */
@Composable
fun SonarTabScreen(
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
        // 1. Hero Material 3 Signal Visualizer
        RadarVisualizer(
            signalData = uiState.currentSignal,
            pulseTriggerEpoch = uiState.pulseTriggerEpoch,
            intervalMs = uiState.currentIntervalMs
        )

        // 2. Real-Time Signal Quality Trend Chart (30 Rolling Measurements)
        SignalQualityChart(
            history = uiState.signalHistory,
            currentSignal = uiState.currentSignal
        )

        // 3. Peak Signal Hold Card
        PeakSignalCard(
            peakSignal = uiState.peakSignal,
            onResetPeak = { viewModel.resetPeakSignal() }
        )

        // 4. Operator & SIM Card
        SignalStatsCard(
            signalData = uiState.currentSignal
        )

        // 5. Sonar Control Panel (Volume, Mute, Test Ping, Screen On)
        SonarControlPanel(
            isAudioEnabled = uiState.isSonarAudioEnabled,
            onToggleAudio = { viewModel.toggleSonarAudio() },
            volume = uiState.inAppVolume,
            onVolumeChange = { viewModel.setInAppVolume(it) },
            onTestPing = { viewModel.testSonarPing() },
            isKeepScreenOn = uiState.isKeepScreenOnEnabled,
            onToggleKeepScreenOn = { viewModel.toggleKeepScreenOn() },
            intervalMs = uiState.currentIntervalMs
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
