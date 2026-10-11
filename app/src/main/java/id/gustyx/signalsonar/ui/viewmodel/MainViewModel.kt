package id.gustyx.signalsonar.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.gustyx.signalsonar.audio.SignalIntervalMapper
import id.gustyx.signalsonar.audio.SonarAudioEngine
import id.gustyx.signalsonar.data.model.ObstacleAnalysis
import id.gustyx.signalsonar.data.model.PeakSignal
import id.gustyx.signalsonar.data.model.SignalData
import id.gustyx.signalsonar.telephony.SignalMonitor
import id.gustyx.signalsonar.telephony.TrafficMonitor
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
 * live traffic throughput, obstacle penetration loss analysis, and Media stream audio.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val signalMonitor = SignalMonitor(application)
    private val trafficMonitor = TrafficMonitor()
    val audioEngine = SonarAudioEngine(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var signalCollectorJob: Job? = null
    private var trafficCollectorJob: Job? = null
    private var sonarLoopJob: Job? = null

    init {
        audioEngine.setMuted(!_uiState.value.isSonarAudioEnabled)
        audioEngine.setInAppVolume(_uiState.value.inAppVolume)
    }

    fun updatePermissions(granted: Boolean) {
        _uiState.update { it.copy(hasRequiredPermissions = granted) }
        if (granted) {
            startSignalMonitoring()
            startTrafficMonitoring()
            startSonarLoop()
        } else {
            stopSignalMonitoring()
            stopTrafficMonitoring()
            stopSonarLoop()
        }
    }

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

    private fun startTrafficMonitoring() {
        trafficCollectorJob?.cancel()
        trafficCollectorJob = viewModelScope.launch {
            trafficMonitor.observeTraffic()
                .catch { /* ignore */ }
                .collect { traffic ->
                    _uiState.update { it.copy(trafficData = traffic) }
                }
        }
    }

    private fun stopTrafficMonitoring() {
        trafficCollectorJob?.cancel()
        trafficCollectorJob = null
    }

    private fun handleNewSignalData(signal: SignalData) {
        val interval = SignalIntervalMapper.calculateIntervalMs(signal.dbm)
        val obstacle = ObstacleAnalysis.analyze(
            dbm = signal.dbm,
            rsrq = signal.rsrq,
            sinr = signal.sinr,
            timingAdvance = signal.timingAdvance
        )

        _uiState.update { current ->
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
                obstacleAnalysis = obstacle,
                currentIntervalMs = interval
            )
        }
    }

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
                            val pitch = if (dbm != null) {
                                0.9f + (((dbm + 110).coerceIn(0, 45) / 45f) * 0.35f)
                            } else {
                                1.0f
                            }
                            audioEngine.playPing(ratePitch = pitch)
                        }
                    }

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

    fun toggleSonarAudio() {
        _uiState.update { current ->
            val newState = !current.isSonarAudioEnabled
            audioEngine.setMuted(!newState)
            current.copy(isSonarAudioEnabled = newState)
        }
    }

    fun setInAppVolume(volume: Float) {
        val clamped = volume.coerceIn(0.0f, 1.0f)
        audioEngine.setInAppVolume(clamped)
        _uiState.update { it.copy(inAppVolume = clamped) }
    }

    fun testSonarPing() {
        audioEngine.playPing(ratePitch = 1.0f)
    }

    fun toggleKeepScreenOn() {
        _uiState.update { current ->
            current.copy(isKeepScreenOnEnabled = !current.isKeepScreenOnEnabled)
        }
    }

    fun resetPeakSignal() {
        _uiState.update { it.copy(peakSignal = null) }
    }

    fun onAppForeground() {
        _uiState.update { it.copy(isAppForegrounded = true) }
        audioEngine.resumeAll()
        if (_uiState.value.hasRequiredPermissions && sonarLoopJob?.isActive != true) {
            startSonarLoop()
        }
        if (_uiState.value.hasRequiredPermissions && trafficCollectorJob?.isActive != true) {
            startTrafficMonitoring()
        }
    }

    fun onAppBackground() {
        _uiState.update { it.copy(isAppForegrounded = false) }
        audioEngine.pauseAll()
    }

    override fun onCleared() {
        super.onCleared()
        stopSignalMonitoring()
        stopTrafficMonitoring()
        stopSonarLoop()
        audioEngine.release()
    }
}
