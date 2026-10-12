package id.gustyx.signalsonar.data.model

import androidx.compose.ui.graphics.Color

/**
 * Representation of cellular signal quality tier, mapped directly to
 * Sonar radar interval speeds, colors, and human-readable feedback.
 */
enum class SignalQuality(
    val label: String,
    val description: String,
    val color: Color,
    val baseIntervalMs: Long
) {
    EXCELLENT(
        label = "Sangat Kuat",
        description = "Sinyal prima, posisi antena & modem sangat ideal",
        color = Color(0xFF00E676), // Vibrant Neon Green
        baseIntervalMs = 150L
    ),
    GOOD(
        label = "Bagus / Optimal",
        description = "Koneksi stabil untuk aktivitas internet berat",
        color = Color(0xFF00E5FF), // Cyan / Bright Teal
        baseIntervalMs = 400L
    ),
    POOR(
        label = "Cukup / Lemah",
        description = "Sinyal terbatas, disarankan mencari sudut lain",
        color = Color(0xFFFFB300), // Amber / Orange
        baseIntervalMs = 800L
    ),
    CRITICAL(
        label = "Sangat Buruk / Hilang",
        description = "Sinyal kritis atau terputus",
        color = Color(0xFFFF5252), // Coral Red
        baseIntervalMs = 1500L
    ),
    UNKNOWN(
        label = "Mencari Sinyal…",
        description = "Menunggu data pembacaan antena seluler",
        color = Color(0xFF90A4AE), // Neutral Blue Grey
        baseIntervalMs = 2000L
    );

    companion object {
        fun fromDbm(dbm: Int?): SignalQuality {
            if (dbm == null || dbm == 0 || dbm < -140 || dbm > -40) {
                return UNKNOWN
            }
            return when {
                dbm >= -75 -> EXCELLENT
                dbm in -90..-76 -> GOOD
                dbm in -105..-91 -> POOR
                else -> CRITICAL
            }
        }

        fun fromCompositeScore(score: Int): SignalQuality {
            return when {
                score >= 80 -> EXCELLENT
                score in 55..79 -> GOOD
                score in 30..54 -> POOR
                score > 0 -> CRITICAL
                else -> UNKNOWN
            }
        }
    }
}
