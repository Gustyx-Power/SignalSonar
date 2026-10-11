package id.gustyx.signalsonar.data.model

import java.util.Locale

/**
 * Real-time network throughput and data traffic telemetry.
 */
data class TrafficData(
    val rxBytesPerSec: Long = 0L,
    val txBytesPerSec: Long = 0L,
    val sessionTotalRxBytes: Long = 0L,
    val sessionTotalTxBytes: Long = 0L
) {
    val formattedRxSpeed: String
        get() = formatSpeed(rxBytesPerSec)

    val formattedTxSpeed: String
        get() = formatSpeed(txBytesPerSec)

    val formattedTotalRx: String
        get() = formatBytes(sessionTotalRxBytes)

    val formattedTotalTx: String
        get() = formatBytes(sessionTotalTxBytes)

    companion object {
        fun formatSpeed(bytesPerSec: Long): String {
            val bitsPerSec = bytesPerSec * 8.0
            return when {
                bitsPerSec >= 1_000_000_000.0 -> String.format(Locale.US, "%.1f Gbps", bitsPerSec / 1_000_000_000.0)
                bitsPerSec >= 1_000_000.0 -> String.format(Locale.US, "%.1f Mbps", bitsPerSec / 1_000_000.0)
                bitsPerSec >= 1_000.0 -> String.format(Locale.US, "%.0f Kbps", bitsPerSec / 1_000.0)
                else -> String.format(Locale.US, "%.0f bps", bitsPerSec)
            }
        }

        fun formatBytes(bytes: Long): String {
            return when {
                bytes >= 1_073_741_824L -> String.format(Locale.US, "%.2f GB", bytes / 1_073_741_824.0)
                bytes >= 1_048_576L -> String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)
                bytes >= 1024L -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
                else -> "$bytes B"
            }
        }
    }
}
