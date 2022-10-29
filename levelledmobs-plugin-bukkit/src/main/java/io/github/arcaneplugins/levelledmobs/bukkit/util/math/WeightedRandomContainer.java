package io.github.arcaneplugins.levelledmobs.bukkit.util.math;

import com.google.common.collect.ImmutableMap;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.impl.weightedrandom.WeightedRandomLevellingStrategy;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;

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
public class WeightedRandomContainer<T> {

    private float totalWeight = 0f;
    private final ImmutableMap<T, Float> objectWeightMap;

    public WeightedRandomContainer(final Map<@NotNull T, @NotNull Float> objectWeightMap) {
        if(objectWeightMap.isEmpty()) {
            throw new IllegalArgumentException("An empty weighted random map was supplied: " +
                "weighted random maps must have at least 1 entry.");
        }

        this.objectWeightMap = ImmutableMap.copyOf(objectWeightMap);

        for(final float weight : getObjectWeightMap().values()) {
            if(weight <= 0) {
                throw new IllegalArgumentException("An object in a weighted random map " +
                    "is assigned a weight that is not greater than zero: weightings must be" +
                    "greater than zero.");
            }

            totalWeight += weight;
        }
    }

    /**
     * Selects one of the objects randomly, according to its weighting.
     *
     * @return object
     */
    @NotNull
    public T choose() {
        final float random = ThreadLocalRandom.current().nextFloat() * getTotalWeight();

        float currentWeight = 0f;
        for(final Entry<T, Float> entry : getObjectWeightMap().entrySet()) {
            currentWeight += entry.getValue();
            if(currentWeight >= random) return entry.getKey();
        }

        // should be impossible to reach here :)
        throw new IllegalStateException("Unable to choose a weighted random object: logic error");
    }

    @Nonnull
    public ImmutableMap<@NotNull T, @NotNull Float> getObjectWeightMap() {
        return objectWeightMap;
    }

    private float getTotalWeight() {
        return totalWeight;
    }
}
