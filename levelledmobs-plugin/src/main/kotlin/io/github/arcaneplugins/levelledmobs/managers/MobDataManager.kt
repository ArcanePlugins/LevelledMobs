package io.github.arcaneplugins.levelledmobs.managers

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.debug.DebugManager
import io.github.arcaneplugins.levelledmobs.debug.DebugType
import io.github.arcaneplugins.levelledmobs.enums.Addition
import io.github.arcaneplugins.levelledmobs.enums.AttributeNames
import io.github.arcaneplugins.levelledmobs.enums.VanillaBonusEnum
import io.github.arcaneplugins.levelledmobs.misc.CachedModalList
import io.github.arcaneplugins.levelledmobs.misc.StringReplacer
import io.github.arcaneplugins.levelledmobs.result.AttributePreMod
import io.github.arcaneplugins.levelledmobs.result.EvaluationResult
import io.github.arcaneplugins.levelledmobs.result.MultiplierResult
import io.github.arcaneplugins.levelledmobs.rules.FineTuningAttributes
import io.github.arcaneplugins.levelledmobs.util.Log
import io.github.arcaneplugins.levelledmobs.util.Utils
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import io.github.arcaneplugins.levelledmobs.wrappers.SchedulerWrapper
import java.util.Collections
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.EntityType
import redempt.crunch.Crunch
import redempt.crunch.functional.EvaluationEnvironment
import kotlin.math.max
import kotlin.math.min

/**
 * Manages data related to various mob levelling
 *
 * @author lokka30, stumper66
 * @since 2.6.0
 */
class MobDataManager {
    val vanillaMultiplierNames = mutableMapOf<String, VanillaBonusEnum>()
    var genericMaxHealth: Attribute? = null

    companion object {
        lateinit var instance: MobDataManager
            private set

        private val crunchEvalEnv = EvaluationEnvironment()

        fun evaluateExpression(
            expression: String
        ): EvaluationResult {
            var numberResult = 0.0
            var error: String? = null
            try{
                numberResult = Crunch.compileExpression(
                    expression, crunchEvalEnv
                ).evaluate()
            }
            catch (e: Exception){
                error = e.message
            }

            if (numberResult.isInfinite()){
                error = "Result was infinite"
                numberResult = 0.0
            }
            else if (numberResult.isNaN()){
                error = "Result was NaN (not a number)"
                numberResult = 0.0
            }

            return EvaluationResult(
                numberResult,
                error
            )
        }

        fun populateAttributeCache(
            lmEntity: LivingEntityWrapper,
            whichOnes: MutableList<Attribute>? = null
        ){
            val result = mutableMapOf<Attribute, AttributeInstance>()
            val useList: MutableList<Attribute> = whichOnes ?: Utils.getAllAttributes()

            for (attribute in useList){
                val attribInstance = lmEntity.livingEntity.getAttribute(attribute)
                if (attribInstance != null)
                    result[attribute] = attribInstance
            }

            lmEntity.attributeValuesCache = result
        }
    }

    init {
        instance = this
        this.vanillaMultiplierNames.putAll(mapOf(
            "Armor modifier" to VanillaBonusEnum.ARMOR_MODIFIER,
            "Armor toughness" to VanillaBonusEnum.ARMOR_TOUGHNESS,
            "Attacking speed boost" to VanillaBonusEnum.ATTACKING_SPEED_BOOST,
            "Baby speed boost" to VanillaBonusEnum.BABY_SPEED_BOOST,
            "Covered armor bonus" to VanillaBonusEnum.COVERED_ARMOR_BONUS,
            "Drinking speed penalty" to VanillaBonusEnum.DRINKING_SPEED_PENALTY,
            "Fleeing speed boost" to VanillaBonusEnum.FLEEING_SPEED_BOOST,
            "Horse armor bonus" to VanillaBonusEnum.HORSE_ARMOR_BONUS,
            "Knockback resistance" to VanillaBonusEnum.KNOCKBACK_RESISTANCE,
            "Leader zombie bonus" to VanillaBonusEnum.LEADER_ZOMBIE_BONUS,
            "Random spawn bonus" to VanillaBonusEnum.RANDOM_SPAWN_BONUS,
            "Random zombie-spawn bonus" to VanillaBonusEnum.RANDOM_ZOMBIE_SPAWN_BONUS,
            "Sprinting speed boost" to VanillaBonusEnum.SPRINTING_SPEED_BOOST,
            "Tool modifier" to VanillaBonusEnum.TOOL_MODIFIER,
            "Weapon modifier" to VanillaBonusEnum.WEAPON_MODIFIER,
            "Zombie reinforcement caller charge" to VanillaBonusEnum.ZOMBIE_REINFORCE_CALLER,
            "Zombie reinforcement callee charge" to VanillaBonusEnum.ZOMBIE_REINFORCE_CALLEE
        ))
        /*
        Syntax: between(num, lower, upper)

        Returns `1` if `num` is between (inclusive) `lower` and `upper`, else, returning `0`.

        Example: between(7, 1, 5) -> 0
            (7 is not between 1 and 5 (inclusive), so 0 (false) was returned.)
         */
        crunchEvalEnv.addFunction("between", 3) { d: DoubleArray ->
            if ((d[0] >= d[1] && d[0] <= d[2])
            ) 1.0 else 0.0
        }

        /*
        Syntax: clamp(num, lower, upper)

        Returns `num` although increasing it / reducing it to be in between (inclusive) the
        `lower` and `upper` bounds.

        Example: clamp(7, 1, 5) -> 5
            (7 is larger than the upper bound of 5, so 5 was returned.)
         */
        crunchEvalEnv.addFunction("clamp", 3) { d: DoubleArray ->
            min(
                d[2],
                max(d[0], d[1])
            )
        }
    }

