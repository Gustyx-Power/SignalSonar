package id.gustyx.signalsonar.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.gustyx.signalsonar.audio.SignalIntervalMapper
import id.gustyx.signalsonar.audio.SonarAudioEngine
import id.gustyx.signalsonar.data.model.PeakSignal
import id.gustyx.signalsonar.data.model.SignalData
import id.gustyx.signalsonar.telephony.SignalMonitor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel governing cellular monitoring, acoustic sonar timers, peak hold,
 * and UI state transitions following Unidirectional Data Flow (UDF).
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val signalMonitor = SignalMonitor(application)
    val audioEngine = SonarAudioEngine(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var signalCollectorJob: Job? = null
    private var sonarLoopJob: Job? = null

    init {
        audioEngine.setMuted(!_uiState.value.isSonarAudioEnabled)
    }

    /**
     * Updates permission status and triggers cellular monitoring when granted.
     */
    fun updatePermissions(granted: Boolean) {
        _uiState.update { it.copy(hasRequiredPermissions = granted) }
        if (granted) {
            startSignalMonitoring()
            startSonarLoop()
        } else {
            stopSignalMonitoring()
            stopSonarLoop()
        }
    }

    /**
     * Starts listening to telephony changes from the default data SIM.
     */
    private fun startSignalMonitoring() {
        signalCollectorJob?.cancel()
        signalCollectorJob = viewModelScope.launch {
            signalMonitor.observeSignal()
                .catch { emit(SignalData()) }
                .collect { signal ->
                    handleNewSignalData(signal)
                }
        }
    }

    private fun stopSignalMonitoring() {
        signalCollectorJob?.cancel()
        signalCollectorJob = null
    }

    private fun handleNewSignalData(signal: SignalData) {
        val interval = SignalIntervalMapper.calculateIntervalMs(signal.dbm)

        _uiState.update { current ->
            // Check peak signal update
            val currentPeak = current.peakSignal
            val newPeak = if (signal.isSignalValid && signal.dbm != null) {
                if (currentPeak == null || signal.dbm > currentPeak.dbm) {
                    PeakSignal(
                        dbm = signal.dbm,
                        networkType = signal.networkType
                    )
                } else {
                    currentPeak
                }
            } else {
                currentPeak
            }

            current.copy(
                currentSignal = signal,
                peakSignal = newPeak,
                currentIntervalMs = interval
            )
        }
    }

    /**
     * Coroutine timer loop producing acoustic sonar feedback at dynamically calculated intervals.
     */
    private fun startSonarLoop() {
        sonarLoopJob?.cancel()
        sonarLoopJob = viewModelScope.launch {
            while (isActive) {
                val state = _uiState.value

                if (state.isAppForegrounded && state.hasRequiredPermissions) {
                    val dbm = state.currentSignal.dbm
                    val interval = state.currentIntervalMs

                    if (state.isSonarAudioEnabled && state.currentSignal.isSignalValid) {
                        if (SignalIntervalMapper.shouldUseFlatTone(dbm)) {
                            audioEngine.playFlat()
                        } else {
                            // Dynamic pitch scaling based on dBm (-75 dBm is crisp high, -100 dBm is lower)
                            val pitch = if (dbm != null) {
                                0.9f + (((dbm + 110).coerceIn(0, 45) / 45f) * 0.35f)
                            } else {
                                1.0f
                            }
                            audioEngine.playPing(ratePitch = pitch)
                        }
                    }

                    // Dispatch visual ripple trigger timestamp
                    _uiState.update { it.copy(pulseTriggerEpoch = System.currentTimeMillis()) }

                    delay(interval)
                } else {
                    delay(500L)
                }
            }
        }
    }

    private fun stopSonarLoop() {
        sonarLoopJob?.cancel()
        sonarLoopJob = null
    }

    /**
     * Toggles acoustic sonar audio on/off.
     */
    fun toggleSonarAudio() {
        _uiState.update { current ->
            val newState = !current.isSonarAudioEnabled
            audioEngine.setMuted(!newState)
            current.copy(isSonarAudioEnabled = newState)
        }
    }

    /**
     * Toggles whether the screen should remain turned on during active sonar tracking.
     */
    fun toggleKeepScreenOn() {
        _uiState.update { current ->
            current.copy(isKeepScreenOnEnabled = !current.isKeepScreenOnEnabled)
        }
    }

    /**
     * Resets peak signal hold tracking to allow starting a fresh measurement session.
     */
    fun resetPeakSignal() {
        _uiState.update { it.copy(peakSignal = null) }
    }

    /**
     * Triggered when the Activity transitions to foreground.
     */
    fun onAppForeground() {
        _uiState.update { it.copy(isAppForegrounded = true) }
        audioEngine.resumeAll()
        if (_uiState.value.hasRequiredPermissions && sonarLoopJob?.isActive != true) {
            startSonarLoop()
        }
    }

    /**
     * Strict lifecycle management: triggered on Activity onPause/onStop
     * immediately halts audio playback and timer loop to prevent background battery/sound leaks.
     */
    fun onAppBackground() {
        _uiState.update { it.copy(isAppForegrounded = false) }
        audioEngine.pauseAll()
    }

    override fun onCleared() {
        super.onCleared()
        stopSignalMonitoring()
        stopSonarLoop()
        audioEngine.release()
    }
}
