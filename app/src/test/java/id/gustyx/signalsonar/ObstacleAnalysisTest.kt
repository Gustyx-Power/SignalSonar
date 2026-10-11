package id.gustyx.signalsonar

import id.gustyx.signalsonar.data.model.ObstacleAnalysis
import id.gustyx.signalsonar.data.model.ObstacleSeverity
import id.gustyx.signalsonar.data.model.RfInterferenceLevel
import id.gustyx.signalsonar.data.model.TrafficData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ObstacleAnalysisTest {

    @Test
    fun testClearLineOfSightDetection() {
        val analysis = ObstacleAnalysis.analyze(
            dbm = -70,
            rsrq = -8,
            sinr = 18,
            timingAdvance = 4
        )

        assertEquals(ObstacleSeverity.CLEAR_LOS, analysis.obstacleSeverity)
        assertEquals(RfInterferenceLevel.CLEAN, analysis.interferenceLevel)
        assertEquals(312, analysis.distanceEstimateMeters) // 4 * 78 = 312m
        assertTrue(analysis.estimatedLossDb <= 4)
        assertTrue(analysis.recommendations.isNotEmpty())
    }

    @Test
    fun testHeavyObstacleAndCriticalInterference() {
        val analysis = ObstacleAnalysis.analyze(
            dbm = -115,
            rsrq = -19,
            sinr = -4,
            timingAdvance = 25
        )

        assertEquals(ObstacleSeverity.HEAVY_OBSTACLE, analysis.obstacleSeverity)
        assertEquals(RfInterferenceLevel.CRITICAL, analysis.interferenceLevel)
        assertEquals(1950, analysis.distanceEstimateMeters) // 25 * 78 = 1950m
        assertTrue(analysis.estimatedLossDb >= 20)
    }

    @Test
    fun testTrafficDataFormatting() {
        assertEquals("10.0 Mbps", TrafficData.formatSpeed(1_250_000L)) // 1.25 MB/s * 8 = 10 Mbps
        assertEquals("500 Kbps", TrafficData.formatSpeed(62_500L))

        assertEquals("50.0 MB", TrafficData.formatBytes(52_428_800L))
        assertEquals("2.00 GB", TrafficData.formatBytes(2_147_483_648L))
    }
}
