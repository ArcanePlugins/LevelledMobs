package io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.impl.spawndistance

import io.github.arcaneplugins.levelledmobs.bukkit.api.util.Pair
import io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugCategory
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.setlevel.SetLevelAction
import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.LevellingStrategy
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.debug
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.war
import io.github.arcaneplugins.levelledmobs.bukkit.util.math.RangedInt
import org.bukkit.World
import org.spongepowered.configurate.CommentedConfigurationNode
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt

class SpawnDistanceLevellingStrategy(
    minLevel: Int,
    maxLevel: Int,
    val distanceLevelTiersMap: MutableMap<RangedInt, RangedInt>,
    val defaultLevel: Int,
    val spawnLocationsMap: MutableMap<String, Pair<Int, Int>>
): LevellingStrategy("Spawn-Distance", minLevel, maxLevel) {
    init {
        // warn user if distance keys overlap
        var previousMax: Int? = null
        for (distanceRange: RangedInt in distanceLevelTiersMap.keys) {
            if (previousMax != null && previousMax > distanceRange.min) {
                war(
                    "[Spawn-Distance] Tier overlap detected in '" +
                            "${distanceRange.min}-${distanceRange.max}" +
                            "'. This will cause issues!"
                )
            }
            previousMax = distanceRange.max
        }
    }

    override fun generate(context: Context): Int? {
        if (context.livingEntity == null) return null

        /*
        determine spawn location
         */
        val lent = context.livingEntity!!
        val world: World = lent.world

        val spawnXZ: Pair<Int, Int> = spawnLocationsMap.computeIfAbsent(
            world.name
        ) {
            val spawnLocation = world.spawnLocation
            Pair<Int, Int>(
                spawnLocation.blockX,
                spawnLocation.blockY
            )
        }
        val spawnX = spawnXZ.left
        val spawnZ = spawnXZ.right
        /*
        determine distance from spawn
         */

        val lentX = lent.location.blockX
        val lentZ = lent.location.blockZ

        val distance = floor(
            sqrt(
                ((lentX - spawnX).toDouble()).pow(2) +
                        ((lentZ - spawnZ).toDouble()).pow(2)
            )
        ).toInt()
        /*
        determine distane-level tier
         */
        for (distanceLevelRanges in distanceLevelTiersMap.entries.stream().sorted().toList()){
            val distanceRange = distanceLevelRanges.key
            val levelRange = distanceLevelRanges.value

            debug(DebugCategory.SPAWN_DISTANCE_STRATEGY) {
                "Distance range: ${distanceRange.min}-${distanceRange.max}, Level range: ${levelRange.min}-${levelRange.max}"
            }

            if (distanceRange.contains(distance)) {
                val distanceRatio = (distance - distanceRange.min) * 1.0f /
                        (distanceRange.max - distanceRange.min)
                val levelMin = levelRange.min
                val levelMax = levelRange.max
                return floor(((levelMax - levelMin) * distanceRatio + levelMin).toDouble()).toInt()
            }
        }

        // Mob's distance from spawn was not factored in the tiers. Return default level instead
        return defaultLevel
    }

    override fun replaceInFormula(
        formula: String,
        context: Context
    ): String {
        val placeholder = "%spawn-distance-level%"
        if (!formula.contains(placeholder)) return formula
        val generatedLevel = generate(context) ?: return formula
        return formula.replace(placeholder, generatedLevel.toString())
    }

    companion object {
        fun parse(
            node: CommentedConfigurationNode
        ): SpawnDistanceLevellingStrategy {
            /*
        [YAML Structure - Example]

        strategies:
            spawn-distance:
                tiers:
                    0-4998: 1-49
                    4999: 50-99
                    5000-10000: 100
                    undefined: 1
                spawn-locations:
                    "example_world":
                        x: -153
                        z: 302
         */

            /*
        parse distance-level tiers
         */

            /*
        *         val langToPathFun =
            Function<String, Path> { langInput: String ->
                Path.of(
                    "${LevelledMobs.lmInstance.dataFolder}${File.separator}translations" +
                            "${File.separator}$langInput.yml"
                )
            }
        * */

            val parseRngInt = Function<Any, RangedInt> { obj: Any ->
                when (obj) {
                    is Int -> {
                        return@Function RangedInt(obj)
                    }

                    is String -> {
                        return@Function RangedInt(obj)
                    }

                    else -> {
                        throw IllegalArgumentException(
                            "[Spawn-Distance] Unable to parse ranged int as the " +
                                    "entry '$obj' is a '${obj.javaClass.getName()}', not a String or Integer"
                        )
                    }
                }
            }

            val distanceLevelTiersMap = mutableMapOf<RangedInt, RangedInt>()
            val tiersNodeMap = node.node("tiers").childrenMap()

            require(tiersNodeMap.isNotEmpty()) { "Spawn distance strategy requires one or more distance-to-level tiers." }

            val defaultLevel = AtomicInteger(SetLevelAction.getMinPossibleLevel())

            tiersNodeMap.forEach { (key: Any, value: CommentedConfigurationNode?) ->
                if (key.toString().equals("undefined", ignoreCase = true)) {
                    defaultLevel.set(value!!.int)
                } else {
                    distanceLevelTiersMap[parseRngInt.apply(key)] = parseRngInt.apply(value!!)
                }
            }

            /*
        parse spawn locations
         */

            val spawnLocationsMap = mutableMapOf<String, Pair<Int, Int>>()

            node.node("spawn-locations").childrenMap().forEach { (key: Any, value: CommentedConfigurationNode) ->
                spawnLocationsMap[key.toString()] = Pair(
                    value.node("x").int,
                    value.node("z").int
                )
            }

            /*
        build strategy object
         */

            // determine min and max level out of the distance-level tiers
            var minLevel: Int? = null
            var maxLevel: Int? = null

            for (levelRange in distanceLevelTiersMap.values) {
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

            if (minLevel!! > defaultLevel.get()) {
                minLevel = defaultLevel.get()
            }

            if (maxLevel!! < defaultLevel.get()) {
                maxLevel = defaultLevel.get()
            }

            return SpawnDistanceLevellingStrategy(
                minLevel,
                maxLevel,
                distanceLevelTiersMap,
                defaultLevel.get(),
                spawnLocationsMap
            )
        }
    }
}