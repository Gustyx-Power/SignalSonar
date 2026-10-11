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
                    "Jalur sinyal sangat terbuka (Line-of-Sight). Tidak ada halangan fisik yang meredam penerimaan sinyal di HP."
                ObstacleSeverity.LIGHT_OBSTACLE ->
                    "Teredam ringan oleh partisi tipis (kaca jendela, kayu, atau kanopi). Penerimaan sinyal di HP tetap stabil."
                ObstacleSeverity.MODERATE_OBSTACLE ->
                    "Sinyal mengalami atenuasi oleh tembok bata atau sekat ruangan. Daya sinyal yang diterima HP melemah."
                ObstacleSeverity.HEAVY_OBSTACLE ->
                    "Terblokir struktur masif (beton bertulang, dinding ganda, atau kontur gedung). Sinyal di HP teredam kuat."
                ObstacleSeverity.UNKNOWN ->
                    "Menunggu pembacaan sensor radio HP..."
            }

            val interferenceDesc = when (interference) {
                RfInterferenceLevel.CLEAN ->
                    "Rasio sinyal terhadap derau (SINR) tinggi. Saluran frekuensi sangat bersih untuk transfer data HP."
                RfInterferenceLevel.MODERATE ->
                    "Tingkat noise wajar untuk lingkungan sekitar. Kecepatan data HP tetap dalam batas optimal."
                RfInterferenceLevel.HIGH ->
                    "Terdeteksi interferensi gelombang antarsel atau elektromagnetik di sekitar posisi HP."
                RfInterferenceLevel.CRITICAL ->
                    "Derau gelombang tinggi melebihi daya sinyal. HP rentan mengalami latensi tinggi dan paket data hilang."
                RfInterferenceLevel.UNKNOWN ->
                    "Mengukur kualitas modulasi saluran..."
            }

            // 4. Generate Targeted Recommendations for Smartphone Users
            val recs = mutableListOf<String>()
            when (severity) {
                ObstacleSeverity.CLEAR_LOS -> {
                    recs.add("Posisi HP di titik ini sangat optimal untuk internet cepat dan panggilan lancar.")
                    recs.add("Pertahankan posisi HP di area ini jika sedang mengunduh atau bermain game.")
                }
                ObstacleSeverity.LIGHT_OBSTACLE -> {
                    recs.add("Bawa HP sedikit lebih dekat ke jendela atau pintu yang menghadap area luar.")
                    recs.add("Pegang HP secara wajar agar antena internal tidak tertutup telapak tangan.")
                }
                ObstacleSeverity.MODERATE_OBSTACLE -> {
                    recs.add("Pindahkan posisi HP ke dekat bukaan ruangan atau balkon untuk menghindari redaman tembok.")
                    recs.add("Coba berpindah ke lantai atas atau ruangan dengan jendela terbuka.")
                }
                ObstacleSeverity.HEAVY_OBSTACLE -> {
                    recs.add("Bawa HP ke area luar ruangan (outdoor) atau mendekat ke jendela utama.")
                    recs.add("Hindari menggunakan HP di sudut ruangan beton tebal atau basement tertutup.")
                }
                ObstacleSeverity.UNKNOWN -> {}
            }

            if (interference == RfInterferenceLevel.HIGH || interference == RfInterferenceLevel.CRITICAL) {
                recs.add("Jauhkan HP dari sumber radiasi listrik seperti microwave, adaptor daya besar, atau motor listrik.")
            }

            if (distanceMeters != null && distanceMeters > 1500) {
                recs.add("Estimasi jarak HP ke BTS sekitar ${distanceMeters}m; posisikan HP menghadap area terbuka.")
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
