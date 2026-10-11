package id.gustyx.signalsonar

import id.gustyx.signalsonar.audio.SignalIntervalMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalIntervalMapperTest {

    @Test
    fun testExcellentSignalIntervals() {
        // >= -75 dBm should produce fast beep interval (~120ms - 200ms)
        val interval70 = SignalIntervalMapper.calculateIntervalMs(-70)
        val interval75 = SignalIntervalMapper.calculateIntervalMs(-75)
        val interval50 = SignalIntervalMapper.calculateIntervalMs(-50)

        assertTrue("Interval at -70 dBm should be <= 200ms", interval70 <= 200L)
        assertTrue("Interval at -75 dBm should be <= 200ms", interval75 <= 200L)
        assertTrue("Interval at -50 dBm should be >= 120ms", interval50 >= 120L)
    }

    @Test
    fun testGoodSignalIntervals() {
        // -76 to -90 dBm should produce medium interval around 400ms
        val interval80 = SignalIntervalMapper.calculateIntervalMs(-80)
        val interval85 = SignalIntervalMapper.calculateIntervalMs(-85)

        assertTrue("Interval at -80 dBm should be between 300ms and 550ms", interval80 in 300L..550L)
        assertTrue("Interval at -85 dBm should be between 300ms and 550ms", interval85 in 300L..550L)
    }

    @Test
    fun testPoorSignalIntervals() {
        // -91 to -105 dBm should produce slow interval around 800ms
        val interval95 = SignalIntervalMapper.calculateIntervalMs(-95)
        val interval100 = SignalIntervalMapper.calculateIntervalMs(-100)

        assertTrue("Interval at -95 dBm should be between 700ms and 1050ms", interval95 in 700L..1050L)
        assertTrue("Interval at -100 dBm should be between 700ms and 1050ms", interval100 in 700L..1050L)
    }

    @Test
    fun testCriticalSignalIntervals() {
        // <= -106 dBm should produce maximum slow interval (1500ms)
        val interval110 = SignalIntervalMapper.calculateIntervalMs(-110)
        val interval125 = SignalIntervalMapper.calculateIntervalMs(-125)

        assertEquals(1500L, interval110)
        assertEquals(1500L, interval125)
    }

    @Test
    fun testFlatToneDetection() {
        // Critical or missing signal should trigger flat warning tone
        assertTrue(SignalIntervalMapper.shouldUseFlatTone(null))
        assertTrue(SignalIntervalMapper.shouldUseFlatTone(-116))
        assertTrue(SignalIntervalMapper.shouldUseFlatTone(-125))

        // Normal/good signal should NOT trigger flat tone
        assertFalse(SignalIntervalMapper.shouldUseFlatTone(-75))
        assertFalse(SignalIntervalMapper.shouldUseFlatTone(-85))
        assertFalse(SignalIntervalMapper.shouldUseFlatTone(-100))
    }
}
