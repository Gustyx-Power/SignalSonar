package id.gustyx.signalsonar.telephony

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.CellSignalStrength
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.PhoneStateListener
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import id.gustyx.signalsonar.data.model.NetworkType
import id.gustyx.signalsonar.data.model.SignalData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Monitors real-time cellular signal strength, network technology, RF engineering metrics,
 * and carrier metadata.
 * Uses TelephonyCallback on API 31+ (Android 12+) and PhoneStateListener fallback on API 29-30.
 * Focuses on the primary Default Data SIM in Dual SIM environments.
 */
class SignalMonitor(private val context: Context) {

    private val tag = "SignalMonitor"

    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager

    @SuppressLint("MissingPermission")
    fun observeSignal(): Flow<SignalData> = callbackFlow {
        var currentOverrideType = 0

        fun getTargetedTelephonyManager(): Pair<TelephonyManager, SubscriptionInfo?> {
            var subInfo: SubscriptionInfo? = null
            var subId = SubscriptionManager.getDefaultDataSubscriptionId()

            if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                try {
                    val activeSubs = subscriptionManager?.activeSubscriptionInfoList
                    if (!activeSubs.isNullOrEmpty()) {
                        subInfo = activeSubs.first()
                        subId = subInfo.subscriptionId
                    }
                } catch (e: SecurityException) {
                    Log.w(tag, "Permission not granted to read subscription info", e)
                }
            } else {
                try {
                    subInfo = subscriptionManager?.getActiveSubscriptionInfo(subId)
                } catch (e: SecurityException) {
                    Log.w(tag, "Permission not granted for subId $subId", e)
                }
            }

            val targetedManager = if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                telephonyManager.createForSubscriptionId(subId)
            } else {
                telephonyManager
            }

