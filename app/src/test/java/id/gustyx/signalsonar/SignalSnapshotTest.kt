package id.gustyx.signalsonar

import id.gustyx.signalsonar.audio.SignalIntervalMapper
import id.gustyx.signalsonar.data.model.SignalData
import id.gustyx.signalsonar.data.model.SignalQuality
import id.gustyx.signalsonar.data.model.SignalSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalSnapshotTest {

    @Test
    fun testCompositeScoreWithIdealSignal() {
        // -70 dBm (strong), -6 dB (clean RSRQ), 25 dB (clean SINR)
        val score = SignalSnapshot.calculateCompositeScore(
            dbm = -70,
            rsrp = -70,
            rsrq = -6,
            sinr = 25
        )
        assertTrue("Ideal signal score should be >= 90%, actual: $score", score >= 90)
        assertEquals(SignalQuality.EXCELLENT, SignalQuality.fromCompositeScore(score))
    }

    @Test
    fun testCompositeScoreWithCongestedNearTower() {
        // Strong power (-70 dBm) but bad interference (RSRQ -18 dB, SINR 0 dB)
        val score = SignalSnapshot.calculateCompositeScore(
            dbm = -70,
            rsrp = -70,
            rsrq = -18,
            sinr = 0
        )
        // Score should be degraded despite high dBm power
        assertTrue("Congested signal score should be penalized below 55%, actual: $score", score in 35..55)
        assertEquals(SignalQuality.POOR, SignalQuality.fromCompositeScore(score))
    }

    @Test
    fun testCompositeScoreWithCleanFarSignal() {
        // Moderate/weak power (-100 dBm) but very clean line-of-sight (RSRQ -8 dB, SINR 20 dB)
        val score = SignalSnapshot.calculateCompositeScore(
            dbm = -100,
            rsrp = -100,
            rsrq = -8,
            sinr = 20
        )
        // Score should be boosted into GOOD tier due to clean channel
        assertTrue("Clean channel score should be elevated into GOOD tier (>= 60%), actual: $score", score >= 60)
        assertEquals(SignalQuality.GOOD, SignalQuality.fromCompositeScore(score))
    }

    @Test
    fun testFallbackWhenSinrMissing() {
        // When chipset doesn't report SINR
        val score = SignalSnapshot.calculateCompositeScore(
            dbm = -80,
            rsrp = -80,
            rsrq = -10,
            sinr = null
        )
        assertTrue("Fallback without SINR should compute valid score, actual: $score", score in 60..90)
    }

    @Test
    fun testFallbackWhenOnlyDbmAvailable() {
        // When only raw dBm is available
        val score = SignalSnapshot.calculateCompositeScore(
            dbm = -75,
            rsrp = null,
            rsrq = null,
            sinr = null
        )
        assertTrue("Power-only score should compute valid score, actual: $score", score in 80..100)
    }

    @Test
    fun testSignalIntervalMapperWithCompositeScore() {
        // High score -> fast interval (120ms..200ms)
        val fastInterval = SignalIntervalMapper.calculateIntervalMsFromScore(compositeScore = 95)
        assertTrue("Score 95 should yield <= 200ms interval, actual: $fastInterval", fastInterval <= 200L)

        // Moderate score -> medium interval (300ms..550ms)
        val medInterval = SignalIntervalMapper.calculateIntervalMsFromScore(compositeScore = 65)
        assertTrue("Score 65 should yield 300ms..550ms, actual: $medInterval", medInterval in 300L..550L)

        // Low score -> slow interval (700ms..1050ms)
        val slowInterval = SignalIntervalMapper.calculateIntervalMsFromScore(compositeScore = 40)
        assertTrue("Score 40 should yield 700ms..1050ms, actual: $slowInterval", slowInterval in 700L..1050L)

        // Critical score -> 1500ms
        val critInterval = SignalIntervalMapper.calculateIntervalMsFromScore(compositeScore = 15)
        assertEquals(1500L, critInterval)
    }
}
