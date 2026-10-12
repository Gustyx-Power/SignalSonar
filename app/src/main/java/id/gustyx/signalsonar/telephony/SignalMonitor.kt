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
import id.gustyx.signalsonar.data.model.CellTowerInfo
import id.gustyx.signalsonar.data.model.CoverageZone
import id.gustyx.signalsonar.data.model.NeighborCell
import id.gustyx.signalsonar.data.model.NetworkType
import id.gustyx.signalsonar.data.model.SignalData
import id.gustyx.signalsonar.data.model.SimCardInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Monitors real-time cellular signal strength, network technology, RF engineering metrics,
 * global frequency bands, carrier aggregation (LTE-CA), 5G mode (NSA/SA), and Dual SIM slots.
 */
class SignalMonitor(private val context: Context) {

    private val tag = "SignalMonitor"

    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager

    data class BandDetails(
        val bandName: String,
        val duplexMode: String,
        val downlinkMhz: Double?,
        val uplinkMhz: Double?
    )

    fun getActiveSimCards(): List<SimCardInfo> {
        val list = mutableListOf<SimCardInfo>()
        try {
            val subs = subscriptionManager?.activeSubscriptionInfoList ?: emptyList()
            val defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId()
            subs.forEach { sub ->
                list.add(
                    SimCardInfo(
                        subscriptionId = sub.subscriptionId,
                        slotIndex = sub.simSlotIndex,
                        displayName = sub.displayName?.toString().orEmpty(),
                        carrierName = sub.carrierName?.toString().orEmpty(),
                        isDefaultData = sub.subscriptionId == defaultDataSubId,
                        countryIso = sub.countryIso?.uppercase().orEmpty()
                    )
                )
            }
        } catch (e: SecurityException) {
            Log.w(tag, "Permission not granted to read subscription info", e)
        }
        return list
    }

    private fun parseEarfcnDetails(earfcn: Int): BandDetails {
        return when (earfcn) {
            in 0..599 -> {
                val dl = 2110.0 + 0.1 * (earfcn - 0)
                val ul = 1920.0 + 0.1 * (earfcn - 0)
                BandDetails("B1 (2100 MHz)", "FDD", dl, ul)
            }
            in 600..1199 -> {
                val dl = 1930.0 + 0.1 * (earfcn - 600)
                val ul = 1850.0 + 0.1 * (earfcn - 600)
                BandDetails("B2 (1900 MHz)", "FDD", dl, ul)
            }
            in 1200..1949 -> {
                val dl = 1805.0 + 0.1 * (earfcn - 1200)
                val ul = 1710.0 + 0.1 * (earfcn - 1200)
                BandDetails("B3 (1800 MHz)", "FDD", dl, ul)
            }
            in 1950..2399 -> {
                val dl = 2110.0 + 0.1 * (earfcn - 1950)
                val ul = 1710.0 + 0.1 * (earfcn - 1950)
                BandDetails("B4 (AWS 1700/2100)", "FDD", dl, ul)
            }
            in 2400..2649 -> {
                val dl = 869.0 + 0.1 * (earfcn - 2400)
                val ul = 824.0 + 0.1 * (earfcn - 2400)
                BandDetails("B5 (850 MHz)", "FDD", dl, ul)
            }
            in 2750..3449 -> {
                val dl = 2620.0 + 0.1 * (earfcn - 2750)
                val ul = 2500.0 + 0.1 * (earfcn - 2750)
                BandDetails("B7 (2600 MHz)", "FDD", dl, ul)
            }
            in 3450..3799 -> {
                val dl = 925.0 + 0.1 * (earfcn - 3450)
                val ul = 880.0 + 0.1 * (earfcn - 3450)
                BandDetails("B8 (900 MHz)", "FDD", dl, ul)
            }
            in 5010..5179 -> {
                val dl = 729.0 + 0.1 * (earfcn - 5010)
                val ul = 699.0 + 0.1 * (earfcn - 5010)
                BandDetails("B12 (700 MHz)", "FDD", dl, ul)
            }
            in 6150..6449 -> {
                val dl = 791.0 + 0.1 * (earfcn - 6150)
                val ul = 832.0 + 0.1 * (earfcn - 6150)
                BandDetails("B20 (800 MHz)", "FDD", dl, ul)
            }
            in 9210..9659 -> {
                val dl = 758.0 + 0.1 * (earfcn - 9210)
                val ul = 703.0 + 0.1 * (earfcn - 9210)
                BandDetails("B28 (700 MHz)", "FDD", dl, ul)
            }
            in 37750..38249 -> {
                val f = 2570.0 + 0.1 * (earfcn - 37750)
                BandDetails("B38 (TDD 2600 MHz)", "TDD", f, f)
            }
            in 38650..39649 -> {
                val f = 2300.0 + 0.1 * (earfcn - 38650)
                BandDetails("B40 (TDD 2300 MHz)", "TDD", f, f)
            }
            in 39650..41589 -> {
                val f = 2496.0 + 0.1 * (earfcn - 39650)
                BandDetails("B41 (TDD 2500 MHz)", "TDD", f, f)
            }
            in 66436..67335 -> {
                val dl = 2110.0 + 0.1 * (earfcn - 66436)
                val ul = 1710.0 + 0.1 * (earfcn - 66436)
                BandDetails("B66 (AWS-3)", "FDD", dl, ul)
            }
            in 68586..68935 -> {
                val dl = 617.0 + 0.1 * (earfcn - 68586)
                val ul = 663.0 + 0.1 * (earfcn - 68586)
                BandDetails("B71 (600 MHz)", "FDD", dl, ul)
            }
            else -> BandDetails(if (earfcn > 0) "EARFCN $earfcn" else "LTE Band Standar", "FDD", null, null)
        }
    }

