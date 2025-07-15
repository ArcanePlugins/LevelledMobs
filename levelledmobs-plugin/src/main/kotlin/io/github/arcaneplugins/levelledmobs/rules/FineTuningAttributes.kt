package io.github.arcaneplugins.levelledmobs.rules

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.enums.Addition
import io.github.arcaneplugins.levelledmobs.misc.EffectiveInfo
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import kotlin.collections.iterator
import kotlin.collections.set

/**
 * Holds any custom multipliers values parsed from rules.yml
 *
 * @author stumper66
 * @since 3.0.0
 */
class FineTuningAttributes : MergableRule, Cloneable, EffectiveInfo {
    var multipliers: MutableMap<Addition, Multiplier>? = null
    var mobSpecificMultipliers: MutableMap<String, MutableMap<Addition, Multiplier>>? = null
    var baseAttributeModifiers: MutableMap<Addition, Multiplier>? = null
    var mobSpecificBaseModifiers: MutableMap<String, MutableMap<Addition, Multiplier>>? = null

    var doNotMergeAllMultipliers = false
    var doNotMergeMobSpecificMultipliers = false
    var doNotMergeAllBaseMods = false
    var doNotMergeMobSpecificBaseMods = false
    var useStacked: Boolean? = null

    var doNotMerge: Boolean
        set(value) {
            doNotMergeAllMultipliers = value
            doNotMergeMobSpecificMultipliers = value
            doNotMergeAllBaseMods = value
            doNotMergeMobSpecificBaseMods = value
        }
        get() = doNotMergeAllMultipliers
                && doNotMergeMobSpecificMultipliers
                && doNotMergeAllBaseMods
                && doNotMergeMobSpecificBaseMods

    fun getUseStacked(): Boolean {
        return useStacked != null && useStacked!!
    }

    val isEmpty: Boolean
        get() = multipliers.isNullOrEmpty() &&
                baseAttributeModifiers.isNullOrEmpty() &&
                mobSpecificMultipliers.isNullOrEmpty() &&
                mobSpecificBaseModifiers.isNullOrEmpty()

    override fun merge(mergableRule: MergableRule?) {
        if (mergableRule !is FineTuningAttributes) return

        copyMultipliers(mergableRule, this, false)
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

    fun checkMerges(){
        if (!multipliers.isNullOrEmpty() && doNotMergeAllMultipliers)
            multipliers!!.clear()

        if (!mobSpecificMultipliers.isNullOrEmpty() && doNotMergeMobSpecificMultipliers)
            mobSpecificMultipliers!!.clear()

        if (!baseAttributeModifiers.isNullOrEmpty() && doNotMergeMobSpecificBaseMods)
            baseAttributeModifiers!!.clear()

        if (!mobSpecificBaseModifiers.isNullOrEmpty() && doNotMergeMobSpecificBaseMods)
            mobSpecificBaseModifiers!!.clear()
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
            dest: FineTuningAttributes,
            copyAll: Boolean
        ) {
            if (source.doNotMergeAllMultipliers && !copyAll)
                dest.multipliers?.clear()

            if (source.doNotMergeAllBaseMods && !copyAll)
                dest.baseAttributeModifiers?.clear()

            if (source.doNotMergeMobSpecificMultipliers && !copyAll)
                dest.mobSpecificMultipliers?.clear()

            if (source.doNotMergeMobSpecificBaseMods && !copyAll)
                dest.mobSpecificBaseModifiers?.clear()

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

        private fun formatMultipliers(
            items: MutableMap<Addition, Multiplier>?,
            sb: StringBuilder,
            header: String?,
            useCarriageReturn: Boolean,
            mobName: String?
        ) {
            if (items == null) return

            if (useCarriageReturn) sb.append("\n     ")
            if (header != null) {
                if (mobName != null)
                    sb.append("&r&l$header: ($mobName)&r ")
                else
                    sb.append("&r&l$header:&r ")
            }
            else
                sb.append("&r")

            var isFirst = true
            for (multiplier in items.values) {
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
            useCarriageReturn: Boolean,
            lmEntity: LivingEntityWrapper?
        ): Boolean {
            if (items == null) return false

            if (lmEntity != null){
                val mobSpecificItems = items[lmEntity.nameIfBaby]
                if (mobSpecificItems == null) return false

                formatMultipliers(
                    mobSpecificItems,
                    sb,
                    header,
                    useCarriageReturn,
                    lmEntity.nameIfBaby
                )

                return true
            }

            var isFirst = true
            if (useCarriageReturn) sb.append("\n     ")
            if (header != null)
                sb.append("&r&l$header&r: ")
            else
                sb.append("&r")

            for (mobName in items){
                if (isFirst)
                    isFirst = false
                else
                    sb.append(", ")

                var isFirstValues = true
                sb.append("&b${mobName.key}:&r ")
                for (mobValues in mobName.value){
                    if (isFirstValues)
                        isFirstValues = false
                    else
                        sb.append(", ")

                    sb.append(mobValues.value)
                }
            }

            return true
        }
    }

    override fun cloneItem(): Any {
        var copy: FineTuningAttributes? = null
        try {
            copy = super.clone() as FineTuningAttributes
            copyMultipliers(this, copy, true)
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

    override fun getEffectiveInfo(lmEntity: LivingEntityWrapper): String{
        val fineTunning = LevelledMobs.instance.rulesManager.getFineTuningAttributes(lmEntity)

        return fineTunning?.formatToString(lmEntity) ?: formatToString(lmEntity)
    }

    override fun toString(): String {
        return formatToString(null)
    }

    private fun formatToString(lmEntity: LivingEntityWrapper?): String{
        if (this.isEmpty) return "No items"

        val sb = StringBuilder()
        if (this.getUseStacked()) sb.append("(all stk) ")

        if (doNotMerge) {
            //if (sb.isNotEmpty()) sb.append(", ")
            sb.append("noMerge ")
        }

        var hadItems = false

        if (!multipliers.isNullOrEmpty()) {
            var skipMultipliers = false

            if (lmEntity != null && !mobSpecificMultipliers.isNullOrEmpty()
                && mobSpecificMultipliers!!.contains(lmEntity.nameIfBaby)){
                skipMultipliers = true
            }

            if (!skipMultipliers) {
                hadItems = true
                formatMultipliers(
                    multipliers,
                    sb,
                    null,
                    false,
                    null
                )
            }
        }

        if (!mobSpecificMultipliers.isNullOrEmpty()){
            hadItems = formatMobSpecific(
                mobSpecificMultipliers,
                sb,
                "mob specific",
                hadItems,
                lmEntity
            )
        }

        if (!baseAttributeModifiers.isNullOrEmpty()){
            var skipBaseMods = false

            if (lmEntity != null && !mobSpecificBaseModifiers.isNullOrEmpty()
                && mobSpecificBaseModifiers!!.contains(lmEntity.nameIfBaby)){
                skipBaseMods = true
            }

            if (!skipBaseMods) {
                formatMultipliers(
                    baseAttributeModifiers,
                    sb,
                    "base mods",
                    hadItems,
                    null
                )
                hadItems = true
            }
        }

        if (!mobSpecificBaseModifiers.isNullOrEmpty())
            formatMobSpecific(
                mobSpecificBaseModifiers,
                sb,
                "mob specific base mods",
                hadItems,
                lmEntity
            )

        return sb.toString()
    }
}