package io.github.arcaneplugins.levelledmobs.rules

import io.github.arcaneplugins.levelledmobs.annotations.MergableRule

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
        if (mergableRule !is ChunkKillOptions) {
            return
        }

        if (isDefault) return

        if (disableVanillaDrops != null) this.disableVanillaDrops = disableVanillaDrops!!
        if (disableItemBoost != null) this.disableItemBoost = disableItemBoost!!
        if (disableXpDrops != null) this.disableXpDrops = disableXpDrops!!
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
        if (disableVanillaDrops != null && disableVanillaDrops!!) {
            sb.append("disableVanillaDrops")
        }
        if (disableItemBoost != null && disableItemBoost!!) {
            if (sb.isNotEmpty()) sb.append(", ")
            sb.append("disableItemBoost")
        }
        if (disableXpDrops != null && disableXpDrops!!) {
            if (sb.isNotEmpty()) sb.append(", ")
            sb.append("disableXpDrops")
        }

        return sb.toString()
    }
}