    private fun parseNrArfcnDetails(nrarfcn: Int): BandDetails {
        return when (nrarfcn) {
            in 422000..434000 -> BandDetails("n1 (2100 MHz)", "FDD", 2110.0 + 0.005 * (nrarfcn - 422000), null)
            in 361000..376000 -> BandDetails("n3 (1800 MHz)", "FDD", 1805.0 + 0.005 * (nrarfcn - 361000), null)
            in 151600..160600 -> BandDetails("n28 (700 MHz)", "FDD", 758.0 + 0.005 * (nrarfcn - 151600), null)
            in 460000..480000 -> BandDetails("n40 (TDD 2300 MHz)", "TDD", 2300.0 + 0.005 * (nrarfcn - 460000), null)
            in 499200..537999 -> BandDetails("n41 (TDD 2500 MHz)", "TDD", 2496.0 + 0.015 * (nrarfcn - 499200), null)
            in 620000..680000 -> BandDetails("n78 (3500 MHz)", "TDD", 3300.0 + 0.015 * (nrarfcn - 620000), null)
            in 620000..653333 -> BandDetails("n77 (3700 MHz)", "TDD", 3300.0 + 0.015 * (nrarfcn - 620000), null)
            else -> BandDetails("NR ARFCN $nrarfcn", "TDD", null, null)
        }
    }

