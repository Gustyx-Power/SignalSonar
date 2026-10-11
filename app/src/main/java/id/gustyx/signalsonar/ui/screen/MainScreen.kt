package id.gustyx.signalsonar.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
 * Primary Screen orchestrating the Tactical Radar Visualizer,
 * physical obstacle analysis, RF engineering telemetry, real-time throughput traffic,
 * and Media stream sonar controls.
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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Subtle ambient radial glow matching active signal quality
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    uiState.currentSignal.quality.color.copy(alpha = 0.09f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Hero Tactical Radar Visualizer
                    RadarVisualizer(
                        signalData = uiState.currentSignal,
                        pulseTriggerEpoch = uiState.pulseTriggerEpoch,
                        intervalMs = uiState.currentIntervalMs,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // 2. Obstacle & RF Interference Diagnostic Card
                    ObstacleAnalysisCard(
                        obstacleAnalysis = uiState.obstacleAnalysis
                    )

                    // 3. Advanced RF Engineering Telemetry (RSRP, RSRQ, SINR, TA, Cell ID)
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
}
