package id.gustyx.signalsonar.data.model

import androidx.compose.ui.graphics.Color

/**
 * Categorization of physical obstacles and radio wave attenuation severity.
 */
enum class ObstacleSeverity(
    val label: String,
    val shortBadge: String,
    val color: Color,
    val estimatedLossRange: String
) {
    CLEAR_LOS(
        label = "LOS Terbuka (Bebas Halangan)",
        shortBadge = "LOS Optimal",
        color = Color(0xFF00E676),
        estimatedLossRange = "0 - 4 dB"
    ),
    LIGHT_OBSTACLE(
        label = "Halangan Ringan (Kaca / Kayu / Kanopi)",
        shortBadge = "Atenuasi Ringan",
        color = Color(0xFF00E5FF),
        estimatedLossRange = "5 - 10 dB"
    ),
    MODERATE_OBSTACLE(
        label = "Halangan Sedang (Tembok Bata / Partisi)",
        shortBadge = "Atenuasi Sedang",
        color = Color(0xFFFFB300),
        estimatedLossRange = "11 - 20 dB"
    ),
    HEAVY_OBSTACLE(
        label = "Halangan Berat (Beton / Atap Seng / Gedung)",
        shortBadge = "Terblokir Kuat",
        color = Color(0xFFFF5252),
        estimatedLossRange = "> 20 dB"
    ),
    UNKNOWN(
        label = "Menghitung Estimasi Halangan...",
        shortBadge = "Menganalisis",
        color = Color(0xFF90A4AE),
        estimatedLossRange = "--"
    )
}

/**
 * Categorization of radio frequency interference and signal purity.
 */
enum class RfInterferenceLevel(
    val label: String,
    val shortBadge: String,
    val color: Color
) {
    CLEAN(
        label = "Sangat Bersih (Noise Minimal)",
        shortBadge = "Bersih",
        color = Color(0xFF00E676)
    ),
    MODERATE(
        label = "Interferensi Normal (Standar Pemukiman)",
        shortBadge = "Normal",
        color = Color(0xFF00E5FF)
    ),
    HIGH(
        label = "Interferensi Tinggi (Crowded Tower)",
        shortBadge = "Tinggi",
        color = Color(0xFFFFB300)
    ),
    CRITICAL(
        label = "Interferensi Kritis (Tabrakan Frekuensi)",
        shortBadge = "Kritis",
        color = Color(0xFFFF5252)
    ),
    UNKNOWN(
        label = "Mengukur Rasio Noise...",
        shortBadge = "Menganalisis",
        color = Color(0xFF90A4AE)
    )
}

/**
 * Diagnostic results for cellular environmental propagation, obstacles, and RF interference.
 */
