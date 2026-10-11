package id.gustyx.signalsonar.audio

/**
 * Calculates acoustic sonar feedback timing and tone type based on cellular signal strength (dBm).
 * Follows the Geiger-counter / radar metaphor where stronger signals produce faster ping rates.
 */
object SignalIntervalMapper {

    const val MIN_INTERVAL_MS = 120L
    const val MAX_INTERVAL_MS = 1500L
    const val DEFAULT_IDLE_INTERVAL_MS = 2000L

    /**
     * Determines interval delay in milliseconds for the sonar beep timer.
     */
    fun calculateIntervalMs(dbm: Int?): Long {
        if (dbm == null || dbm < -140 || dbm > -40) {
            return DEFAULT_IDLE_INTERVAL_MS
        }

        return when {
            // Sangat Bagus (>= -75 dBm): Beep sangat cepat (~120ms - 200ms)
            dbm >= -75 -> {
                // Smooth interpolation: -50 dBm -> 120ms, -75 dBm -> 200ms
                val clamped = dbm.coerceIn(-75, -50)
                val fraction = (clamped - (-75)) / 25f
                (200L - (fraction * 80L)).toLong().coerceIn(MIN_INTERVAL_MS, 200L)
            }
            // Bagus (-76 dBm s/d -90 dBm): Beep menengah (~400ms)
            dbm in -90..-76 -> {
                val clamped = dbm.coerceIn(-90, -76)
                val fraction = (clamped - (-90)) / 14f
                (550L - (fraction * 250L)).toLong().coerceIn(300L, 550L)
            }
            // Buruk (-91 dBm s/d -105 dBm): Beep lambat (~800ms)
            dbm in -105..-91 -> {
                val clamped = dbm.coerceIn(-105, -91)
                val fraction = (clamped - (-105)) / 14f
                (1050L - (fraction * 350L)).toLong().coerceIn(700L, 1050L)
            }
            // Sangat Buruk / Hilang (<= -106 dBm): Beep sangat lambat (~1500ms)
            else -> {
                MAX_INTERVAL_MS
            }
        }
    }

    /**
     * Identifies whether to trigger an error/flat warning tone instead of a crisp ping
     * when the cellular signal is extremely degraded or disconnected.
     */
    fun shouldUseFlatTone(dbm: Int?): Boolean {
        return dbm == null || dbm <= -115
    }
}
