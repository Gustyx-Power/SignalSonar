package id.gustyx.signalsonar.data.model

import androidx.compose.ui.graphics.Color

enum class LatencyStatus {
    IDLE,
    TESTING,
    SUCCESS,
    FAILED
}

/**
 * Results of host ping, jitter, and packet loss tests to Google / Cloudflare.
 */
data class LatencyResult(
    val status: LatencyStatus = LatencyStatus.IDLE,
    val pingMs: Int? = null,
    val jitterMs: Float? = null,
    val packetLossPercent: Int = 0,
    val targetHost: String = "Google DNS (8.8.8.8)",
    val rating: String = "Belum diuji",
    val ratingColor: Color = Color.Gray,
    val timestamp: Long = 0L
) {
    companion object {
        fun evaluateRating(ping: Int, loss: Int): Pair<String, Color> {
            return when {
                loss > 20 -> Pair("Koneksi Buruk (Banyak Loss)", Color(0xFFFF5252))
                ping < 35 -> Pair("Sangat Cepat & Responsif", Color(0xFF00C853))
                ping in 35..75 -> Pair("Bagus & Stabil", Color(0xFF00B0FF))
                ping in 76..150 -> Pair("Cukup (Sedikit Lag)", Color(0xFFFFB300))
                else -> Pair("Tinggi / Lambat", Color(0xFFFF5252))
            }
        }
    }
}