    fun isLevelledDropManaged(
        material: Material
    ): Boolean {
        // Head drops
        val main = LevelledMobs.instance
        if (material.toString().endsWith("_HEAD") || material.toString().endsWith("_SKULL"))
            return main.helperSettings.getBoolean( "mobs-multiply-head-drops")

        return false
    }

    fun prepareSetAttributes(
        lmEntity: LivingEntityWrapper,
        attribute: Attribute,
        addition: Addition
    ): AttributePreMod? {
        val attribInstance = lmEntity.attributeValuesCache?.get(attribute) ?: return null

        val defaultValue = attribInstance.baseValue.toFloat()
        val multiplierResult = getAdditionsForLevel(lmEntity, addition, defaultValue)
        val additionValue = multiplierResult.multiplierAmount
        if (additionValue == 0.0f && multiplierResult.baseModAmount == null)
            return null

        val modifierOperation = if (multiplierResult.isAddition)
            AttributeModifier.Operation.ADD_NUMBER
        else
            AttributeModifier.Operation.MULTIPLY_SCALAR_1

        @Suppress("removal", "DEPRECATION")
        val mod: AttributeModifier = if (LevelledMobs.instance.ver.useOldEnums) {
            val attributeEnum = attribute as Enum<*>
            AttributeModifier(attributeEnum.name, additionValue.toDouble(), modifierOperation)
        }
        else{
            val equipmentSlotGroupANY = LevelledMobs.instance.definitions.fieldEquipmentSlotAny!!.get(null)
            LevelledMobs.instance.definitions.ctorAttributeModifier!!.newInstance(
                attribute.key, additionValue.toDouble(), modifierOperation, equipmentSlotGroupANY
            ) as AttributeModifier
        }

        // if zombified piglins get this attribute applied, they will spawn in zombies in the nether
        if (lmEntity.entityType == EntityType.ZOMBIFIED_PIGLIN &&
            attribute.toString() == Utils.getAttribute(AttributeNames.SPAWN_REINFORCEMENTS)!!.toString()
        ) {
            return null
        }

        return AttributePreMod(
            mod,
            multiplierResult,
            attribute
        )
    }

