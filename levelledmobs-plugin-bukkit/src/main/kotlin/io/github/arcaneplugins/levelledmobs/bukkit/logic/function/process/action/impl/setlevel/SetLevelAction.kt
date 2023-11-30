package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.setlevel

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil.setInheritanceBreedingFormula
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil.setInheritanceTransformationFormula
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil.setLevel
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil.setMaxLevel
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil.setMinLevel
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil.unlevelMob
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler.evaluateExpression
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler.replacePapiAndContextPlaceholders
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.setlevel.inheritance.DifferingFormulaResolveType
import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.LevelTuple
import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.LevellingStrategy
import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.LevellingStrategyRequestEvent
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log
import io.github.arcaneplugins.levelledmobs.bukkit.util.StringUtils.emptyIfNull
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.spongepowered.configurate.CommentedConfigurationNode
import java.lang.IllegalArgumentException
import java.util.concurrent.ThreadLocalRandom
import java.util.function.Function
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class SetLevelAction(
    val process: Process,
    val node: CommentedConfigurationNode
): Action(process, node) {
    val formula: String
    val strategies = mutableSetOf<LevellingStrategy>()
    private var useInheritanceIfAvailable: Boolean
    private val inheritanceBreedingFormula: String
    private val inheritanceTransformationFormula: String

    init {
        formula = actionNode.node("formula")
            .getString("no-level")

        useInheritanceIfAvailable = actionNode
            .node("inheritance", "use-if-available")
            .getBoolean(false)

        inheritanceBreedingFormula = actionNode
            .node("inheritance", "breeding", "formula")
            .getString("(%father-level% + %mother-level%) / 2")

        inheritanceTransformationFormula = actionNode
            .node("inheritance", "transformation", "formula")
            .getString("%mother-level%")

        /*
        Here we want to call out for all known levelling strategies to be registered to the
        SetLevelAction.
         */

        for (strategyNodeEntry in actionNode
            .node("strategies")
            .childrenMap().entries){
            val strategyNode = strategyNodeEntry.value

            requireNotNull(strategyNodeEntry.key) { "Strategy keys must not be null" }
            require(strategyNodeEntry.key is String) { "Strategy keys must be of type String" }
            val strategyId = strategyNodeEntry.key as String

            // fire LevellingStrategyRequestEvent
            val stratReqEvent = LevellingStrategyRequestEvent(strategyId, strategyNode)

            Bukkit.getPluginManager().callEvent(stratReqEvent)

            if (stratReqEvent.cancelled) continue

            // add all strategies from the events
            strategies.addAll(stratReqEvent.strategies)
        }

        if(!formula.equals("no-level", ignoreCase = true) && strategies.isEmpty()) {
            throw IllegalArgumentException(
                "SetLevelAction requres at least 1 levelling strategy, unless specifying " +
                    "no-level. " +
                    "For a simple context-based formula, you can use the Basic Levelling Strategy."
            )
        }
    }

    override fun run(context: Context) {
        requireNotNull(context.livingEntity) {"Requires entity context; missing."}
        val lent = context.livingEntity!!

        if (EntityDataUtil.isLevelled(lent, true)) {
            // Looks like the mob was previously levelled, so we need to remove most of the LM stuff
            unlevelMob(lent)
        }

        var result = generateInheritedLevels(context)
        if (result == null) result = generateStandardLevels(context)

        // no level = remove it if it exists
        if (result == null) {
            unlevelMob(lent)
            return
        }

        setMinLevel(lent, result.minLevel, true)
        setLevel(lent, result.level, true)
        setMaxLevel(lent, result.maxLevel, true)

        // apply inheritance formulas to (parent) entity.
        if (useInheritanceIfAvailable) {
            setInheritanceBreedingFormula(lent,inheritanceBreedingFormula, true)
            setInheritanceTransformationFormula(lent,inheritanceTransformationFormula, true)
        }
    }

    private fun generateStandardLevels(context: Context): LevelTuple? {
        return processFormula(context)
    }

    private fun generateInheritedLevels(context: Context): LevelTuple?{
        if (!useInheritanceIfAvailable) return null

        requireNotNull(context.livingEntity){ "LivingEntity context is required" }

        val lent = context.livingEntity!!
        val father = EntityDataUtil.getFather(lent, false)
        val mother = EntityDataUtil.getMother(lent, false)

        /*
        Entity Breeding Level Inheritance
         */
        if (true == EntityDataUtil.wasBred(lent, true)) {
            if (father == null || mother == null) return null
            context
                .withFather(father)
                .withMother(mother)

            val fatherFormula = emptyIfNull(
                EntityDataUtil.getInheritanceBreedingFormula(father, true)
            )
            val motherFormula = emptyIfNull(
                EntityDataUtil.getInheritanceBreedingFormula(mother, true)
            )

            // skip if both are null
            if (fatherFormula.isBlank() && motherFormula.isBlank()) {
                return null
            }

            // skip if both formulas are 'no-level'
            if (fatherFormula.equals("no-level", ignoreCase = true) &&
                motherFormula.equals("no-level", ignoreCase = true)
            ) {
                return null
            }

            val levelEvaluator =
                Function { formula: String ->
                    if (formula.isBlank() || formula.equals(
                            "no-level",
                            ignoreCase = true
                        )
                    ) return@Function getMinPossibleLevel()
                    try {
                        return@Function floor(
                            evaluateExpression(
                                replacePapiAndContextPlaceholders(formula, context)
                            )
                        ).toInt()
                    } catch (ex: Exception) {
                        throw RuntimeException(ex)
                    }
                }

            val fatherInheritedLevel = levelEvaluator.apply(fatherFormula)
            val motherInheritedLevel = levelEvaluator.apply(motherFormula)

            val minLevel: Int
            val fatherMinLevel = EntityDataUtil
                .getMinLevel(father, true)
            val motherMinLevel = EntityDataUtil
                .getMinLevel(mother, true)

            val maxLevel: Int
            val fatherMaxLevel = EntityDataUtil
                .getMaxLevel(father, true)
            val motherMaxLevel = EntityDataUtil
                .getMaxLevel(mother, true)

            minLevel = if (fatherMinLevel == null && motherMinLevel == null) {
                return null
            } else (if (fatherMinLevel != null && motherMinLevel != null) {
                min(fatherMinLevel, motherMinLevel)
            } else {
                fatherMinLevel ?: motherMinLevel
            })!!

            maxLevel = if (fatherMaxLevel == null && motherMaxLevel == null) {
                return null
            } else if (fatherMaxLevel != null && motherMaxLevel != null) {
                min(fatherMaxLevel, motherMaxLevel)
            } else {
                fatherMaxLevel ?: motherMaxLevel
            }!!

            // resolve differing formulas
            if (!fatherFormula.equals(motherFormula, ignoreCase = true)) {
                return when (DifferingFormulaResolveType.getFromAdvancedSettings()) {
                    DifferingFormulaResolveType.USE_AVERAGE -> LevelTuple(
                        minLevel,
                        (fatherInheritedLevel + motherInheritedLevel) / 2,
                        maxLevel
                    )

                    DifferingFormulaResolveType.USE_RANDOM -> LevelTuple(
                        minLevel,
                        if (ThreadLocalRandom.current().nextBoolean()) fatherInheritedLevel else motherInheritedLevel,
                        maxLevel
                    )

                    DifferingFormulaResolveType.USE_NEITHER -> null
                }
            }

            // yes, we are ignoring fatherLevel since it should be the same
            return LevelTuple(minLevel, motherInheritedLevel, maxLevel)
        }

        /*
        Entity Transformation Level Inheritance
         */
        if (true == EntityDataUtil.wasTransformed(lent, true)){
            // during transformation, mother == father. we only check for one.
            if (mother == null) return null

            // yes: it is intentional the father is the same as the mother during transformation.
            context
                .withFather(mother)
                .withMother(mother)

            if (!EntityDataUtil.isLevelled(mother, true)) return null

            val formula = emptyIfNull(
                EntityDataUtil
                    .getInheritanceTransformationFormula(mother, true)
            )

            if (formula.isBlank() || formula.equals("no-level", ignoreCase = true)) {
                return null
            }

            return LevelTuple(
                EntityDataUtil.getMinLevel(father!!, true)!!,
                floor(
                    evaluateExpression(
                        replacePapiAndContextPlaceholders(formula, context)
                    )
                ).toInt(),
                EntityDataUtil.getMaxLevel(father, true)!!
            )
        }

        /*
        Passenger/Vehicle Level Inheritance
         */
        var vehicleEntity: Entity = lent
        while (lent.isInsideVehicle){
            if (vehicleEntity is LivingEntity) {
                if (EntityDataUtil.isLevelled(vehicleEntity, true)) {
                    return LevelTuple(
                        EntityDataUtil.getMinLevel(vehicleEntity, true)!!,
                        EntityDataUtil.getLevel(vehicleEntity, true)!!,
                        EntityDataUtil.getMaxLevel(vehicleEntity, true)!!
                    )
                }
            }

            if (!vehicleEntity.isInsideVehicle) continue
            vehicleEntity = vehicleEntity.vehicle!!
        }

        // No level could be inherited, so return null.
        return null
    }

    fun processFormula(context: Context): LevelTuple?{
        // check if the mob should have no level

        // check if the mob should have no level
        if (formula.equals("no-level", ignoreCase = true)) {
            // remember:    null = no level
            return null
        }

        // replace context placeholders in the formula
        var formula = replacePapiAndContextPlaceholders(formula, context)

        var minLevel = getMinPossibleLevel()
        var maxLevel = getMinPossibleLevel()

        // replace levelling strategy placeholders in the formula
        for (strategy in strategies) {
            formula = strategy.replaceInFormula(formula, context)
            minLevel = minLevel.coerceAtMost(strategy.minLevel)
            maxLevel = maxLevel.coerceAtLeast(strategy.maxLevel)
        }

        if (maxLevel < minLevel) maxLevel = minLevel

        // evaluate the formula with Crunch
        val levelEval = Math.round(evaluateExpression(formula)).toInt()

        // finally, ensure the evaluated level is between the min and max levels.
        val level = min(max(levelEval.toDouble(), minLevel.toDouble()), maxLevel.toDouble()).toInt()

        return LevelTuple(minLevel, level, maxLevel)
    }

    companion object{
        // TODO let's move this into a more accessible area - SettingsCfg class?
        fun getMinPossibleLevel(): Int{
            // we don't want negative values as they create undefined game behaviour
            return 0.coerceAtLeast(
                LevelledMobs.lmInstance
                    .configHandler.settingsCfg
                    .root!!.node("advanced", "minimum-level").getInt(1)
            )
        }
    }
}