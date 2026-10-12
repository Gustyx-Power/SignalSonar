package id.gustyx.signalsonar.data.model

/**
 * Representation of a detected physical SIM or eSIM card slot.
 */
data class SimCardInfo(
    val subscriptionId: Int,
    val slotIndex: Int,
    val displayName: String,
    val carrierName: String,
    val isDefaultData: Boolean,
    val countryIso: String = ""
) {
    val shortLabel: String
        get() = "SIM ${slotIndex + 1}: ${carrierName.ifBlank { displayName.ifBlank { "Operator" } }}"
}
