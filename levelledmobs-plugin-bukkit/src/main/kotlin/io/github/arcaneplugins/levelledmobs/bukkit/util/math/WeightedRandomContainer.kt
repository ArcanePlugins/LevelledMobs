package io.github.arcaneplugins.levelledmobs.bukkit.util.math

import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.impl.weightedrandom.WeightedRandomLevellingStrategy
import java.util.concurrent.ThreadLocalRandom

/**
 * Allows creation of weighted random containers: these have a map of objects along with their
 * weightings.
 * <p>
 * The {@link WeightedRandomContainer#choose()} method is used to randomly select one of them,
 * although the chance that a particular object is chosen is proportional to its weighting in the
 * provided map.
 * <p>
 * When an object is instantiated, the provided map is cloned into an immutable form,
 * this is because mutations to the map would not reflect in an adjustment of the {@code totalWeight}.
 *
 * @author Lachlan Adamson (lokka30)
 * @version 1
 * @param <T> Type of objects used in the weighted random map.
 * @see WeightedRandomLevellingStrategy
 * @since 4.0.0.0
 */
class WeightedRandomContainer<T>(
    objectWeightMap: MutableMap<T, Float>
) {
    val objectWeightMap: MutableMap<T, Float>
    var totalWeight = 0f

    init {
        require(objectWeightMap.isNotEmpty()) {
            "An empty weighted random map was supplied: " +
                    "weighted random maps must have at least 1 entry."
        }

        this.objectWeightMap = objectWeightMap.toMutableMap()

        for (weight in objectWeightMap.values) {
            require(weight > 0) {
                "An object in a weighted random map " +
                        "is assigned a weight that is not greater than zero: weightings must be" +
                        "greater than zero."
            }

            totalWeight += weight
        }
    }

    /**
     * Selects one of the objects randomly, according to its weighting.
     *
     * @return object
     */
    fun choose(): T{
        val random: Float = ThreadLocalRandom.current().nextFloat() * totalWeight

        var currentWeight = 0f
        for ((key, value) in objectWeightMap.entries) {
            currentWeight += value
            if (currentWeight >= random) return key
        }

        // should be impossible to reach here :)
        throw IllegalStateException("Unable to choose a weighted random object: logic error")
    }
}