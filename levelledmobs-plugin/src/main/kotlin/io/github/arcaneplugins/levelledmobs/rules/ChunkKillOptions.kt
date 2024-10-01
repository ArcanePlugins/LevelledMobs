package io.github.arcaneplugins.levelledmobs.rules

/**
 * Holds various options relating to the
 * chunk kill feature
 *
 * @author stumper66
 * @since 3.12.0
 */
class ChunkKillOptions : MergableRule, Cloneable {
    var disableVanillaDrops: Boolean? = null
    var disableItemBoost: Boolean? = true
    var disableXpDrops: Boolean? = true

    val isDefault: Boolean
        get() = (disableVanillaDrops == null && disableItemBoost == null && disableXpDrops == null)

    fun getDisableVanillaDrops(): Boolean {
        return this.disableVanillaDrops != null && disableVanillaDrops!!
    }

    fun getDisableItemBoost(): Boolean {
        return this.disableItemBoost != null && disableItemBoost!!
    }

    fun getDisableXpDrops(): Boolean {
        return this.disableXpDrops != null && disableXpDrops!!
    }

    override fun merge(mergableRule: MergableRule?) {
        if (mergableRule !is ChunkKillOptions) return
        if (mergableRule.isDefault) return

        if (disableVanillaDrops != null) this.disableVanillaDrops = mergableRule.disableVanillaDrops
        if (disableItemBoost != null) this.disableItemBoost = mergableRule.disableItemBoost
        if (disableXpDrops != null) this.disableXpDrops = mergableRule.disableXpDrops
    }

    override val doMerge = true

    override fun cloneItem(): Any {
        var copy: ChunkKillOptions? = null
        try {
            copy = super.clone() as ChunkKillOptions
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return copy as Any
    }

    override fun toString(): String {
        if (this.isDefault) return "Default"

        val sb = StringBuilder()
        if (disableVanillaDrops != null) {
            if (getDisableVanillaDrops()) sb.append("disableVanillaDrops")
            else sb.append("disableVanillaDrops: false")
        }
        if (disableItemBoost != null) {
            if (sb.isNotEmpty()) sb.append(", ")
            if (getDisableItemBoost()) sb.append("disableItemBoost")
            else sb.append("disableItemBoost: false")
        }
        if (disableXpDrops != null) {
            if (sb.isNotEmpty()) sb.append(", ")
            if (getDisableXpDrops()) sb.append("disableXpDrops")
            else sb.append("disableXpDrops: false")
        }

        return sb.toString()
    }
}