    @SuppressLint("MissingPermission")
    fun observeSignal(targetSubId: Int? = null): Flow<SignalData> = callbackFlow {
        var currentOverrideType = 0

        fun getTargetedTelephonyManager(): Pair<TelephonyManager, SubscriptionInfo?> {
            var subInfo: SubscriptionInfo? = null
            var subId = targetSubId ?: SubscriptionManager.getDefaultDataSubscriptionId()

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

            val isCarrierAggregation = overrideType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA

            val nrMode = when (overrideType) {
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA -> "5G NSA (Non-Standalone)"
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED -> "5G Advanced (High-Speed)"
                else -> if (rawNetworkType == TelephonyManager.NETWORK_TYPE_NR) "5G SA (Standalone)" else null
            }

            val isRoaming = try {
                activeManager.isNetworkRoaming
            } catch (_: Exception) {
                false
            }

            val countryIso = subInfo?.countryIso?.uppercase()
                ?: activeManager.networkCountryIso?.uppercase()
                ?: ""

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
                    isConnected = false,
                    isCarrierAggregationActive = isCarrierAggregation,
                    nrMode = nrMode,
                    isRoaming = isRoaming,
                    countryIso = countryIso
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
            var mcc: String? = null
            var mnc: String? = null
            var enodebId: Int? = null
            var sectorId: Int? = null
            var duplexMode = "FDD"
            var channelBandwidthMhz: Float? = null
            var downlinkFreqMhz: Double? = null
            var uplinkFreqMhz: Double? = null
            val neighborCells = mutableListOf<NeighborCell>()

            try {
                val cellInfoList = activeManager.allCellInfo
                val registeredCell = cellInfoList?.firstOrNull { it.isRegistered } ?: cellInfoList?.firstOrNull()

                if (registeredCell is CellInfoLte) {
                    val identity = registeredCell.cellIdentity
                    if (identity.pci != CellInfo.UNAVAILABLE) pci = identity.pci
                    if (identity.tac != CellInfo.UNAVAILABLE) tac = identity.tac
                    if (identity.ci != CellInfo.UNAVAILABLE) {
                        val rawCi = identity.ci.toLong()
                        ci = rawCi
                        if (rawCi > 0) {
                            enodebId = (rawCi shr 8).toInt()
                            sectorId = (rawCi and 0xFF).toInt()
                        }
                    }
                    if (identity.earfcn != CellInfo.UNAVAILABLE) {
                        earfcn = identity.earfcn
                        val bandInfo = parseEarfcnDetails(identity.earfcn)
                        band = bandInfo.bandName
                        duplexMode = bandInfo.duplexMode
                        downlinkFreqMhz = bandInfo.downlinkMhz
                        uplinkFreqMhz = bandInfo.uplinkMhz
                    }
                    if (identity.bandwidth != CellInfo.UNAVAILABLE && identity.bandwidth > 0) {
                        channelBandwidthMhz = identity.bandwidth / 1000f
                    }
                    mcc = identity.mccString
                    mnc = identity.mncString
                    if (bestTimingAdvance == null && registeredCell.cellSignalStrength.timingAdvance != CellInfo.UNAVAILABLE) {
                        bestTimingAdvance = registeredCell.cellSignalStrength.timingAdvance
                    }
                } else if (registeredCell is CellInfoNr) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val identity = registeredCell.cellIdentity as? android.telephony.CellIdentityNr
                        identity?.let {
                            if (it.pci != CellInfo.UNAVAILABLE) pci = it.pci
                            if (it.tac != CellInfo.UNAVAILABLE) tac = it.tac
                            if (it.nci != CellInfo.UNAVAILABLE.toLong()) {
                                ci = it.nci
                                if (it.nci > 0) {
                                    enodebId = (it.nci shr 14).toInt()
                                    sectorId = (it.nci and 0x3FFF).toInt()
                                }
                            }
                            if (it.nrarfcn != CellInfo.UNAVAILABLE) {
                                earfcn = it.nrarfcn
                                val bandInfo = parseNrArfcnDetails(it.nrarfcn)
                                band = bandInfo.bandName
                                duplexMode = bandInfo.duplexMode
                                downlinkFreqMhz = bandInfo.downlinkMhz
                            }
                            mcc = it.mccString
                            mnc = it.mncString
                        }
                    }
                }

                // Parse Neighbor Cells
                cellInfoList?.filter { !it.isRegistered }?.forEach { cell ->
                    when (cell) {
                        is CellInfoLte -> {
                            val p = cell.cellIdentity.pci.takeIf { it != CellInfo.UNAVAILABLE }
                            val d = cell.cellSignalStrength.dbm.takeIf { it in -140..-40 }
                            if (p != null || d != null) {
                                neighborCells.add(
                                    NeighborCell(
                                        pci = p,
                                        dbm = d,
                                        networkType = "LTE",
                                        isRegistered = false
                                    )
                                )
                            }
                        }
                        is CellInfoNr -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val nrIdent = cell.cellIdentity as? android.telephony.CellIdentityNr
                                val p = nrIdent?.pci?.takeIf { it != CellInfo.UNAVAILABLE }
                                val d = cell.cellSignalStrength.dbm.takeIf { it in -140..-40 }
                                if (p != null || d != null) {
                                    neighborCells.add(
                                        NeighborCell(
                                            pci = p,
                                            dbm = d,
                                            networkType = "5G NR",
                                            isRegistered = false
                                        )
                                    )
                                }
                            }
                        }
                        is CellInfoWcdma -> {
                            val p = cell.cellIdentity.psc.takeIf { it != CellInfo.UNAVAILABLE }
                            val d = cell.cellSignalStrength.dbm.takeIf { it in -140..-40 }
                            if (p != null || d != null) {
                                neighborCells.add(
                                    NeighborCell(
                                        pci = p,
                                        dbm = d,
                                        networkType = "3G",
                                        isRegistered = false
                                    )
                                )
                            }
                        }
                        is CellInfoGsm -> {
                            val p = cell.cellIdentity.cid.takeIf { it != CellInfo.UNAVAILABLE }
                            val d = cell.cellSignalStrength.dbm.takeIf { it in -140..-40 }
                            if (p != null || d != null) {
                                neighborCells.add(
                                    NeighborCell(
                                        pci = p,
                                        dbm = d,
                                        networkType = "2G",
                                        isRegistered = false
                                    )
                                )
                            }
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Unable to query allCellInfo", e)
            }

            // Calculate Distance and BTS Coverage Range
            val isPreciseDistance = bestTimingAdvance != null && bestTimingAdvance in 0..1282
            val distanceMeters = when {
                isPreciseDistance -> bestTimingAdvance!! * 78
                bestDbm != null && bestDbm in -140..-40 -> {
                    val pathLoss = 43 - bestDbm
                    Math.pow(10.0, (pathLoss - 40.0) / 32.0).toInt().coerceIn(100, 5000)
                }
                else -> null
            }

            val maxCoverageMeters = when {
                band?.contains("B28") == true || band?.contains("B8") == true || band?.contains("700") == true || band?.contains("900") == true -> 8000
                band?.contains("B3") == true || band?.contains("B1") == true || band?.contains("1800") == true || band?.contains("2100") == true -> 3500
                band?.contains("B40") == true || band?.contains("B7") == true || band?.contains("2300") == true || band?.contains("2500") == true -> 2000
                band?.contains("NR") == true || band?.contains("5G") == true -> 1500
                else -> 3000
            }

            val coverageFraction = if (distanceMeters != null) {
                (distanceMeters.toFloat() / maxCoverageMeters.toFloat()).coerceIn(0.04f, 1.0f)
            } else {
                0f
            }

            val coverageZone = when {
                distanceMeters == null -> CoverageZone.UNKNOWN
                coverageFraction < 0.25f -> CoverageZone.NEAR_TOWER
                coverageFraction <= 0.70f -> CoverageZone.MID_CELL
                coverageFraction <= 0.95f -> CoverageZone.CELL_EDGE
                else -> CoverageZone.OUT_OF_RANGE
            }

            val towerInfo = CellTowerInfo(
                enodebId = enodebId,
                sectorId = sectorId,
                cellId = ci,
                pci = pci,
                tac = tac,
                band = band,
                earfcn = earfcn,
                mcc = mcc,
                mnc = mnc,
                timingAdvance = bestTimingAdvance,
                distanceMeters = distanceMeters,
                isDistancePrecise = isPreciseDistance,
                estimatedMaxCoverageMeters = maxCoverageMeters,
                coveragePercentage = coverageFraction,
                coverageZone = coverageZone,
                duplexMode = duplexMode,
                bandwidthMhz = channelBandwidthMhz,
                downlinkMhz = downlinkFreqMhz,
                uplinkMhz = uplinkFreqMhz,
                isCarrierAggregation = isCarrierAggregation,
                nrMode = nrMode,
                neighborCells = neighborCells
            )

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
                cellTowerInfo = towerInfo,
                isCarrierAggregationActive = isCarrierAggregation,
                nrMode = nrMode,
                duplexMode = duplexMode,
                channelBandwidthMhz = channelBandwidthMhz,
                downlinkFreqMhz = downlinkFreqMhz,
                uplinkFreqMhz = uplinkFreqMhz,
                isRoaming = isRoaming,
                countryIso = countryIso,
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
                        val currentSignal = mgr.signalStrength
                        trySend(extractSignalData(currentSignal, currentOverrideType))
                    } catch (e: Exception) {
                        Log.w(tag, "Error extracting signal after display info change", e)
                    }
                }

                override fun onServiceStateChanged(serviceState: ServiceState) {
                    try {
                        val (mgr, _) = getTargetedTelephonyManager()
                        val currentSignal = mgr.signalStrength
                        trySend(extractSignalData(currentSignal, currentOverrideType))
                    } catch (e: Exception) {
                        Log.w(tag, "Error extracting signal after service state change", e)
                    }
                }
            }

            initialManager.registerTelephonyCallback(context.mainExecutor, callback)
            awaitClose {
                initialManager.unregisterTelephonyCallback(callback)
            }
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                    trySend(extractSignalData(signalStrength))
                }

                @Deprecated("Deprecated in Java")
                override fun onServiceStateChanged(serviceState: ServiceState) {
                    try {
                        val (mgr, _) = getTargetedTelephonyManager()
                        val currentSignal = mgr.signalStrength
                        trySend(extractSignalData(currentSignal, currentOverrideType))
                    } catch (e: Exception) {
                        Log.w(tag, "Error in legacy onServiceStateChanged", e)
                    }
                }
            }

            @Suppress("DEPRECATION")
            initialManager.listen(
                listener,
                PhoneStateListener.LISTEN_SIGNAL_STRENGTHS or PhoneStateListener.LISTEN_SERVICE_STATE
            )

            awaitClose {
                @Suppress("DEPRECATION")
                initialManager.listen(listener, PhoneStateListener.LISTEN_NONE)
            }
        }
    }
}
