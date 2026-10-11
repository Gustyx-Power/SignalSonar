package id.gustyx.signalsonar.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import id.gustyx.signalsonar.ui.components.ModernAppHeader
import id.gustyx.signalsonar.ui.components.ObstacleAnalysisCard
import id.gustyx.signalsonar.ui.components.PeakSignalCard
import id.gustyx.signalsonar.ui.components.PermissionDeniedState
import id.gustyx.signalsonar.ui.components.RadarVisualizer
import id.gustyx.signalsonar.ui.components.RfMetricsCard
import id.gustyx.signalsonar.ui.components.SignalStatsCard
import id.gustyx.signalsonar.ui.components.SonarControlPanel
import id.gustyx.signalsonar.ui.components.TrafficMonitorCard
import id.gustyx.signalsonar.ui.viewmodel.MainViewModel

/**
 * Main Screen built entirely with Material 3 (Material You) components.
 * Assembles the TopAppBar, Hero Signal Visualizer, environmental obstacle analysis,
 * RF telemetry, real-time traffic speeds, and sonar audio controls.
 */
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_PHONE_STATE
    )

    fun checkPermissionsGranted(): Boolean {
        return requiredPermissions.all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = requiredPermissions.all { perm -> results[perm] == true }
        viewModel.updatePermissions(allGranted)
    }

    LaunchedEffect(Unit) {
        val granted = checkPermissionsGranted()
        viewModel.updatePermissions(granted)
        if (!granted) {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ModernAppHeader(
                signalData = uiState.currentSignal,
                trafficData = uiState.trafficData,
                intervalMs = uiState.currentIntervalMs,
                isAudioEnabled = uiState.isSonarAudioEnabled,
                onToggleAudio = { viewModel.toggleSonarAudio() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (!uiState.hasRequiredPermissions) {
            PermissionDeniedState(
                onRequestPermission = {
                    permissionLauncher.launch(requiredPermissions)
                },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
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

                // 2. Environmental & Obstacle Propagation Analysis Card
                ObstacleAnalysisCard(
                    obstacleAnalysis = uiState.obstacleAnalysis
                )

                // 3. RF Engineering Telemetry (RSRP, RSRQ, SINR, TA, Cell ID)
                RfMetricsCard(
                    signalData = uiState.currentSignal
                )

                // 4. Real-time Network Throughput Traffic (Rx/Tx)
                TrafficMonitorCard(
                    trafficData = uiState.trafficData
                )

                // 5. Operator, SIM Slot, and Signal Bars Card
                SignalStatsCard(
                    signalData = uiState.currentSignal
                )

                // 6. Peak Signal Hold Card
                PeakSignalCard(
                    peakSignal = uiState.peakSignal,
                    onResetPeak = { viewModel.resetPeakSignal() }
                )

                // 7. Sonar Control Panel (Media Volume, Sound Toggle, Keep Screen On)
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

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
