package io.github.arcaneplugins.levelledmobs.rules

import io.github.arcaneplugins.levelledmobs.misc.Addition
import org.jetbrains.annotations.Contract

/**
 * Holds any custom multipliers values parsed from rules.yml
 *
 * @author stumper66
 * @since 3.0.0
 */
class FineTuningAttributes : MergableRule, Cloneable {
    private var multipliers = mutableMapOf<Addition, Multiplier>()
    var doNotMerge: Boolean = false
    var useStacked: Boolean? = null

    fun getUseStacked(): Boolean {
        return useStacked != null && useStacked!!
    }

    val isEmpty: Boolean
        get() = multipliers.isEmpty() && !doNotMerge && (useStacked == null)


    override fun merge(mergableRule: MergableRule?) {
        if (mergableRule !is FineTuningAttributes) {
            return
        }

        multipliers.putAll(mergableRule.copyMultipliers())
    }

    fun addItem(addition: Addition?, multiplier: Multiplier?) {
        multipliers[addition!!] = multiplier!!
    }

    override val doMerge: Boolean
        get() = !this.doNotMerge

    fun getItem(addition: Addition): Multiplier? {
        return multipliers[addition]
    }

    companion object{
        fun getShortName(addition: Addition): String {
            return when (addition) {
                Addition.ATTRIBUTE_ATTACK_DAMAGE -> { "attkDmg" }
                Addition.CREEPER_BLAST_DAMAGE -> { "creeperDmg" }
                Addition.ATTRIBUTE_MAX_HEALTH -> { "maxHlth" }
                Addition.ATTRIBUTE_MOVEMENT_SPEED -> { "moveSpd" }
                Addition.CUSTOM_RANGED_ATTACK_DAMAGE -> { "rangdAtkDmg" }
                Addition.CUSTOM_ITEM_DROP -> { "itemDrp" }
                Addition.ATTRIBUTE_ARMOR_BONUS -> { "armrBns" }
                Addition.ATTRIBUTE_ARMOR_TOUGHNESS -> { "armrTuf" }
                Addition.ATTRIBUTE_ATTACK_KNOCKBACK -> { "attkKnbk" }
                Addition.ATTRIBUTE_FLYING_SPEED -> { "flySpd" }
                Addition.ATTRIBUTE_KNOCKBACK_RESISTANCE -> { "knbkRst"}
                Addition.ATTRIBUTE_HORSE_JUMP_STRENGTH -> { "horseJump" }
                Addition.ATTRIBUTE_ZOMBIE_SPAWN_REINFORCEMENTS -> {"zmbRnfrce"}
                Addition.ATTRIBUTE_FOLLOW_RANGE -> { "flwRng" }
                Addition.CUSTOM_XP_DROP -> {"xpDrp"}
            }
        }
    }

    fun getAdditionFromLMMultiplier(lmMultiplier: LMMultiplier): Addition {
        return when (lmMultiplier) {
            LMMultiplier.ATTACK_DAMAGE -> { Addition.ATTRIBUTE_ATTACK_DAMAGE }
            LMMultiplier.CREEPER_BLAST_DAMAGE -> { Addition.CREEPER_BLAST_DAMAGE }
            LMMultiplier.MAX_HEALTH -> { Addition.ATTRIBUTE_MAX_HEALTH }
            LMMultiplier.MOVEMENT_SPEED -> { Addition.ATTRIBUTE_MOVEMENT_SPEED }
            LMMultiplier.RANGED_ATTACK_DAMAGE -> { Addition.CUSTOM_RANGED_ATTACK_DAMAGE }
            LMMultiplier.ITEM_DROP -> { Addition.CUSTOM_ITEM_DROP }
            LMMultiplier.ARMOR_BONUS -> { Addition.ATTRIBUTE_ARMOR_BONUS }
            LMMultiplier.ARMOR_TOUGHNESS -> { Addition.ATTRIBUTE_ARMOR_TOUGHNESS }
            LMMultiplier.ATTACK_KNOCKBACK -> { Addition.ATTRIBUTE_ATTACK_KNOCKBACK }
            LMMultiplier.FLYING_SPEED -> { Addition.ATTRIBUTE_FLYING_SPEED }
            LMMultiplier.KNOCKBACK_RESISTANCE -> { Addition.ATTRIBUTE_KNOCKBACK_RESISTANCE }
            LMMultiplier.HORSE_JUMP_STRENGTH -> { Addition.ATTRIBUTE_HORSE_JUMP_STRENGTH }
            LMMultiplier.ZOMBIE_SPAWN_REINFORCEMENTS -> { Addition.ATTRIBUTE_ZOMBIE_SPAWN_REINFORCEMENTS }
            LMMultiplier.FOLLOW_RANGE -> { Addition.ATTRIBUTE_FOLLOW_RANGE }
            LMMultiplier.XP_DROP -> { Addition.CUSTOM_XP_DROP }
        }
    }

    @JvmRecord
    data class Multiplier(val addition: Addition, val useStacked: Boolean, val value: Float) {
        @Contract(pure = true)
        override fun toString(): String {
            val sb = StringBuilder()
            sb.append(getShortName(addition))
            if (useStacked) sb.append(" (stkd): ")
            else sb.append(": ")
            sb.append(value)

            return sb.toString()
        }
    }

    override fun cloneItem(): Any {
        var copy: FineTuningAttributes? = null
        try {
            copy = super.clone() as FineTuningAttributes
            copy.cloneMultipliers()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return copy as Any
    }

    private fun cloneMultipliers() {
        val copy: Map<Addition, Multiplier> = copyMultipliers()
        this.multipliers = LinkedHashMap(copy.size)
        multipliers.putAll(copy)
    }

    private fun copyMultipliers(): Map<Addition, Multiplier> {
        val copy: MutableMap<Addition, Multiplier> = LinkedHashMap(
            multipliers.size
        )

        for (addition in multipliers.keys) {
            val old = multipliers[addition]
            copy[addition] = Multiplier(addition, old!!.useStacked, old.value)
        }

        return copy
    }

    override fun toString(): String {
        if (this.isEmpty) return "No items"

        val sb = java.lang.StringBuilder()

        if (this.getUseStacked()) sb.append("(all stk)")

        for ((addition, useStacked1, value) in multipliers.values) {
            if (sb.isNotEmpty()) sb.append(", ")

            sb.append(getShortName(addition))
            sb.append(": ")
            sb.append(value)
            if (useStacked1) {
                sb.append(" (")
                sb.append("stk)")
            }
        }

        if (doNotMerge) {
            if (sb.isNotEmpty()) sb.append(", ")
            sb.append("noMerge")
        }

        return sb.toString()
    }
}