            return Pair(targetedManager, subInfo)
        }

        fun mapEarfcnToBand(earfcn: Int): String {
            return when (earfcn) {
                in 0..599 -> "B1 (2100 MHz)"
                in 600..1199 -> "B2 (1900 MHz)"
                in 1200..1949 -> "B3 (1800 MHz)"
                in 1950..2399 -> "B4 (AWS 1700/2100)"
                in 2400..2649 -> "B5 (850 MHz)"
                in 2750..3449 -> "B7 (2600 MHz)"
                in 3450..3799 -> "B8 (900 MHz)"
                in 6150..6449 -> "B20 (800 MHz)"
                in 9210..9659 -> "B28 (700 MHz)"
                in 38650..39649 -> "B40 (TDD 2300 MHz)"
                in 39650..41589 -> "B41 (TDD 2500 MHz)"
                else -> if (earfcn > 0) "EARFCN $earfcn" else "LTE Band Standar"
            }
        }

        fun extractSignalData(
            signalStrength: SignalStrength?,
            overrideType: Int = currentOverrideType
        ): SignalData {
            val (activeManager, subInfo) = getTargetedTelephonyManager()

            val rawNetworkType = try {
                activeManager.dataNetworkType
            } catch (e: SecurityException) {
                activeManager.networkType
            }

            val resolvedNetworkType = NetworkType.fromTelephonyType(rawNetworkType, overrideType)

            val operator = subInfo?.carrierName?.toString()
                ?.takeIf { it.isNotBlank() }
                ?: activeManager.networkOperatorName.takeIf { it.isNotBlank() }
                ?: activeManager.simOperatorName.takeIf { it.isNotBlank() }
                ?: "Operator Tidak Dikenal"

            val simSlot = subInfo?.simSlotIndex ?: 0
            val subId = subInfo?.subscriptionId ?: -1

            if (signalStrength == null) {
                return SignalData(
                    networkType = resolvedNetworkType,
                    operatorName = operator,
                    simSlotIndex = simSlot,
                    subscriptionId = subId,
                    isConnected = false
                )
            }

            val cellStrengths: List<CellSignalStrength> = signalStrength.cellSignalStrengths

            var bestDbm: Int? = null
            var bestLevel = 0
            var bestAsu: Int? = null
            var bestRsrp: Int? = null
            var bestRsrq: Int? = null
            var bestSinr: Int? = null
            var bestCqi: Int? = null
            var bestTimingAdvance: Int? = null

            for (cell in cellStrengths) {
                val dbm = cell.dbm
                if (dbm in -140..-40) {
                    val isPriorityType = cell is CellSignalStrengthNr || cell is CellSignalStrengthLte
                    if (bestDbm == null || (isPriorityType && dbm > bestDbm)) {
                        bestDbm = dbm
                        bestLevel = cell.level
                        bestAsu = cell.asuLevel

                        if (cell is CellSignalStrengthLte) {
                            val rsrp = cell.rsrp
                            if (rsrp != CellInfo.UNAVAILABLE && rsrp in -140..-40) bestRsrp = rsrp
                            val rsrq = cell.rsrq
                            if (rsrq != CellInfo.UNAVAILABLE && rsrq in -34..3) bestRsrq = rsrq
                            val rssnr = cell.rssnr
                            if (rssnr != CellInfo.UNAVAILABLE && rssnr in -20..30) bestSinr = rssnr
                            val cqi = cell.cqi
                            if (cqi != CellInfo.UNAVAILABLE && cqi in 1..15) bestCqi = cqi
                            val ta = cell.timingAdvance
                            if (ta != CellInfo.UNAVAILABLE && ta in 0..1282) bestTimingAdvance = ta
                        } else if (cell is CellSignalStrengthNr) {
                            val ssRsrp = cell.ssRsrp
                            if (ssRsrp != CellInfo.UNAVAILABLE && ssRsrp in -140..-40) bestRsrp = ssRsrp
                            val ssRsrq = cell.ssRsrq
                            if (ssRsrq != CellInfo.UNAVAILABLE && ssRsrq in -43..20) bestRsrq = ssRsrq
                            val ssSinr = cell.ssSinr
                            if (ssSinr != CellInfo.UNAVAILABLE && ssSinr in -23..40) bestSinr = ssSinr
                        }
                    }
                }
            }

            // Cell Identity metadata from allCellInfo
            var pci: Int? = null
            var tac: Int? = null
            var ci: Long? = null
            var band: String? = null
            var earfcn: Int? = null

            try {
                val cellInfoList = activeManager.allCellInfo
                val registeredCell = cellInfoList?.firstOrNull { it.isRegistered } ?: cellInfoList?.firstOrNull()

                if (registeredCell is CellInfoLte) {
                    val identity = registeredCell.cellIdentity
                    if (identity.pci != CellInfo.UNAVAILABLE) pci = identity.pci
                    if (identity.tac != CellInfo.UNAVAILABLE) tac = identity.tac
                    if (identity.ci != CellInfo.UNAVAILABLE) ci = identity.ci.toLong()
                    if (identity.earfcn != CellInfo.UNAVAILABLE) {
                        earfcn = identity.earfcn
                        band = mapEarfcnToBand(identity.earfcn)
                    }
                    if (bestTimingAdvance == null && registeredCell.cellSignalStrength.timingAdvance != CellInfo.UNAVAILABLE) {
                        bestTimingAdvance = registeredCell.cellSignalStrength.timingAdvance
                    }
                } else if (registeredCell is CellInfoNr) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val identity = registeredCell.cellIdentity as? android.telephony.CellIdentityNr
                        identity?.let {
                            if (it.pci != CellInfo.UNAVAILABLE) pci = it.pci
                            if (it.tac != CellInfo.UNAVAILABLE) tac = it.tac
                            if (it.nci != CellInfo.UNAVAILABLE.toLong()) ci = it.nci
                            if (it.nrarfcn != CellInfo.UNAVAILABLE) {
                                earfcn = it.nrarfcn
                                band = "NR ARFCN ${it.nrarfcn}"
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Unable to query allCellInfo", e)
            }

            val distanceMeters = bestTimingAdvance?.let { if (it in 0..1282) it * 78 else null }

            return SignalData(
                dbm = bestDbm,
                asu = bestAsu,
                level = bestLevel,
                networkType = resolvedNetworkType,
                operatorName = operator,
                simSlotIndex = simSlot,
                subscriptionId = subId,
                isConnected = bestDbm != null,
                rsrp = bestRsrp ?: bestDbm,
                rsrq = bestRsrq,
                sinr = bestSinr,
                cqi = bestCqi,
                timingAdvance = bestTimingAdvance,
                estimatedDistanceMeters = distanceMeters,
                pci = pci,
                tac = tac,
                ci = ci,
                band = band,
                earfcn = earfcn,
                timestamp = System.currentTimeMillis()
            )
        }

        val (initialManager, _) = getTargetedTelephonyManager()

        try {
            val initialSignal = initialManager.signalStrength
            trySend(extractSignalData(initialSignal))
        } catch (e: Exception) {
            Log.w(tag, "Unable to get initial signal snapshot", e)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(),
                TelephonyCallback.SignalStrengthsListener,
                TelephonyCallback.DisplayInfoListener,
                TelephonyCallback.ServiceStateListener {

                override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                    trySend(extractSignalData(signalStrength))
                }

                override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
                    currentOverrideType = telephonyDisplayInfo.overrideNetworkType
                    try {
                        val (mgr, _) = getTargetedTelephonyManager()
                        trySend(extractSignalData(mgr.signalStrength, currentOverrideType))
                    } catch (e: Exception) {
                        Log.w(tag, "Error updating display info", e)
                    }
                }

                override fun onServiceStateChanged(serviceState: ServiceState) {
                    try {
                        val (mgr, _) = getTargetedTelephonyManager()
                        trySend(extractSignalData(mgr.signalStrength))
                    } catch (e: Exception) {
                        Log.w(tag, "Error updating service state", e)
                    }
                }
            }

            try {
                initialManager.registerTelephonyCallback(ContextCompat.getMainExecutor(context), callback)
            } catch (e: Exception) {
                Log.e(tag, "Error registering TelephonyCallback", e)
            }

            awaitClose {
                try {
                    initialManager.unregisterTelephonyCallback(callback)
                } catch (e: Exception) {
                    Log.e(tag, "Error unregistering TelephonyCallback", e)
                }
            }
        } else {
            val listener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                    trySend(extractSignalData(signalStrength))
                }

                @Deprecated("Deprecated in Java")
                override fun onServiceStateChanged(serviceState: ServiceState?) {
                    try {
                        val (mgr, _) = getTargetedTelephonyManager()
                        trySend(extractSignalData(mgr.signalStrength))
                    } catch (e: Exception) {
                        Log.w(tag, "Error updating service state fallback", e)
                    }
                }

                override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        currentOverrideType = telephonyDisplayInfo.overrideNetworkType
                        try {
                            val (mgr, _) = getTargetedTelephonyManager()
                            trySend(extractSignalData(mgr.signalStrength, currentOverrideType))
                        } catch (e: Exception) {
                            Log.w(tag, "Error on display info fallback", e)
                        }
                    }
                }
            }

            var events = PhoneStateListener.LISTEN_SIGNAL_STRENGTHS or PhoneStateListener.LISTEN_SERVICE_STATE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                events = events or PhoneStateListener.LISTEN_DISPLAY_INFO_CHANGED
            }

            try {
                @Suppress("DEPRECATION")
                initialManager.listen(listener, events)
            } catch (e: Exception) {
                Log.e(tag, "Error registering PhoneStateListener", e)
            }

            awaitClose {
                try {
                    @Suppress("DEPRECATION")
                    initialManager.listen(listener, PhoneStateListener.LISTEN_NONE)
                } catch (e: Exception) {
                    Log.e(tag, "Error unregistering PhoneStateListener", e)
                }
            }
        }
    }
}
