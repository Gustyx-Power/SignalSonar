package id.gustyx.signalsonar

import id.gustyx.signalsonar.data.model.NetworkType
import id.gustyx.signalsonar.data.model.SignalQuality
import org.junit.Assert.assertEquals
import org.junit.Test

class SignalQualityTest {

    @Test
    fun testSignalQualityThresholds() {
        assertEquals(SignalQuality.EXCELLENT, SignalQuality.fromDbm(-65))
        assertEquals(SignalQuality.EXCELLENT, SignalQuality.fromDbm(-75))

        assertEquals(SignalQuality.GOOD, SignalQuality.fromDbm(-76))
        assertEquals(SignalQuality.GOOD, SignalQuality.fromDbm(-85))
        assertEquals(SignalQuality.GOOD, SignalQuality.fromDbm(-90))

        assertEquals(SignalQuality.POOR, SignalQuality.fromDbm(-91))
        assertEquals(SignalQuality.POOR, SignalQuality.fromDbm(-100))
        assertEquals(SignalQuality.POOR, SignalQuality.fromDbm(-105))

        assertEquals(SignalQuality.CRITICAL, SignalQuality.fromDbm(-106))
        assertEquals(SignalQuality.CRITICAL, SignalQuality.fromDbm(-120))

        assertEquals(SignalQuality.UNKNOWN, SignalQuality.fromDbm(null))
        assertEquals(SignalQuality.UNKNOWN, SignalQuality.fromDbm(0))
        assertEquals(SignalQuality.UNKNOWN, SignalQuality.fromDbm(-150))
    }

    @Test
    fun testNetworkTypeResolution() {
        // NR (5G)
        assertEquals(NetworkType.NR_5G, NetworkType.fromTelephonyType(20))
        // 5G NSA override
        assertEquals(NetworkType.NR_5G, NetworkType.fromTelephonyType(13, overrideType = 2))

        // LTE (4G)
        assertEquals(NetworkType.LTE, NetworkType.fromTelephonyType(13, overrideType = 0))
        assertEquals(NetworkType.LTE, NetworkType.fromTelephonyType(19, overrideType = 0))

        // 3G
        assertEquals(NetworkType.HSPA_3G, NetworkType.fromTelephonyType(3))
        assertEquals(NetworkType.HSPA_3G, NetworkType.fromTelephonyType(10))

        // 2G
        assertEquals(NetworkType.GSM_2G, NetworkType.fromTelephonyType(16))
    }
}
