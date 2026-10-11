package id.gustyx.signalsonar.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import id.gustyx.signalsonar.ui.components.PeakSignalCard
import id.gustyx.signalsonar.ui.components.PermissionDeniedState
import id.gustyx.signalsonar.ui.components.RadarVisualizer
import id.gustyx.signalsonar.ui.components.SignalStatsCard
import id.gustyx.signalsonar.ui.components.SonarControlPanel
import id.gustyx.signalsonar.ui.viewmodel.MainViewModel

/**
 * Primary Screen orchestrating the Radar visualizer, real-time signal statistics,
 * acoustic feedback toggles, and permissions management.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Sensors,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "SignalSonar",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "Pencari Sinyal Seluler",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Active Status Dot
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (uiState.isActivelyTracking) uiState.currentSignal.quality.color
                                else Color.Gray.copy(alpha = 0.5f)
                            )
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
                // Background subtle ambient radial glow matching active signal quality
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    uiState.currentSignal.quality.color.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Hero Radar Visualizer
                    RadarVisualizer(
                        signalData = uiState.currentSignal,
                        pulseTriggerEpoch = uiState.pulseTriggerEpoch,
                        intervalMs = uiState.currentIntervalMs,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Real-time Network & Carrier Information
                    SignalStatsCard(
                        signalData = uiState.currentSignal
                    )

                    // Peak Signal Hold Card
                    PeakSignalCard(
                        peakSignal = uiState.peakSignal,
                        onResetPeak = { viewModel.resetPeakSignal() }
                    )

                    // Acoustic & Screen Control Panel
                    SonarControlPanel(
                        isAudioEnabled = uiState.isSonarAudioEnabled,
                        onToggleAudio = { viewModel.toggleSonarAudio() },
                        isKeepScreenOn = uiState.isKeepScreenOnEnabled,
                        onToggleKeepScreenOn = { viewModel.toggleKeepScreenOn() },
                        intervalMs = uiState.currentIntervalMs
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}
