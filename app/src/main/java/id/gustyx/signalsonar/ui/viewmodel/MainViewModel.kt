package id.gustyx.signalsonar.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.gustyx.signalsonar.audio.SignalIntervalMapper
import id.gustyx.signalsonar.audio.SonarAudioEngine
import id.gustyx.signalsonar.data.model.ObstacleAnalysis
import id.gustyx.signalsonar.data.model.PeakSignal
import id.gustyx.signalsonar.data.model.SignalData
import id.gustyx.signalsonar.network.LatencyTester
import id.gustyx.signalsonar.network.NetworkUtils
import id.gustyx.signalsonar.telephony.SignalMonitor
import id.gustyx.signalsonar.telephony.TrafficMonitor
import id.gustyx.signalsonar.ui.navigation.AppTab
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
 * Main ViewModel managing real-time cellular signal collection, Dual SIM switching,
 * acoustic sonar loops, latency testing, rolling signal history, and traffic telemetry.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val signalMonitor = SignalMonitor(application)
    private val trafficMonitor = TrafficMonitor()
    private val latencyTester = LatencyTester()
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
            refreshSimCardsAndIp()
            startSignalMonitoring()
            startTrafficMonitoring()
            startSonarLoop()
        } else {
            stopSignalMonitoring()
            stopTrafficMonitoring()
            stopSonarLoop()
        }
    }

    fun refreshSimCardsAndIp() {
        val sims = signalMonitor.getActiveSimCards()
        val (ipv4, ipv6) = NetworkUtils.getCellularIpAddresses()
        _uiState.update { current ->
            current.copy(
                availableSims = sims,
                cellularIpv4 = ipv4,
                cellularIpv6 = ipv6
            )
        }
    }

    fun switchSim(subId: Int) {
        if (_uiState.value.selectedSubId == subId) return
        _uiState.update { it.copy(selectedSubId = subId, signalHistory = emptyList()) }
        startSignalMonitoring()
    }

    private fun startSignalMonitoring() {
        signalCollectorJob?.cancel()
        val targetSubId = _uiState.value.selectedSubId
        signalCollectorJob = viewModelScope.launch {
            signalMonitor.observeSignal(targetSubId)
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
        val interval = SignalIntervalMapper.calculateIntervalMsFromScore(
            compositeScore = signal.compositeScore,
            fallbackDbm = signal.dbm
        )
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

            // Append to rolling signal history (last 30 measurements)
            val updatedHistory = if (signal.isSignalValid) {
                (current.signalHistory + id.gustyx.signalsonar.data.model.SignalSnapshot.fromSignalData(signal)).takeLast(30)
            } else {
                current.signalHistory
            }

            current.copy(
                currentSignal = signal,
                peakSignal = newPeak,
                obstacleAnalysis = obstacle,
                currentIntervalMs = interval,
                signalHistory = updatedHistory
            )
        }
    }

    private fun startSonarLoop() {
        sonarLoopJob?.cancel()
        sonarLoopJob = viewModelScope.launch {
            while (isActive) {
                val state = _uiState.value
                val isTracking = state.isActivelyTracking && state.isAppForegrounded
                val interval = state.currentIntervalMs

                if (isTracking && state.isSonarAudioEnabled) {
                    val useFlat = SignalIntervalMapper.shouldUseFlatToneFromScore(
                        compositeScore = state.currentSignal.compositeScore,
                        fallbackDbm = state.currentSignal.dbm
                    )
                    if (useFlat) {
                        audioEngine.playFlat()
                    } else {
                        val ratePitch = SignalIntervalMapper.calculatePitchRateFromScore(
                            compositeScore = state.currentSignal.compositeScore,
                            fallbackDbm = state.currentSignal.dbm
                        )
                        audioEngine.playPing(ratePitch)
                    }

                    _uiState.update { it.copy(pulseTriggerEpoch = System.currentTimeMillis()) }
                }

                delay(interval)
            }
        }
    }

    private fun stopSonarLoop() {
        sonarLoopJob?.cancel()
        sonarLoopJob = null
    }

    fun runLatencyTest(targetHost: String = "8.8.8.8") {
        if (_uiState.value.isLatencyTesting) return
        _uiState.update { it.copy(isLatencyTesting = true) }
        viewModelScope.launch {
            val result = latencyTester.testLatency(host = targetHost)
            _uiState.update { it.copy(latencyResult = result, isLatencyTesting = false) }
        }
    }

    fun toggleSonarAudio() {
        _uiState.update { current ->
            val newAudioState = !current.isSonarAudioEnabled
            audioEngine.setMuted(!newAudioState)
            current.copy(isSonarAudioEnabled = newAudioState)
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

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onAppForeground() {
        _uiState.update { it.copy(isAppForegrounded = true) }
        audioEngine.resumeAll()
        if (_uiState.value.hasRequiredPermissions) {
            refreshSimCardsAndIp()
            if (sonarLoopJob?.isActive != true) startSonarLoop()
            if (trafficCollectorJob?.isActive != true) startTrafficMonitoring()
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