    fun setAttributeMods(
        lmEntity: LivingEntityWrapper,
        attribInfos: MutableList<AttributePreMod>
    ) {
        if (genericMaxHealth == null)
            genericMaxHealth = Utils.getAttribute(AttributeNames.MAX_HEALTH)

        for (info in attribInfos){
            val additionValue = info.multiplierResult.multiplierAmount
            val attrib = lmEntity.livingEntity.getAttribute(info.attribute) ?: return

            // if zombified piglins get this attribute applied, they will spawn in zombies in the nether
            if (lmEntity.entityType == EntityType.ZOMBIFIED_PIGLIN &&
                info.attribute.toString() == Utils.getAttribute(AttributeNames.SPAWN_REINFORCEMENTS)!!.toString()
            ) {
                continue
            }

            var existingDamagePercent: Float? = null

            if (info.attribute == genericMaxHealth)
                existingDamagePercent = getExistingDamagePercent(info, lmEntity)

            if (info.multiplierResult.baseModAmount != null){
                val oldValue = attrib.baseValue
                attrib.baseValue = info.multiplierResult.baseModAmount.toDouble()

                if (info.attribute == genericMaxHealth && additionValue == 0.0f)
                    checkHealth(lmEntity, attrib, existingDamagePercent)

                DebugManager.log(DebugType.APPLY_BASE_MODIFIERS, lmEntity) {
                    "attrib: ${info.attribute}, old base: ${Utils.round(oldValue, 3)}, " +
                            "new base: ${Utils.round(info.multiplierResult.baseModAmount.toDouble(), 3)}"
                }
            }

            if (additionValue == 0.0f) continue

            removeExistingMultipliers(lmEntity, attrib)
            attrib.addModifier(info.attributeModifier)

            DebugManager.log(DebugType.APPLY_MULTIPLIERS, lmEntity) {
                "attrib: ${info.attribute}, base: ${Utils.round(attrib.baseValue, 3)}, " +
                        "addtion: ${Utils.round(additionValue.toDouble(), 3)}"
            }

            if (info.attribute == genericMaxHealth)
                checkHealth(lmEntity, attrib, existingDamagePercent)
        }
    }

    private fun getExistingDamagePercent(
        info: AttributePreMod,
        lmEntity: LivingEntityWrapper
    ): Float? {
        val maxHealth = lmEntity.livingEntity.getAttribute(info.attribute)
        if (maxHealth == null) return null

        val existingDamage = maxHealth.value - lmEntity.livingEntity.health
        return if (existingDamage > 0.0)
            (maxHealth.value.toFloat() - existingDamage.toFloat()) / maxHealth.value.toFloat()
        else
            null
    }

    private fun checkHealth(
        lmEntity: LivingEntityWrapper,
        attrib: AttributeInstance,
        existingDamagePercent: Float?
    ){
        // MAX_HEALTH specific: set health to max health
        val newHealth = if (existingDamagePercent != null)
            (attrib.value * existingDamagePercent).toFloat()
        else
            attrib.value.toFloat()
        try {
            if (lmEntity.livingEntity.health <= 0.0) return
            lmEntity.livingEntity.health = newHealth.toDouble().coerceAtLeast(1.0)
        } catch (e: IllegalArgumentException) {
            DebugManager.log(DebugType.APPLY_MULTIPLIERS, lmEntity){
                "Error setting maxhealth = $newHealth, ${e.message}"
            }
        }
    }

    private fun removeExistingMultipliers(
        lmEntity: LivingEntityWrapper,
        attrib: AttributeInstance
    ){
        val allowedVanillaBonusEnums: CachedModalList<VanillaBonusEnum> =
            LevelledMobs.instance.rulesManager.getAllowedVanillaBonuses(lmEntity)
        val existingMods = Collections.enumeration(attrib.modifiers)
        while (existingMods.hasMoreElements()) {
            val existingMod = existingMods.nextElement()
            val vanillaBonusEnum = vanillaMultiplierNames[existingMod.name]
            if (vanillaBonusEnum != null) {
                if (allowedVanillaBonusEnums.isEmpty() || allowedVanillaBonusEnums.isIncludedInList(
                        vanillaBonusEnum,
                        lmEntity
                    )
                ) {
                    continue
                }
            }

            if (!existingMod.name.startsWith("GENERIC_")) {
                DebugManager.log(DebugType.REMOVED_MULTIPLIERS, lmEntity) {
                    val locationStr = "${lmEntity.location.blockX},${lmEntity.location.blockY},${lmEntity.location.blockZ}"
                    "Removing ${existingMod.name} at $locationStr"
                }
            }

            attrib.removeModifier(existingMod)
        }
    }

    fun getAllAttributeValues(lmEntity: LivingEntityWrapper, whichOnes: MutableList<Attribute>? = null){
        if (LevelledMobs.instance.ver.isRunningFolia || Bukkit.isPrimaryThread()){
            populateAttributeCache(lmEntity, whichOnes)
            return
        }

        val completableFuture = CompletableFuture<Boolean>()
        val scheduler = SchedulerWrapper(lmEntity.livingEntity){
            populateAttributeCache(lmEntity, whichOnes)
            completableFuture.complete(true)
            lmEntity.free()
        }

        lmEntity.inUseCount.getAndIncrement()
        scheduler.entity = lmEntity.livingEntity
        scheduler.run()

        completableFuture.get(5000L, TimeUnit.MILLISECONDS)
    }

