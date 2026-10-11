package id.gustyx.signalsonar.ui.viewmodel

import id.gustyx.signalsonar.data.model.PeakSignal
import id.gustyx.signalsonar.data.model.SignalData

/**
 * Immutable UI State representing the current screen snapshot.
 */
data class MainUiState(
    val currentSignal: SignalData = SignalData(),
    val peakSignal: PeakSignal? = null,
    val isSonarAudioEnabled: Boolean = true,
    val isKeepScreenOnEnabled: Boolean = true,
    val hasRequiredPermissions: Boolean = false,
    val currentIntervalMs: Long = 1500L,
    val pulseTriggerEpoch: Long = 0L, // Changes every time a beep pulse fires
    val isAppForegrounded: Boolean = true
) {
    val isActivelyTracking: Boolean
        get() = hasRequiredPermissions && currentSignal.isSignalValid
}
