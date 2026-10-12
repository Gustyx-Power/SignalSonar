package id.gustyx.signalsonar.network

import id.gustyx.signalsonar.data.model.LatencyResult
import id.gustyx.signalsonar.data.model.LatencyStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.abs

/**
 * Performs TCP socket latency, jitter, and packet loss measurements
 * against high-availability DNS root nodes (Google 8.8.8.8 / Cloudflare 1.1.1.1).
 */
class LatencyTester {

    suspend fun testLatency(
        host: String = "8.8.8.8",
        port: Int = 53,
        probesCount: Int = 4,
        timeoutMs: Int = 2000
    ): LatencyResult = withContext(Dispatchers.IO) {
        val rtts = mutableListOf<Long>()
        var failedCount = 0

        for (i in 0 until probesCount) {
            val startTime = System.currentTimeMillis()
            var socket: Socket? = null
            try {
                socket = Socket()
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                val elapsed = System.currentTimeMillis() - startTime
                rtts.add(elapsed)
            } catch (_: Exception) {
                failedCount++
            } finally {
                try {
                    socket?.close()
                } catch (_: Exception) {}
            }
        }

        val lossPercent = ((failedCount.toFloat() / probesCount.toFloat()) * 100).toInt()

        if (rtts.isEmpty()) {
            return@withContext LatencyResult(
                status = LatencyStatus.FAILED,
                pingMs = null,
                jitterMs = null,
                packetLossPercent = 100,
                targetHost = if (host == "8.8.8.8") "Google DNS (8.8.8.8)" else host,
                rating = "Tidak Terhubung (Timeout)",
                timestamp = System.currentTimeMillis()
            )
        }

        val avgPing = rtts.average().toInt()

        // Calculate Jitter (average difference between consecutive RTTs)
        val jitter = if (rtts.size > 1) {
            var diffSum = 0L
            for (j in 1 until rtts.size) {
                diffSum += abs(rtts[j] - rtts[j - 1])
            }
            diffSum.toFloat() / (rtts.size - 1)
        } else {
            0f
        }

        val (ratingText, ratingColor) = LatencyResult.evaluateRating(avgPing, lossPercent)

        LatencyResult(
            status = LatencyStatus.SUCCESS,
            pingMs = avgPing,
            jitterMs = jitter,
            packetLossPercent = lossPercent,
            targetHost = if (host == "8.8.8.8") "Google DNS (8.8.8.8)" else if (host == "1.1.1.1") "Cloudflare (1.1.1.1)" else host,
            rating = ratingText,
            ratingColor = ratingColor,
            timestamp = System.currentTimeMillis()
        )
    }
}
