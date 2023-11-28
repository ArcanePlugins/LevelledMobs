package io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.impl.yaxis

import io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugCategory
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.setlevel.SetLevelAction
import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.LevellingStrategy
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.debug
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.war
import io.github.arcaneplugins.levelledmobs.bukkit.util.math.RangedInt
import org.spongepowered.configurate.CommentedConfigurationNode
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function
import kotlin.math.floor

class YAxisLevellingStrategy(
    minLevel: Int,
    maxLevel: Int,
    val yLevelTiersMap: MutableMap<RangedInt, RangedInt>,
    val defaultLevel: Int,
    val inverse: Boolean
) : LevellingStrategy("Y-Axis", minLevel, maxLevel) {
    init {
        var previousMax: Int? = null

        for (distanceRange in yLevelTiersMap.keys){
            if (previousMax != null && previousMax > distanceRange.min) {
                war("[Y-Axis] Tier overlap detected in " +
                            "'${distanceRange.min}-${distanceRange.max}'" +
                            ". This will cause issues!"
                )
            }

            previousMax = distanceRange.max
        }
    }

    override fun generate(
        context: Context
    ): Int? {
        if (context.livingEntity == null) return null
        val lent = context.livingEntity!!

        /*
        determine y-level tier
         */
        val y: Int = lent.location.blockY
        for (yLevelRanges in yLevelTiersMap.entries.stream().sorted().toList()){
            val yRange = yLevelRanges.key
            val levelRange = yLevelRanges.value

            debug(
                DebugCategory.Y_AXIS_STRATEGY
            ) {
                "Y range: ${yRange.min}-${yRange.max}, " +
                        "Level range: ${levelRange.min}-${levelRange.max}"
            }

            if (yRange.contains(y)) {
                val levelMin = levelRange.min
                val levelMax = levelRange.max
                val yMin = yRange.min
                val yMax = yRange.max
                return if (inverse) {
                    floor((
                        (levelMax - levelMin) * ((y - yMax) * 1.0f / (yMin - yMax)) + levelMin
                        ).toDouble()
                    ).toInt()
                } else {
                    floor((
                        (levelMax - levelMin) * ((y - yMin) * 1.0f / (yMax - yMin)) + levelMin
                        ).toDouble()
                    ).toInt()
                }
            }
        }

        // Mob's Y level was not factored in the tiers. Return default level instead
        return defaultLevel
    }

    override fun replaceInFormula(
        formula: String,
        context: Context
    ): String {
        val placeholder = "%y-axis-level%"
        if (!formula.contains(placeholder)) return formula
        val generatedLevel = generate(context) ?: return formula
        return formula.replace(placeholder, generatedLevel.toString())
    }

    companion object{
        fun parse(
            node: CommentedConfigurationNode
        ): YAxisLevellingStrategy{
            /*
            [YAML Structure - Example]

            strategies:
                spawn-distance:
                    tiers:
                        0-4998: 1-49
                        4999: 50-99
                        5000-10000: 100
                        undefined: 1
                        inverse: true
                    spawn-locations:
                        "example_world":
                            x: -153
                            z: 302
             */

            /*
            parse distance-level tiers
             */

            val parseRngInt = Function<Any, RangedInt>{ obj: Any ->
                when (obj){
                    is Int -> {
                        return@Function RangedInt(obj)
                    }
                    is String -> {
                        return@Function RangedInt(obj)
                    }
                    else -> {
                        throw IllegalArgumentException(
                            "[Y-Axis] Unable to parse ranged int as the " +
                                    "entry '$obj' is a '${obj.javaClass.getName()}', not a String or Integer"
                        )
                    }
                }
            }

            val yLevelTiersMap = mutableMapOf<RangedInt, RangedInt>()
            val tiersNodeMap = node.node("tiers").childrenMap()

            require(tiersNodeMap.isNotEmpty()) { "Y-Axis levelling strategy requires one or more Y-to-Level tiers." }

            val defaultLevel = AtomicInteger(SetLevelAction.getMinPossibleLevel())
            val inverse = AtomicBoolean(true)

            tiersNodeMap.forEach { (key: Any, value: CommentedConfigurationNode?) ->
                if (key.toString().equals("undefined", ignoreCase = true)) {
                    defaultLevel.set(value!!.int)
                } else if (key.toString().equals("inverse", ignoreCase = true)) {
                    inverse.set(value!!.boolean)
                } else {
                    yLevelTiersMap[parseRngInt.apply(key)] = parseRngInt.apply(value)
                }
            }

            /*
            build strategy object
             */

            // determine min and max level out of the distance-level tiers
            var minLevel: Int? = null
            var maxLevel: Int? = null

            for (levelRange in yLevelTiersMap.values){
                // determine min level
                if (minLevel == null) {
                    minLevel = levelRange.min
                } else {
                    if (minLevel > levelRange.min) minLevel = levelRange.min
                }

                // determine max level
                if (maxLevel == null) {
                    maxLevel = levelRange.max
                } else {
                    if (maxLevel > levelRange.max) maxLevel = levelRange.max
                }
            }

            if (minLevel == null || minLevel > defaultLevel.get()) {
                minLevel = defaultLevel.get()
            }

            if (maxLevel == null || maxLevel < defaultLevel.get()) {
                maxLevel = defaultLevel.get()
            }

            return YAxisLevellingStrategy(
                minLevel,
                maxLevel,
                yLevelTiersMap,
                defaultLevel.get(),
                inverse.get()
            )
        }
    }
}