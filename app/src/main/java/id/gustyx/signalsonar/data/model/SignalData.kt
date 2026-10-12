package id.gustyx.signalsonar.data.model

/**
 * Comprehensive real-time cellular signal and RF engineering snapshot.
 */
data class SignalData(
    val dbm: Int? = null,
    val asu: Int? = null,
    val level: Int = 0, // 0 to 4 bars
    val networkType: NetworkType = NetworkType.UNKNOWN,
    val operatorName: String = "Mencari Operator...",
    val simSlotIndex: Int = 0,
    val subscriptionId: Int = -1,
    val isConnected: Boolean = false,

    // Advanced Radio Frequency (RF) Engineering Metrics
    val rsrp: Int? = null,              // Reference Signal Received Power (dBm)
    val rsrq: Int? = null,              // Reference Signal Received Quality (dB)
    val sinr: Int? = null,              // Signal-to-Interference-plus-Noise Ratio (dB)
    val cqi: Int? = null,               // Channel Quality Indicator (1..15)
    val timingAdvance: Int? = null,     // Timing Advance (LTE/NR symbols)
    val estimatedDistanceMeters: Int? = null, // Distance to BTS estimate in meters

    // Cell Identity & Carrier Frequency
    val pci: Int? = null,               // Physical Cell Identity
    val tac: Int? = null,               // Tracking Area Code
    val ci: Long? = null,               // Cell Identity (CID/eNB)
    val band: String? = null,           // E.g., "B3 (1800 MHz)", "B1 (2100 MHz)"
    val earfcn: Int? = null,            // Frequency Channel Number
    val cellTowerInfo: CellTowerInfo = CellTowerInfo(),

    // Advanced Global Carrier & Network Details
    val isCarrierAggregationActive: Boolean = false, // 4G+ LTE-A
    val nrMode: String? = null,                      // "5G NSA", "5G SA", "5G Advanced"
    val duplexMode: String = "FDD",                  // "FDD" or "TDD"
    val channelBandwidthMhz: Float? = null,          // e.g. 20.0 MHz
    val downlinkFreqMhz: Double? = null,             // Exact DL frequency MHz
    val uplinkFreqMhz: Double? = null,               // Exact UL frequency MHz
    val isRoaming: Boolean = false,                  // True if connected via roaming
    val countryIso: String = "",                     // e.g. "ID", "IN", "SA"

    val timestamp: Long = System.currentTimeMillis()
) {
    val cid: Long?
        get() = ci

    val compositeScore: Int
        get() = SignalSnapshot.calculateCompositeScore(dbm, rsrp, rsrq, sinr)

    val quality: SignalQuality
        get() = if (compositeScore > 0) SignalQuality.fromCompositeScore(compositeScore) else SignalQuality.fromDbm(dbm)

    val formattedDbm: String
        get() = if (dbm != null && dbm in -140..-40) "$dbm dBm" else "-- dBm"

    val isSignalValid: Boolean
        get() = dbm != null && dbm in -140..-40

    val formattedSinr: String
        get() = if (sinr != null) "${if (sinr > 0) "+" else ""}$sinr dB" else "-- dB"

    val formattedRsrq: String
        get() = if (rsrq != null) "$rsrq dB" else "-- dB"

    val formattedRsrp: String
        get() = if (rsrp != null) "$rsrp dBm" else formattedDbm
}
