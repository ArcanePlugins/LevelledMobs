package io.github.arcaneplugins.levelledmobs.customdrops

/**
 * Holds settings used for the group-limits feature
 * of custom drops
 *
 * @author stumper66
 * @since 3.13.0
 */
class GroupLimits {
    var capTotal: Int = 0
    var capEquipped: Int = 0
    var capPerItem: Int = 0
    var capSelect: Int = 0
    var retries: Int = 0

    val isEmpty: Boolean
        get() {
            return capTotal <= 0 && capEquipped <= 0 && capPerItem <= 0 &&
                    capSelect <= 0 && retries <= 0
    }

    val hasCapTotal: Boolean
        get() = this.capTotal > 0

    val hasCapEquipped: Boolean
        get() = this.capEquipped > 0

    val hasCapPerItem: Boolean
        get() = this.capPerItem > 0

    val hasCapSelect: Boolean
        get() = this.capSelect > 0

    fun hasReachedCapTotal(amount: Int): Boolean {
        return hasCapTotal && amount >= this.capTotal
    }

    fun hasReachedCapEquipped(amount: Int): Boolean {
        return hasCapEquipped && amount >= this.capEquipped
    }

    fun hasReachedCapPerItem(amount: Int): Boolean {
        return hasCapPerItem && amount >= this.capPerItem
    }

    fun hasReachedCapSelect(amount: Int): Boolean {
        return hasCapSelect && amount >= this.capSelect
    }

    override fun toString(): String {
        return String.format(
            "capTotal: %s, capEquip: %s, capPerItem: %s, capSelect: %s, retries: %s",
            capTotal, capEquipped, capPerItem, capSelect, retries
        )
    }
}