    fun getAdditionsForLevel(
        lmEntity: LivingEntityWrapper,
        addition: Addition,
        defaultValue: Float
    ): MultiplierResult {
        val maxLevel = LevelledMobs.instance.rulesManager.getRuleMobMaxLevel(lmEntity).toFloat()
        val fineTuning = lmEntity.fineTuningAttributes
        var attributeMultiplier: FineTuningAttributes.Multiplier? = null
        var attributeMax = 0f
        var multiplierValue = 0f
        var baseModifierAmount: Float? = null
        var isAddition = true

        if (fineTuning != null) {
            attributeMultiplier = fineTuning.getMultiplier(addition, lmEntity)

            for (loop in 0..1){
                // loop 0 = multipliers, loop 1 = base modifiers
                val multiplierOrMod = if (loop == 0)
                    attributeMultiplier
                else
                    fineTuning.getBaseModifier(addition, lmEntity)

                if (multiplierOrMod == null) continue

                if (multiplierOrMod.hasFormula){
                    isAddition = multiplierOrMod.isAddition
                    val formulaStr = StringReplacer(multiplierOrMod.formula!!)
                    formulaStr.replaceIfExists("%level%"){ lmEntity.getMobLevel.toString() }
                    formulaStr.text = LevelledMobs.instance.levelManager.replaceStringPlaceholdersForFormulas(
                        formulaStr.text,
                        lmEntity
                    )

                    val evalResult = evaluateExpression(formulaStr.text)
                    multiplierValue = evalResult.result.toFloat()
                    if (evalResult.hadError)
                        Log.war("Error evaluating formula for ${lmEntity.nameIfBaby}: '$formulaStr', ${evalResult.error}")

                    DebugManager.log(DebugType.APPLY_MULTIPLIERS, lmEntity, !evalResult.hadError) {
                        "${multiplierOrMod.addition.name}, formulaPre: '${multiplierOrMod.formula}'\nformula: " +
                                "'$formulaStr', result: '$multiplierValue'" }
                }
                else {
                    if (loop == 0)
                        multiplierValue = multiplierOrMod.useValue
                    else
                        baseModifierAmount = multiplierOrMod.useValue
                }
            }

            attributeMax = when (addition) {
                Addition.ATTRIBUTE_ARMOR_BONUS -> 30.0f
                Addition.ATTRIBUTE_ARMOR_TOUGHNESS -> 50.0f
                Addition.ATTRIBUTE_ATTACK_KNOCKBACK -> 5.0f
                Addition.ATTRIBUTE_KNOCKBACK_RESISTANCE, Addition.ATTRIBUTE_ZOMBIE_SPAWN_REINFORCEMENTS -> 1.0f
                else -> 0.0f
            }
        }

        if (maxLevel == 0f || multiplierValue == 0.0f) {
            DebugManager.log(DebugType.APPLY_MULTIPLIERS, lmEntity) {
                val msg = if (maxLevel == 0f) "maxLevel was 0" else "multiplier was 0"
                "$msg; returning 0 for $addition"
            }
            return MultiplierResult(0.0f, baseModifierAmount, isAddition)
        }

        if (attributeMultiplier?.hasFormula == true)
            return MultiplierResult(multiplierValue, baseModifierAmount, isAddition)

        if ((addition == Addition.CUSTOM_ITEM_DROP || addition == Addition.CUSTOM_XP_DROP)
            && multiplierValue == -1f
        ) return MultiplierResult(Float.MIN_VALUE, baseModifierAmount, isAddition)

        if (fineTuning!!.getUseStacked() || attributeMultiplier!!.useStacked) {
            DebugManager.log(DebugType.APPLY_MULTIPLIERS, lmEntity) {
                "attrib: ${addition.name}, stkd formula, ${attributeMultiplier!!.value}"
            }
            return MultiplierResult(lmEntity.getMobLevel.toFloat() * multiplierValue, baseModifierAmount, isAddition)
        } else {
            DebugManager.log(DebugType.APPLY_MULTIPLIERS, lmEntity) {
                "attrib: ${addition.name}, std formula, ${attributeMultiplier.value}"
            }

            multiplierValue = if (attributeMax > 0.0) {
                // only used for 5 specific attributes
                lmEntity.getMobLevel / maxLevel * (attributeMax * multiplierValue)
            } else {
                // normal formula for most attributes
                defaultValue * multiplierValue * ((lmEntity.getMobLevel) / maxLevel)
            }
            return MultiplierResult(multiplierValue, baseModifierAmount, isAddition)
        }
    }
}