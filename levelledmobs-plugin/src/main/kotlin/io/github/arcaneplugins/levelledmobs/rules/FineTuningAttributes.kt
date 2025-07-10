package io.github.arcaneplugins.levelledmobs.rules

import io.github.arcaneplugins.levelledmobs.enums.Addition
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import kotlin.collections.iterator
import kotlin.collections.set

/**
 * Holds any custom multipliers values parsed from rules.yml
 *
 * @author stumper66
 * @since 3.0.0
 */
class FineTuningAttributes : MergableRule, Cloneable {
    var multipliers: MutableMap<Addition, Multiplier>? = null
    var mobSpecificMultipliers: MutableMap<String, MutableMap<Addition, Multiplier>>? = null
    var baseAttributeModifiers: MutableMap<Addition, Multiplier>? = null
    var mobSpecificBaseModifiers: MutableMap<String, MutableMap<Addition, Multiplier>>? = null

    var doNotMergeAllMobs = false
    var doNotMergeMobSpecific = false
    var useStacked: Boolean? = null

    var doNotMerge: Boolean
        set(value) {
            doNotMergeMobSpecific = value
            doNotMergeAllMobs = value
        }
        get() = doNotMergeAllMobs || doNotMergeMobSpecific

//    val hasMobSpecificMultipliers: Boolean
//        get() = !mobSpecificMultipliers.isNullOrEmpty()
//
//    val hasBaseAttributeModifiers: Boolean
//        get() = !baseAttributeModifiers.isNullOrEmpty()

    fun getUseStacked(): Boolean {
        return useStacked != null && useStacked!!
    }

    val isEmpty: Boolean
        get() = multipliers.isNullOrEmpty() &&
                mobSpecificMultipliers.isNullOrEmpty() &&
                mobSpecificMultipliers.isNullOrEmpty() &&
                mobSpecificBaseModifiers.isNullOrEmpty()

    override fun merge(mergableRule: MergableRule?) {
        if (mergableRule !is FineTuningAttributes) return

        copyMultipliers(mergableRule, this)
    }

    override val doMerge: Boolean
        get() = !this.doNotMerge

    fun getMultiplier(
        addition: Addition,
        lmEntity: LivingEntityWrapper
    ): Multiplier? {
        if (mobSpecificMultipliers != null) {
            val result = mobSpecificMultipliers!![lmEntity.nameIfBaby]?.get(addition)
            if (result != null) return result
        }

        return if (multipliers == null)
            null
        else
            multipliers!![addition]
    }

    fun getBaseModifier(
        addition: Addition,
        lmEntity: LivingEntityWrapper
    ): Multiplier? {
        if (mobSpecificBaseModifiers != null) {
            val result = mobSpecificBaseModifiers!![lmEntity.nameIfBaby]?.get(addition)
            if (result != null) return result
        }

        return if (baseAttributeModifiers == null)
            null
        else
            baseAttributeModifiers!![addition]
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
                Addition.ATTRIBUTE_ZOMBIE_SPAWN_REINFORCEMENTS -> { "zmbRnfrce" }
                Addition.ATTRIBUTE_FOLLOW_RANGE -> { "flwRng" }
                Addition.CUSTOM_XP_DROP -> { "xpDrp" }
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

        private fun duplicateMultiplier(multiplier: Multiplier): Multiplier{
            return Multiplier(
                multiplier.addition,
                multiplier.useStacked,
                multiplier.value,
                multiplier.formula,
                multiplier.isAddition,
                multiplier.isBaseModifier
            )
        }

        private fun dulicateMap1(
            source: MutableMap<Addition, Multiplier>
        ): MutableMap<Addition, Multiplier>{
            val copy = mutableMapOf<Addition, Multiplier>()

            for (item in source) {
                val sourceAddition = item.key
                val sourceMultiplier = item.value

                copy[sourceAddition] = duplicateMultiplier(sourceMultiplier)
            }

            return copy
        }

        private fun dulicateMap2(
            source: MutableMap<String, MutableMap<Addition, Multiplier>>
        ): MutableMap<String, MutableMap<Addition, Multiplier>>{
            val copy = mutableMapOf<String, MutableMap<Addition, Multiplier>>()

            for (item in source){
                val sourceMob = item.key
                val sourceValues = item.value
                val copiedMap = mutableMapOf<Addition, Multiplier>()

                for (item2 in sourceValues)
                    copiedMap[item2.key] = duplicateMultiplier(item2.value)

                copy[sourceMob] = copiedMap
            }

            return copy
        }

        private fun copyMultipliers(
            source: FineTuningAttributes,
            dest: FineTuningAttributes
        ) {
            if (!source.multipliers.isNullOrEmpty()) {
                if (dest.multipliers == null) dest.multipliers = mutableMapOf()
                dest.multipliers!!.putAll(dulicateMap1(source.multipliers!!))
            }

            if (!source.mobSpecificMultipliers.isNullOrEmpty()){
                if (dest.mobSpecificMultipliers == null) dest.mobSpecificMultipliers = mutableMapOf()
                dest.mobSpecificMultipliers!!.putAll(dulicateMap2(source.mobSpecificMultipliers!!))
            }

            if (!source.baseAttributeModifiers.isNullOrEmpty()){
                if (dest.baseAttributeModifiers == null) dest.baseAttributeModifiers = mutableMapOf()
                dest.baseAttributeModifiers!!.putAll(dulicateMap1(source.baseAttributeModifiers!!))
            }

            if (!source.mobSpecificBaseModifiers.isNullOrEmpty()){
                if (dest.mobSpecificBaseModifiers == null) dest.mobSpecificBaseModifiers = mutableMapOf()
                dest.mobSpecificBaseModifiers!!.putAll(dulicateMap2(source.mobSpecificBaseModifiers!!))
            }
        }
    }

