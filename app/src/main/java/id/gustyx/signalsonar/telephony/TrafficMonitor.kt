package id.gustyx.signalsonar.telephony

import android.net.TrafficStats
import id.gustyx.signalsonar.data.model.TrafficData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Monitors live network throughput (Rx/Tx) and data transferred during the session.
 */
class TrafficMonitor {

    fun observeTraffic(sampleIntervalMs: Long = 1000L): Flow<TrafficData> = flow {
        var lastRx = getRxBytes()
        var lastTx = getTxBytes()
        val initialRx = lastRx
        val initialTx = lastTx

        var sessionTotalRx = 0L
        var sessionTotalTx = 0L

        while (true) {
            delay(sampleIntervalMs)
            val currentRx = getRxBytes()
            val currentTx = getTxBytes()

            val deltaRx = if (currentRx >= lastRx) currentRx - lastRx else 0L
            val deltaTx = if (currentTx >= lastTx) currentTx - lastTx else 0L

            sessionTotalRx = if (currentRx >= initialRx) currentRx - initialRx else sessionTotalRx + deltaRx
            sessionTotalTx = if (currentTx >= initialTx) currentTx - initialTx else sessionTotalTx + deltaTx

            lastRx = currentRx
            lastTx = currentTx

            emit(
                TrafficData(
                    rxBytesPerSec = deltaRx,
                    txBytesPerSec = deltaTx,
                    sessionTotalRxBytes = sessionTotalRx,
                    sessionTotalTxBytes = sessionTotalTx
                )
            )
        }
    }

    private fun getRxBytes(): Long {
        val mobile = TrafficStats.getMobileRxBytes()
        return if (mobile != TrafficStats.UNSUPPORTED.toLong()) mobile else TrafficStats.getTotalRxBytes()
    }

    private fun getTxBytes(): Long {
        val mobile = TrafficStats.getMobileTxBytes()
        return if (mobile != TrafficStats.UNSUPPORTED.toLong()) mobile else TrafficStats.getTotalTxBytes()
    }
}
