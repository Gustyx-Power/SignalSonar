package id.gustyx.signalsonar

import id.gustyx.signalsonar.data.model.CellTowerInfo
import id.gustyx.signalsonar.data.model.CoverageZone
import id.gustyx.signalsonar.data.model.NeighborCell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CellTowerInfoTest {

    @Test
    fun testEnodeBAndSectorDecomposition() {
        // Example LTE CI = 105703169 (0x064CEB01)
        val rawCi = 105703169L
        val enodebId = (rawCi shr 8).toInt() // 412903
        val sectorId = (rawCi and 0xFF).toInt() // 1

        assertEquals(412903, enodebId)
        assertEquals(1, sectorId)
    }

    @Test
    fun testCoverageRadiusAndDistanceFormatting() {
        val nearTower = CellTowerInfo(
            enodebId = 50012,
            sectorId = 2,
            distanceMeters = 312,
            isDistancePrecise = true,
            estimatedMaxCoverageMeters = 3500,
            coveragePercentage = 312f / 3500f,
            coverageZone = CoverageZone.NEAR_TOWER
        )

        assertEquals("312 meter", nearTower.formattedDistance)
        assertEquals("3.5 km", nearTower.formattedMaxCoverage)
        assertEquals(CoverageZone.NEAR_TOWER, nearTower.coverageZone)
        assertEquals("Timing Advance (Presisi Radio)", nearTower.distanceCalculationMethod)

        val farTower = CellTowerInfo(
            enodebId = 50012,
            sectorId = 2,
            distanceMeters = 2400,
            isDistancePrecise = false,
            estimatedMaxCoverageMeters = 3000,
            coveragePercentage = 2400f / 3000f,
            coverageZone = CoverageZone.CELL_EDGE
        )

        assertEquals("2.40 km", farTower.formattedDistance)
        assertEquals(CoverageZone.CELL_EDGE, farTower.coverageZone)
        assertEquals("Estimasi Redaman Sinyal (Path Loss)", farTower.distanceCalculationMethod)
    }

    @Test
    fun testNeighborCellFormatting() {
        val neighbor = NeighborCell(pci = 142, dbm = -95, networkType = "LTE")
        assertEquals("-95 dBm", neighbor.formattedDbm)
    }

    @Test
    fun testLatencyRatingEvaluation() {
        val (ratingFast, _) = id.gustyx.signalsonar.data.model.LatencyResult.evaluateRating(25, 0)
        assertEquals("Sangat Cepat & Responsif", ratingFast)

        val (ratingGood, _) = id.gustyx.signalsonar.data.model.LatencyResult.evaluateRating(50, 0)
        assertEquals("Bagus & Stabil", ratingGood)

        val (ratingLoss, _) = id.gustyx.signalsonar.data.model.LatencyResult.evaluateRating(40, 25)
        assertEquals("Koneksi Buruk (Banyak Loss)", ratingLoss)
    }

    @Test
    fun testSimCardInfoModel() {
        val sim = id.gustyx.signalsonar.data.model.SimCardInfo(
            subscriptionId = 1,
            slotIndex = 0,
            displayName = "Telkomsel",
            carrierName = "Telkomsel",
            isDefaultData = true,
            countryIso = "ID"
        )
        assertEquals("SIM 1: Telkomsel", sim.shortLabel)
        assertTrue(sim.isDefaultData)
    }
}
