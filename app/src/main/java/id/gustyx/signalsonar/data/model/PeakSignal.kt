package id.gustyx.signalsonar.data.model

/**
 * Encapsulates the peak (strongest) cellular signal recorded during the current sonar session.
 * In dBm, values closer to 0 are stronger (e.g., -65 dBm is stronger than -85 dBm).
 */
data class PeakSignal(
    val dbm: Int,
    val networkType: NetworkType,
    val timestamp: Long = System.currentTimeMillis()
) {
    val quality: SignalQuality
        get() = SignalQuality.fromDbm(dbm)
}
