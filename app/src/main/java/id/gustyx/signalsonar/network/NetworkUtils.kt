package id.gustyx.signalsonar.network

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface

/**
 * Utility for retrieving cellular interface IP addresses (IPv4 & IPv6).
 */
object NetworkUtils {
    fun getCellularIpAddresses(): Pair<String?, String?> {
        var ipv4: String? = null
        var ipv6: String? = null

        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return Pair(null, null)
            for (iface in interfaces) {
                val addresses = iface.inetAddresses
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
                        if (addr is Inet4Address && ipv4 == null) {
                            ipv4 = addr.hostAddress
                        } else if (addr is Inet6Address && ipv6 == null) {
                            ipv6 = addr.hostAddress?.substringBefore('%')
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback gracefully on restricted environments
        }
        return Pair(ipv4, ipv6)
    }
}
