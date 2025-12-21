package io.github.arcaneplugins.levelledmobs.rules.strategies

import java.util.concurrent.ThreadLocalRandom
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.debug.DebugManager
import io.github.arcaneplugins.levelledmobs.debug.DebugType
import io.github.arcaneplugins.levelledmobs.rules.RulesManager
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import org.bukkit.Location

/**
 * Holds the configuration and logic for applying a levelling system that is based upon the distance
 * from the world spawn
 *
 * @author stumper66, lokka30
 * @since 3.0.0
 */
class SpawnDistanceStrategy : LevellingStrategy, Cloneable{
    var bufferDistance: Float? = null
    var ringedTiers: Float? = null
    var originCoordX: Float? = null
    var originCoordZ: Float? = null
    var enableHeightMod: Boolean? = null
    var transitionYheight: Float? = null
    var yHeightPeriod: Float? = null
    var lvlMultiplier: Float? = null
    var scaleDownward: Boolean? = null

    override val strategyType = StrategyType.SPAWN_DISTANCE
    override var shouldMerge: Boolean = false

    override fun generateNumber(
        lmEntity: LivingEntityWrapper,
        minLevel: Int,
        maxLevel: Int
    ): Float {
        var spawnLocation = lmEntity.world.spawnLocation

        if (this.originCoordZ != null || this.originCoordX != null) {
            val useX =
                if (this.originCoordX == null) spawnLocation.x else originCoordX!!.toDouble()
            val useZ =
                if (this.originCoordZ == null) spawnLocation.x else originCoordZ!!.toDouble()

            spawnLocation = Location(
                lmEntity.livingEntity.world,
                useX,
                spawnLocation.y,
                useZ
            )
        }

        if (spawnLocation.world != lmEntity.location.world) return minLevel.toFloat()

        val bufferDistance = if (this.bufferDistance == null) 0f else bufferDistance!!
        val distanceFromSpawn = spawnLocation.distance(lmEntity.location).toFloat()
        val levelDistance = (distanceFromSpawn - bufferDistance).coerceAtLeast(0f)
        val variance = LevelledMobs.instance.rulesManager.getRuleMaxRandomVariance(lmEntity)
        var varianceAdded = 0
        if (variance != null)
            varianceAdded = ThreadLocalRandom.current().nextInt(0, variance + 1)

        var ringedTiers =
            if (this.ringedTiers == null) 1f else ringedTiers!!
        if (ringedTiers == 0f)
            ringedTiers = 1f

        //Get the level thats meant to be at a given distance
        var spawnDistanceAssignment =
            ((levelDistance / ringedTiers) + varianceAdded)

        if (spawnDistanceAssignment.isNaN()){
            DebugManager.log(DebugType.STRATEGY_RESULT, lmEntity) {
                "SpawnDistanceStrategy generated NaN, levelDistance: $levelDistance, increaseLevelDistance: $ringedTiers"
            }
            spawnDistanceAssignment = 0f
        }

        if (!heightModIsEnabled)
            return spawnDistanceAssignment

        return generateBlendedLevel(lmEntity, spawnDistanceAssignment)
    }

    private val heightModIsEnabled: Boolean
        get() = enableHeightMod != null && enableHeightMod!!

    override fun mergeRule(levellingStrategy: LevellingStrategy?) {
        if (levellingStrategy == null) return
        if (levellingStrategy is SpawnDistanceStrategy)
            mergeSpawnDistanceStrategy(levellingStrategy as SpawnDistanceStrategy?)
    }

    private fun mergeSpawnDistanceStrategy(sds: SpawnDistanceStrategy?) {
        if (sds == null) return

        try {
            for (f in sds.javaClass.declaredFields) {
                if (f.name == "strategyType") continue
                if (f[sds] == null) continue

                this.javaClass.getDeclaredField(f.name)[this] = f[sds]
            }
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()
        if (heightModIsEnabled) {
            sb.append(
                "blended, dro: ${if (bufferDistance == null) 0 else bufferDistance}, " +
                "rt: ${if (ringedTiers == null) 0 else ringedTiers}, " +
                "t_yHght: ${if (transitionYheight == null) 0 else transitionYheight}, " +
                "yhp: ${if (yHeightPeriod == null) 0 else yHeightPeriod}, " +
                "lvlMlp: ${if (lvlMultiplier == null) 0.0 else lvlMultiplier}, " +
                "scdown: ${scaleDownward == null || scaleDownward!!}"
            )
        } else {
            sb.append(
                "spawn distance, dro: ${if (bufferDistance == null) 0 else bufferDistance}, " +
                "rt: ${if (ringedTiers == null) 0 else ringedTiers}"
            )
        }

        if (this.originCoordX != null)
            sb.append(" x: ").append(this.originCoordX)

        if (this.originCoordZ != null)
            sb.append(" z: ").append(this.originCoordZ)

        return sb.toString()
    }

    private fun generateBlendedLevel(
        lmEntity: LivingEntityWrapper,
        spawnDistanceLevelAssignment: Float
    ): Float {
        val currentYPos = lmEntity.location.blockY.toFloat()
        var result: Float
        val transitionYHeight =
            if (this.transitionYheight == null) 0f else transitionYheight!!
        val yHeightPeriod = if (this.yHeightPeriod == null) 0f else yHeightPeriod!!
        val lvlMultiplier = if (this.lvlMultiplier == null) 0f else lvlMultiplier!!

        result = if (this.scaleDownward == null || scaleDownward!!) {
            ((((transitionYHeight - currentYPos) /
                    yHeightPeriod) * lvlMultiplier)
                    * spawnDistanceLevelAssignment)
        } else {
            ((((transitionYHeight - currentYPos) /
                    yHeightPeriod) * (lvlMultiplier * -1f))
                    * spawnDistanceLevelAssignment)
        }

        if (result.isNaN()){
            DebugManager.log(DebugType.STRATEGY_RESULT, lmEntity) {
                "BlendedLevel generated NaN, returning 0. transitionYHeight: $transitionYHeight, yPos: $currentYPos, " +
                        "yHeightPeriod: $yHeightPeriod, lvlMultiplier: $lvlMultiplier, sda: $spawnDistanceLevelAssignment"
            }
            result = 0f
        }

        result += spawnDistanceLevelAssignment
        val variance = RulesManager.instance.getRuleMaxRandomVariance(lmEntity)

        if (variance != null && variance > 0)
            result += ThreadLocalRandom.current().nextInt(0, variance + 1).toFloat()

        return result
    }

    override fun cloneItem(): SpawnDistanceStrategy {
        var copy: SpawnDistanceStrategy? = null
        try {
            copy = super.clone() as SpawnDistanceStrategy
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return copy!!
    }
}