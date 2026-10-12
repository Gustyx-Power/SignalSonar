package id.gustyx.signalsonar.data.model

import androidx.compose.ui.graphics.Color
import java.util.Locale

/**
 * Categorization of the smartphone's position relative to the cellular BTS tower coverage radius.
 */
enum class CoverageZone(
    val label: String,
    val shortBadge: String,
    val description: String,
    val color: Color
) {
    NEAR_TOWER(
        label = "Dekat Menara (Near-Tower)",
        shortBadge = "Dekat Menara",
        description = "HP berada sangat dekat dengan pusat pemancar BTS (< 25% radius sel). Sinyal Line-of-Sight kuat dan konsumsi daya baterai rendah.",
        color = Color(0xFF00C853)
    ),
    MID_CELL(
        label = "Tengah Jangkauan (Mid-Cell)",
        shortBadge = "Tengah Sel",
        description = "HP berada di zona seimbang jangkauan sel (25% - 70% radius). Kecepatan data stabil.",
        color = Color(0xFF00B0FF)
    ),
    CELL_EDGE(
        label = "Tepi Jangkauan (Cell-Edge)",
        shortBadge = "Tepi Sel",
        description = "HP berada di batas jangkauan sel (> 70% radius). Rentan peralihan antar-menara (handover) atau jitter bertambah.",
        color = Color(0xFFFFB300)
    ),
    OUT_OF_RANGE(
        label = "Batas Ekstrem / Melemah",
        shortBadge = "Batas Ekstrem",
        description = "Posisi berada di batas terluar jangkauan antena menara BTS pemancar.",
        color = Color(0xFFFF5252)
    ),
    UNKNOWN(
        label = "Mengukur Jangkauan...",
        shortBadge = "Menganalisis",
        description = "Menunggu data telemetri radio seluler dari jaringan operator...",
        color = Color(0xFF90A4AE)
    )
}

/**
 * Neighboring cellular tower / cell detected by the smartphone's radio.
 */
data class NeighborCell(
    val pci: Int? = null,
    val dbm: Int? = null,
    val networkType: String = "LTE",
    val isRegistered: Boolean = false
) {
    val formattedDbm: String
        get() = if (dbm != null) "$dbm dBm" else "-- dBm"
}

/**
 * Comprehensive Cell Tower (BTS) telemetry, identity, and coverage range analysis.
 */
data class CellTowerInfo(
    val enodebId: Int? = null,                 // Macro eNodeB ID (LTE) or gNodeB ID (5G)
    val sectorId: Int? = null,                 // Local Antenna Sector index (0..255)
    val cellId: Long? = null,                  // Full Cell Identity (ECI / NCI)
    val pci: Int? = null,                      // Physical Cell Identity
    val tac: Int? = null,                      // Tracking Area Code
    val band: String? = null,                  // Operating Band (e.g., B3 1800 MHz)
    val earfcn: Int? = null,                   // Radio Channel Number
    val mcc: String? = null,                   // Mobile Country Code (e.g., 510)
    val mnc: String? = null,                   // Mobile Network Code (e.g., 10 Telkomsel)
    val timingAdvance: Int? = null,            // Timing Advance step
    val distanceMeters: Int? = null,           // Calculated / estimated distance to BTS
    val isDistancePrecise: Boolean = false,    // True if calculated from radio Timing Advance
    val estimatedMaxCoverageMeters: Int = 3000,// Maximum theoretical coverage radius for this band
    val coveragePercentage: Float = 0f,        // 0.0 to 1.0 representation of cell radius position
    val coverageZone: CoverageZone = CoverageZone.UNKNOWN,
    val neighborCells: List<NeighborCell> = emptyList()
) {
    val formattedDistance: String
        get() = when {
            distanceMeters == null -> "Belum terdeteksi"
            distanceMeters >= 1000 -> String.format(Locale.US, "%.2f km", distanceMeters / 1000f)
            else -> "$distanceMeters meter"
        }

    val formattedMaxCoverage: String
        get() = when {
            estimatedMaxCoverageMeters >= 1000 -> String.format(Locale.US, "%.1f km", estimatedMaxCoverageMeters / 1000f)
            else -> "$estimatedMaxCoverageMeters meter"
        }

    val distanceCalculationMethod: String
        get() = if (isDistancePrecise) {
            "Timing Advance (Presisi Radio)"
        } else {
            "Estimasi Redaman Sinyal (Path Loss)"
        }

    val plmnString: String
        get() = if (!mcc.isNullOrBlank() && !mnc.isNullOrBlank()) "$mcc-$mnc" else "--"
}
