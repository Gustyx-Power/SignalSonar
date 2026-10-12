package id.gustyx.signalsonar.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import id.gustyx.signalsonar.ui.components.AppBottomNavigation
import id.gustyx.signalsonar.ui.components.ModernAppHeader
import id.gustyx.signalsonar.ui.components.PermissionDeniedState
import id.gustyx.signalsonar.ui.navigation.AppTab
import id.gustyx.signalsonar.ui.screen.tabs.BtsTabScreen
import id.gustyx.signalsonar.ui.screen.tabs.DiagnosticsTabScreen
import id.gustyx.signalsonar.ui.screen.tabs.SonarTabScreen
import id.gustyx.signalsonar.ui.screen.tabs.TrafficTabScreen
import id.gustyx.signalsonar.ui.viewmodel.MainViewModel

/**
 * Main Screen with Ookla Speedtest-style Bottom Navigation Bar.
 * Hosts 4 distinct tab screens: Sonar Radar, Menara BTS, Diagnostik, and Trafik.
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
        bottomBar = {
            if (uiState.hasRequiredPermissions) {
                AppBottomNavigation(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = { viewModel.selectTab(it) }
                )
            }
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = uiState.selectedTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tabTransition"
                ) { targetTab ->
                    when (targetTab) {
                        AppTab.SONAR -> SonarTabScreen(
                            uiState = uiState,
                            viewModel = viewModel
                        )
                        AppTab.BTS -> BtsTabScreen(
                            uiState = uiState
                        )
                        AppTab.DIAGNOSTICS -> DiagnosticsTabScreen(
                            uiState = uiState
                        )
                        AppTab.TRAFFIC -> TrafficTabScreen(
                            uiState = uiState
                        )
                    }
                }
            }
        }
    }
}
