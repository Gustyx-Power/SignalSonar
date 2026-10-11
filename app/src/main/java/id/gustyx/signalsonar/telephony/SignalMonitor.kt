package id.gustyx.signalsonar.telephony

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.CellSignalStrength
import android.telephony.CellSignalStrengthCdma
import android.telephony.CellSignalStrengthGsm
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.CellSignalStrengthWcdma
import android.telephony.PhoneStateListener
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import id.gustyx.signalsonar.data.model.NetworkType
import id.gustyx.signalsonar.data.model.SignalData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Monitors real-time cellular signal strength, network technology, and carrier metadata.
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

        // Helper to resolve the targeted TelephonyManager for default data SIM
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

        // Helper to extract SignalData from a SignalStrength snapshot
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

            // Resolve operator name
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

            // Prioritize NR / LTE / WCDMA cell signals
            for (cell in cellStrengths) {
                val dbm = cell.dbm
                // Check valid range for cellular dBm (-140 to -40)
                if (dbm in -140..-40) {
                    val isPriorityType = cell is CellSignalStrengthNr || cell is CellSignalStrengthLte
                    if (bestDbm == null || (isPriorityType && dbm > bestDbm)) {
                        bestDbm = dbm
                        bestLevel = cell.level
                        bestAsu = cell.asuLevel
                    }
                }
            }

            // Fallback if specific cells had out-of-range values
            if (bestDbm == null && cellStrengths.isNotEmpty()) {
                val firstCell = cellStrengths.first()
                if (firstCell.dbm in -140..-40) {
                    bestDbm = firstCell.dbm
                    bestLevel = firstCell.level
                    bestAsu = firstCell.asuLevel
                }
            }

            return SignalData(
                dbm = bestDbm,
                asu = bestAsu,
                level = bestLevel,
                networkType = resolvedNetworkType,
                operatorName = operator,
                simSlotIndex = simSlot,
                subscriptionId = subId,
                isConnected = bestDbm != null,
                timestamp = System.currentTimeMillis()
            )
        }

        val (initialManager, _) = getTargetedTelephonyManager()

        // Emit initial state if available
        try {
            val initialSignal = initialManager.signalStrength
            trySend(extractSignalData(initialSignal))
        } catch (e: Exception) {
            Log.w(tag, "Unable to get initial signal snapshot", e)
        }

        // Implementation for API 31+ (Android 12+)
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
            // Implementation for API 29-30 (Android 10 & 11 fallback)
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
