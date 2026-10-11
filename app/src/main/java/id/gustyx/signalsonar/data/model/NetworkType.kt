package id.gustyx.signalsonar.data.model

/**
 * Representation of cellular network generation and technology.
 */
enum class NetworkType(val displayName: String, val shortBadge: String) {
    NR_5G("5G NR", "5G"),
    LTE("4G LTE", "4G"),
    HSPA_3G("3G HSPA", "3G"),
    GSM_2G("2G GSM/EDGE", "2G"),
    UNKNOWN("Mencari...", "--");

    companion object {
        fun fromTelephonyType(networkType: Int, overrideType: Int = 0): NetworkType {
            // Check 5G NSA override first if available
            if (overrideType == 2 || overrideType == 3) { // NR_NSA / NR_ADVANCED
                return NR_5G
            }

            return when (networkType) {
                // 5G NR
                20 -> NR_5G // TelephonyManager.NETWORK_TYPE_NR

                // 4G LTE
                13, // NETWORK_TYPE_LTE
                19 -> LTE // NETWORK_TYPE_LTE_CA

                // 3G UMTS / HSPA
                3, // NETWORK_TYPE_UMTS
                8, // NETWORK_TYPE_HSDPA
                9, // NETWORK_TYPE_HSUPA
                10, // NETWORK_TYPE_HSPA
                15 -> HSPA_3G // NETWORK_TYPE_HSPAP

                // 2G GSM / EDGE / CDMA
                1, // NETWORK_TYPE_GPRS
                2, // NETWORK_TYPE_EDGE
                4, // NETWORK_TYPE_CDMA
                7, // NETWORK_TYPE_1xRTT
                16 -> GSM_2G // NETWORK_TYPE_GSM

                else -> UNKNOWN
            }
        }
    }
}