    override fun cloneItem(): Any {
        var copy: FineTuningAttributes? = null
        try {
            copy = super.clone() as FineTuningAttributes
            copyMultipliers(this, copy)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return copy!!
    }

    @JvmRecord
    data class Multiplier(
        val addition: Addition,
        val useStacked: Boolean,
        val value: Float?,
        val formula: String?,
        val isAddition: Boolean,
        val isBaseModifier: Boolean
    ) {
        val hasFormula: Boolean
            get() = !this.formula.isNullOrEmpty()

        val useValue: Float
            get() = value ?: 0f

        override fun toString(): String {
            val sb = StringBuilder()
            sb.append(getShortName(addition))

            if (hasFormula){
                sb.append(" formula: '")
                sb.append(formula).append("'")

                if (isAddition && !isBaseModifier)
                    sb.append(" (add)")
                else if (!isBaseModifier)
                    sb.append(" (multiply)")
            }
            else{
                if (value != null){
                    if (useStacked) sb.append(" (stkd): ")
                    else sb.append(": ")
                    sb.append(value)
                }
                else if (useStacked)
                    sb.append(" (stkd)")
            }

            return sb.toString()
        }
    }

    private fun formatMultipliers(
        items: MutableMap<Addition, Multiplier>?,
        sb: StringBuilder,
        header: String?,
        useCarriageReturn: Boolean
    ) {
        if (items == null) return

        if (useCarriageReturn) sb.append("\n    ")
        if (header != null)
            sb.append("&r$header: ")
        else
            sb.append("&r")

        var isFirst = true
        for (multiplier in multipliers!!.values) {
            if (isFirst)
                isFirst = false
            else
                sb.append(", ")

            sb.append(getShortName(multiplier.addition))
            if (multiplier.value != null)
                sb.append(": ").append(multiplier.value)
            if (multiplier.hasFormula)
                sb.append(multiplier.formula)
            else if (multiplier.useStacked)
                sb.append(" (stk)")
        }
    }

    private fun formatMobSpecific(
        items: MutableMap<String, MutableMap<Addition, Multiplier>>?,
        sb: StringBuilder,
        header: String?,
        useCarriageReturn: Boolean
    ) {
        if (items == null) return

        var isFirst = true
        if (useCarriageReturn) sb.append("\n    ")
        if (header != null)
            sb.append("&r$header: ")
        else
            sb.append("&r")

        for (item in items){
            if (isFirst)
                isFirst = false
            else
                sb.append(", ")

            var isFirstValues = true
            sb.append("&b${item.key}:&r ")
            for (mobValues in item.value){
                if (isFirstValues)
                    isFirstValues = false
                else
                    sb.append(", ")

                sb.append(mobValues.value)
            }
        }
    }

    override fun toString(): String {
        if (this.isEmpty) return "No items"

        val sb = StringBuilder()
        if (this.getUseStacked()) sb.append("(all stk)")
        var hadItems = false

        if (!multipliers.isNullOrEmpty()) {
            hadItems = true
            formatMultipliers(multipliers, sb, null, false)
        }

        if (doNotMerge) {
            if (sb.isNotEmpty()) sb.append(", ")
            sb.append("noMerge")
        }

        if (!mobSpecificMultipliers.isNullOrEmpty()){
            formatMobSpecific(mobSpecificMultipliers, sb, "mob specific", hadItems)
            hadItems = true
        }

        if (!baseAttributeModifiers.isNullOrEmpty()){
            formatMultipliers(baseAttributeModifiers, sb, "base mods", hadItems)
            hadItems = true
        }

        if (!mobSpecificBaseModifiers.isNullOrEmpty()){
            formatMobSpecific(mobSpecificBaseModifiers, sb, "mob specific base mods", hadItems)
        }

        return sb.toString()
    }
}