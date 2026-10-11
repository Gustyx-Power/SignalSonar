package id.gustyx.signalsonar.data.model

/**
 * Real-time cellular signal snapshot from the primary data SIM.
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
    val timestamp: Long = System.currentTimeMillis()
) {
    val quality: SignalQuality
        get() = SignalQuality.fromDbm(dbm)

    val formattedDbm: String
        get() = if (dbm != null && dbm in -140..-40) "$dbm dBm" else "-- dBm"

    val isSignalValid: Boolean
        get() = dbm != null && dbm in -140..-40
}
