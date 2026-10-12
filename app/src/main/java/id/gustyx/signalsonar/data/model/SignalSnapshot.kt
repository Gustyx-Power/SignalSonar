package id.gustyx.signalsonar.data.model

import kotlin.math.roundToInt

/**
 * Historical snapshot of cellular RF measurements.
 * Captures raw power (dBm / RSRP), interference (RSRQ), noise ratio (SINR),
 * and the composite RF quality score (0..100%).
 */
data class SignalSnapshot(
    val dbm: Int? = null,
    val rsrp: Int? = null,
    val rsrq: Int? = null,
    val sinr: Int? = null,
    val compositeScore: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Calculates composite RF quality score (0..100%) taking into account:
         * 1. RSRP / Power (dBm): ~35% weight (-125 dBm to -70 dBm)
         * 2. RSRQ / Quality (dB): ~35% weight (-20 dB to -5 dB)
         * 3. SINR / Noise ratio (dB): ~30% weight (-5 dB to +25 dB)
         * Gracefully re-weights if RSRQ or SINR is not reported by the device modem.
         */
        fun calculateCompositeScore(
            dbm: Int?,
            rsrp: Int?,
            rsrq: Int?,
            sinr: Int?
        ): Int {
            val powerDbm = rsrp ?: dbm
            if (powerDbm == null || powerDbm !in -140..-40) return 0

            // 1. Power Score (RSRP / dBm): -125 dBm (0%) to -70 dBm (100%)
            val powerScore = ((powerDbm.toFloat() - (-125f)) / (-70f - (-125f)) * 100f).coerceIn(0f, 100f)

            // 2. RSRQ Score: -20 dB (0%) to -5 dB (100%)
            val rsrqScore = rsrq?.let {
                ((it.toFloat() - (-20f)) / (-5f - (-20f)) * 100f).coerceIn(0f, 100f)
            }

            // 3. SINR Score: -5 dB (0%) to +25 dB (100%)
            val sinrScore = sinr?.let {
                ((it.toFloat() - (-5f)) / (25f - (-5f)) * 100f).coerceIn(0f, 100f)
            }

            return when {
                rsrqScore != null && sinrScore != null -> {
                    (powerScore * 0.35f + rsrqScore * 0.35f + sinrScore * 0.30f).roundToInt()
                }
                rsrqScore != null -> {
                    (powerScore * 0.50f + rsrqScore * 0.50f).roundToInt()
                }
                sinrScore != null -> {
                    (powerScore * 0.55f + sinrScore * 0.45f).roundToInt()
                }
                else -> {
                    powerScore.roundToInt()
                }
            }.coerceIn(0, 100)
        }

        fun fromSignalData(signal: SignalData): SignalSnapshot {
            return SignalSnapshot(
                dbm = signal.dbm,
                rsrp = signal.rsrp,
                rsrq = signal.rsrq,
                sinr = signal.sinr,
                compositeScore = calculateCompositeScore(
                    dbm = signal.dbm,
                    rsrp = signal.rsrp,
                    rsrq = signal.rsrq,
                    sinr = signal.sinr
                ),
                timestamp = signal.timestamp
            )
        }
    }
}
