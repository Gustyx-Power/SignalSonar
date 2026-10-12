package id.gustyx.signalsonar.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation tabs matching the Ookla Speedtest multi-screen bottom navigation pattern.
 */
enum class AppTab(
    val title: String,
    val icon: ImageVector
) {
    SONAR(
        title = "Sonar",
        icon = Icons.Rounded.Sensors
    ),
    BTS(
        title = "Menara BTS",
        icon = Icons.Rounded.CellTower
    ),
    DIAGNOSTICS(
        title = "Diagnostik",
        icon = Icons.Rounded.Analytics
    ),
    TRAFFIC(
        title = "Trafik",
        icon = Icons.Rounded.SwapVert
    )
}