data class ObstacleAnalysis(
    val obstacleSeverity: ObstacleSeverity = ObstacleSeverity.UNKNOWN,
    val interferenceLevel: RfInterferenceLevel = RfInterferenceLevel.UNKNOWN,
    val estimatedLossDb: Int = 0,
    val obstacleDescription: String = "Mengumpulkan telemetri gelombang mikro radio...",
    val interferenceDescription: String = "Mengukur rasio sinyal terhadap derau (SINR/RSRQ)...",
    val recommendations: List<String> = emptyList(),
    val distanceEstimateMeters: Int? = null
) {
    companion object {
        fun analyze(
            dbm: Int?,
            rsrq: Int?,
            sinr: Int?,
            timingAdvance: Int?
        ): ObstacleAnalysis {
            if (dbm == null || dbm < -140 || dbm > -40) {
                return ObstacleAnalysis()
            }

            // Estimate distance to cell tower via Timing Advance (1 TA unit ≈ 78.12 m in LTE)
            val distanceMeters = if (timingAdvance != null && timingAdvance in 0..1282) {
                timingAdvance * 78
            } else {
                null
            }

            // 1. Analyze RF Interference Level (SINR or RSRQ)
            val interference = when {
                sinr != null -> when {
                    sinr >= 13 -> RfInterferenceLevel.CLEAN
                    sinr in 6..12 -> RfInterferenceLevel.MODERATE
                    sinr in 0..5 -> RfInterferenceLevel.HIGH
                    else -> RfInterferenceLevel.CRITICAL
                }
                rsrq != null -> when {
                    rsrq >= -9 -> RfInterferenceLevel.CLEAN
                    rsrq in -13..-10 -> RfInterferenceLevel.MODERATE
                    rsrq in -17..-14 -> RfInterferenceLevel.HIGH
                    else -> RfInterferenceLevel.CRITICAL
                }
                else -> when {
                    dbm >= -80 -> RfInterferenceLevel.CLEAN
                    dbm in -95..-81 -> RfInterferenceLevel.MODERATE
                    dbm in -105..-96 -> RfInterferenceLevel.HIGH
                    else -> RfInterferenceLevel.CRITICAL
                }
            }

            // 2. Analyze Obstacle Severity & Penetration Loss
            val (severity, lossDb) = when {
                dbm >= -80 && (sinr == null || sinr >= 10) -> {
                    Pair(ObstacleSeverity.CLEAR_LOS, 3)
                }
                dbm in -94..-81 && (rsrq == null || rsrq >= -12) -> {
                    Pair(ObstacleSeverity.LIGHT_OBSTACLE, 8)
                }
                dbm in -106..-95 || (rsrq != null && rsrq in -17..-13) -> {
                    Pair(ObstacleSeverity.MODERATE_OBSTACLE, 16)
                }
                else -> {
                    Pair(ObstacleSeverity.HEAVY_OBSTACLE, 24)
                }
            }

            // 3. Generate Contextual Descriptions
            val obstacleDesc = when (severity) {
                ObstacleSeverity.CLEAR_LOS ->
                    "Jalur sinyal sangat terbuka (Line-of-Sight). Tidak ada halangan fisik struktural yang meredam gelombang antena."
                ObstacleSeverity.LIGHT_OBSTACLE ->
                    "Teredam ringan oleh partisi tipis (kaca jendela, kanopi, atau daun pohon). Gelombang masih menembus dengan stabil."
                ObstacleSeverity.MODERATE_OBSTACLE ->
                    "Sinyal mengalami atenuasi oleh tembok bata atau atap bangunan. Menimbulkan pantulan multipath yang memperlemah daya."
                ObstacleSeverity.HEAVY_OBSTACLE ->
                    "Terblokir struktur masif (beton bertulang, dinding tebal ganda, atau kontur tanah). Sinyal mengalami redaman berat."
                ObstacleSeverity.UNKNOWN ->
                    "Menunggu pembacaan sensor radio..."
            }

            val interferenceDesc = when (interference) {
                RfInterferenceLevel.CLEAN ->
                    "Rasio sinyal terhadap derau (SINR) sangat tinggi. Frekuensi bersih dari tabrakan menara tetangga."
                RfInterferenceLevel.MODERATE ->
                    "Tingkat noise wajar untuk area hunian padat. Latensi dan throughput tetap dalam batas optimal."
                RfInterferenceLevel.HIGH ->
                    "Terdeteksi interferensi gelombang antarsel atau radiasi elektromagnetik di sekitar perangkat."
                RfInterferenceLevel.CRITICAL ->
                    "Noise gelombang melebihi kekuatan sinyal data. Rentan terjadi packet loss dan jitter tinggi."
                RfInterferenceLevel.UNKNOWN ->
                    "Mengukur kualitas modulasi saluran..."
            }

            // 4. Generate Targeted Recommendations
            val recs = mutableListOf<String>()
            when (severity) {
                ObstacleSeverity.CLEAR_LOS -> {
                    recs.add("Posisi saat ini sangat optimal. Kunci posisi antena/modem di titik ini.")
                    recs.add("Pastikan kabel antena tidak terlipat tajam untuk mempertahankan throughput.")
                }
                ObstacleSeverity.LIGHT_OBSTACLE -> {
                    recs.add("Dekatkan perangkat ke jendela kaca yang menghadap ke luar bangunan.")
                    recs.add("Arahkan sisi depan modem menghadap ke ruang terbuka tanpa terhalang lemari.")
                }
                ObstacleSeverity.MODERATE_OBSTACLE -> {
                    recs.add("Tinggikan antena minimal 1–2 meter untuk melompati pembatas atap bangunan tetangga.")
                    recs.add("Hindari menaruh modem di balik tembok bata tebal atau sudut mati ruangan.")
                }
                ObstacleSeverity.HEAVY_OBSTACLE -> {
                    recs.add("Pindahkan perangkat ke luar ruangan atau gunakan antena eksternal outdoor (Yagi / Grid / LPDA).")
                    recs.add("Pasang antena setinggi mungkin di atas bubungan atap rumah.")
                }
                ObstacleSeverity.UNKNOWN -> {}
            }

            if (interference == RfInterferenceLevel.HIGH || interference == RfInterferenceLevel.CRITICAL) {
                recs.add("Jauhkan perangkat dari sumber noise RF: router Wi-Fi 2.4GHz lain, microwave, atau charger laptop non-orisinal.")
            }

            if (distanceMeters != null && distanceMeters > 1500) {
                recs.add("Estimasi jarak ke BTS sekitar ${distanceMeters}m; gunakan antena directional terarah.")
            }

            return ObstacleAnalysis(
                obstacleSeverity = severity,
                interferenceLevel = interference,
                estimatedLossDb = lossDb,
                obstacleDescription = obstacleDesc,
                interferenceDescription = interferenceDesc,
                recommendations = recs,
                distanceEstimateMeters = distanceMeters
            )
        }
    }
}
