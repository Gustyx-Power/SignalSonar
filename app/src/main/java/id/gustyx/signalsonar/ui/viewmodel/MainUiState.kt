package id.gustyx.signalsonar.ui.viewmodel

import id.gustyx.signalsonar.data.model.ObstacleAnalysis
import id.gustyx.signalsonar.data.model.PeakSignal
import id.gustyx.signalsonar.data.model.SignalData
import id.gustyx.signalsonar.data.model.TrafficData

/**
 * Comprehensive UI State including RF telemetry, obstacle analysis, and live traffic metrics.
 */
data class MainUiState(
    val currentSignal: SignalData = SignalData(),
    val peakSignal: PeakSignal? = null,
    val obstacleAnalysis: ObstacleAnalysis = ObstacleAnalysis(),
    val trafficData: TrafficData = TrafficData(),
    val isSonarAudioEnabled: Boolean = true,
    val inAppVolume: Float = 0.85f,
    val isKeepScreenOnEnabled: Boolean = true,
    val hasRequiredPermissions: Boolean = false,
    val currentIntervalMs: Long = 1500L,
    val pulseTriggerEpoch: Long = 0L,
    val isAppForegrounded: Boolean = true,
    val selectedTab: id.gustyx.signalsonar.ui.navigation.AppTab = id.gustyx.signalsonar.ui.navigation.AppTab.SONAR
) {
    val cellTowerInfo: id.gustyx.signalsonar.data.model.CellTowerInfo
        get() = currentSignal.cellTowerInfo

    val isActivelyTracking: Boolean
        get() = hasRequiredPermissions && currentSignal.isSignalValid
}
