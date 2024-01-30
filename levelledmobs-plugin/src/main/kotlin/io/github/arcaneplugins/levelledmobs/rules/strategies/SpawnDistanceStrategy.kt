package io.github.arcaneplugins.levelledmobs.rules.strategies

import java.lang.reflect.Modifier
import java.util.concurrent.ThreadLocalRandom
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import org.bukkit.Location
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Holds the configuration and logic for applying a levelling system that is based upon the distance
 * from the world spawn
 *
 * @author stumper66, lokka30
 * @since 3.0.0
 */
class SpawnDistanceStrategy : LevellingStrategy, Cloneable{
    var startDistance: Int? = null
    var increaseLevelDistance: Int? = null
    var spawnLocationX: Int? = null
    var spawnLocationZ: Int? = null
    var blendedLevellingEnabled: Boolean? = null
    var transitionYheight: Int? = null
    var multiplierPeriod: Int? = null
    var lvlMultiplier: Double? = null
    var scaleDownward: Boolean? = null

    override fun mergeRule(levellingStrategy: LevellingStrategy) {
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
                    "sd: %s, ild: %s, t_yHght: %s, mp: %s, lvlMlp: %s, scdown: %s",
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
                    "sd: %s, ild: %s",
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

    override fun generateLevel(
        lmEntity: LivingEntityWrapper?, minLevel: Int,
        maxLevel: Int
    ): Int {
        if (lmEntity == null) return minLevel

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

        val startDistance = if (this.startDistance == null) 0 else startDistance!!
        val distanceFromSpawn = spawnLocation.distance(lmEntity.location).toInt()
        val levelDistance = max((distanceFromSpawn - startDistance).toDouble(), 0.0).toInt()

        val variance = LevelledMobs.instance.rulesManager.getRuleMaxRandomVariance(lmEntity)
        var varianceAdded = 0
        if (variance != null) {
            varianceAdded = ThreadLocalRandom.current().nextInt(0, variance + 1)
        }

        var increaseLevelDistance =
            if (this.increaseLevelDistance == null) 1 else increaseLevelDistance!!
        if (increaseLevelDistance == 0) {
            increaseLevelDistance = 1
        }

        //Get the level thats meant to be at a given distance
        val spawnDistanceAssignment = min(
            ((levelDistance / increaseLevelDistance) + minLevel + varianceAdded).toDouble(), maxLevel.toDouble()
        )
            .toInt()
        if (this.blendedLevellingEnabled == null || !blendedLevellingEnabled!!) {
            return spawnDistanceAssignment
        }

        return generateBlendedLevel(lmEntity, spawnDistanceAssignment, minLevel, maxLevel)
    }

    private fun generateBlendedLevel(
        lmEntity: LivingEntityWrapper,
        spawnDistanceLevelAssignment: Int, minLevel: Int, maxLevel: Int
    ): Int {
        val currentYPos = lmEntity.location.blockY

        var result: Double

        val transitionYHeight =
            if (this.transitionYheight == null) 0.0 else transitionYheight!!.toDouble()
        val multiplierPeriod = if (this.multiplierPeriod == null) 0.0 else multiplierPeriod!!.toDouble()
        val lvlMultiplier = if (this.lvlMultiplier == null) 0.0 else lvlMultiplier!!

        result = if (this.scaleDownward == null || scaleDownward!!) {
            ((((transitionYHeight - currentYPos.toDouble()) /
                    multiplierPeriod) * lvlMultiplier)
                    * spawnDistanceLevelAssignment.toDouble())
        } else {
            ((((transitionYHeight - currentYPos.toDouble()) /
                    multiplierPeriod) * (lvlMultiplier * -1.0))
                    * spawnDistanceLevelAssignment.toDouble())
        }

        result =
            if (result < 0.0) ceil(result) + spawnDistanceLevelAssignment else floor(result) + spawnDistanceLevelAssignment
        val variance = LevelledMobs.instance.rulesManager.getRuleMaxRandomVariance(
            lmEntity
        )
        if (variance != null && variance > 0) {
            result += ThreadLocalRandom.current().nextInt(0, variance + 1).toDouble()
        }

        if (result < minLevel) {
            result = minLevel.toDouble()
        } else if (result > maxLevel) {
            result = maxLevel.toDouble()
        }

        return result.toInt()
    }

    override fun cloneItem(): SpawnDistanceStrategy? {
        var copy: SpawnDistanceStrategy? = null
        try {
            copy = super.clone() as SpawnDistanceStrategy
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return copy
    }
}