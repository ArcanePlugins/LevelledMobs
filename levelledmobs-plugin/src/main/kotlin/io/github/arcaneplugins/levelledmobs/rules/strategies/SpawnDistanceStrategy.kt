package io.github.arcaneplugins.levelledmobs.rules.strategies

import java.lang.reflect.Modifier
import java.util.concurrent.ThreadLocalRandom
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import org.bukkit.Location
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Holds the configuration and logic for applying a levelling system that is based upon the distance
 * from the world spawn
 *
 * @author stumper66, lokka30
 * @since 3.0.0
 */
class SpawnDistanceStrategy : LevellingStrategy, Cloneable{
    var startDistance: Float? = null
    var increaseLevelDistance: Float? = null
    var spawnLocationX: Float? = null
    var spawnLocationZ: Float? = null
    var blendedLevellingEnabled: Boolean? = null
    var transitionYheight: Float? = null
    var multiplierPeriod: Float? = null
    var lvlMultiplier: Float? = null
    var scaleDownward: Boolean? = null

    override val strategyType = StrategyType.SPAWN_DISTANCE

    override fun generateNumber(
        lmEntity: LivingEntityWrapper,
        minLevel: Int,
        maxLevel: Int
    ): Float {
        var spawnLocation = lmEntity.world.spawnLocation

        if (this.spawnLocationZ != null || this.spawnLocationX != null) {
            val useX =
                if (this.spawnLocationX == null) spawnLocation.x else spawnLocationX!!.toDouble()
            val useZ =
                if (this.spawnLocationZ == null) spawnLocation.x else spawnLocationZ!!.toDouble()

            spawnLocation = Location(
                lmEntity.livingEntity.world,
                useX,
                spawnLocation.y,
                useZ
            )
        }

        val startDistance = if (this.startDistance == null) 0f else startDistance!!
        val distanceFromSpawn = spawnLocation.distance(lmEntity.location).toFloat()
        val levelDistance = ((distanceFromSpawn - startDistance)).coerceAtLeast(0f)
        val variance = LevelledMobs.instance.rulesManager.getRuleMaxRandomVariance(lmEntity)
        var varianceAdded = 0
        if (variance != null) {
            varianceAdded = ThreadLocalRandom.current().nextInt(0, variance + 1)
        }

        var increaseLevelDistance =
            if (this.increaseLevelDistance == null) 1f else increaseLevelDistance!!
        if (increaseLevelDistance == 0f) {
            increaseLevelDistance = 1f
        }

        //Get the level thats meant to be at a given distance
        val spawnDistanceAssignment =
            ((levelDistance / increaseLevelDistance) + varianceAdded)

        if (this.blendedLevellingEnabled == null || !blendedLevellingEnabled!!) {
            return spawnDistanceAssignment
        }

        return generateBlendedLevel(lmEntity, spawnDistanceAssignment)
    }

    override fun mergeRule(levellingStrategy: LevellingStrategy?) {
        if (levellingStrategy == null) return
        if (levellingStrategy is SpawnDistanceStrategy) {
            mergeSpawnDistanceStrategy(levellingStrategy as SpawnDistanceStrategy?)
        }
    }

    private fun mergeSpawnDistanceStrategy(sds: SpawnDistanceStrategy?) {
        if (sds == null) {
            return
        }

        try {
            for (f in sds.javaClass.declaredFields) {
                if (!Modifier.isPublic(f.modifiers)) {
                    continue
                }
                if (f[sds] == null) {
                    continue
                }

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
        if (blendedLevellingEnabled != null && blendedLevellingEnabled!!) {
            sb.append(
                String.format(
                    "blended, sd: %s, ild: %s, t_yHght: %s, mp: %s, lvlMlp: %s, scdown: %s",
                    if (startDistance == null) 0 else startDistance,
                    if (increaseLevelDistance == null) 0 else increaseLevelDistance,
                    if (transitionYheight == null) 0 else transitionYheight,
                    if (multiplierPeriod == null) 0 else multiplierPeriod,
                    if (lvlMultiplier == null) 0.0 else lvlMultiplier,
                    scaleDownward == null || scaleDownward!!
                )
            )
        } else {
            sb.append(
                String.format(
                    "spawn distance, sd: %s, ild: %s",
                    if (startDistance == null) 0 else startDistance,
                    if (increaseLevelDistance == null) 0 else increaseLevelDistance
                )
            )
        }

        if (this.spawnLocationX != null) {
            sb.append(" x: ")
            sb.append(this.spawnLocationX)
        }

        if (this.spawnLocationZ != null) {
            sb.append(" z: ")
            sb.append(this.spawnLocationZ)
        }

        return sb.toString()
    }

    private fun generateBlendedLevel(
        lmEntity: LivingEntityWrapper,
        spawnDistanceLevelAssignment: Float
    ): Float {
        val currentYPos = lmEntity.location.blockY.toFloat()
        var result: Float
        val transitionYHeight =
            if (this.transitionYheight == null) 0f else transitionYheight!!.toFloat()
        val multiplierPeriod = if (this.multiplierPeriod == null) 0f else multiplierPeriod!!.toFloat()
        val lvlMultiplier = if (this.lvlMultiplier == null) 0f else lvlMultiplier!!

        result = if (this.scaleDownward == null || scaleDownward!!) {
            ((((transitionYHeight - currentYPos) /
                    multiplierPeriod) * lvlMultiplier)
                    * spawnDistanceLevelAssignment)
        } else {
            ((((transitionYHeight - currentYPos) /
                    multiplierPeriod) * (lvlMultiplier * -1f))
                    * spawnDistanceLevelAssignment)
        }

        result =
            if (result < 0.0) ceil(result) + spawnDistanceLevelAssignment else floor(result) + spawnDistanceLevelAssignment
        val variance = LevelledMobs.instance.rulesManager.getRuleMaxRandomVariance(
            lmEntity
        )
        if (variance != null && variance > 0f) {
            result += ThreadLocalRandom.current().nextInt(0, variance + 1).toFloat()
        }

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