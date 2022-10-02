package me.lokka30.levelledmobs.bukkit.logic.levelling.strategy.impl.weightedrandom;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import me.lokka30.levelledmobs.bukkit.logic.levelling.strategy.LevellingStrategy;
import me.lokka30.levelledmobs.bukkit.util.math.RangedInt;
import me.lokka30.levelledmobs.bukkit.util.math.WeightedRandomContainer;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class WeightedRandomLevellingStrategy extends LevellingStrategy {

    private final WeightedRandomContainer<RangedInt> levelWeightContainer;

    public WeightedRandomLevellingStrategy(
        final WeightedRandomContainer<RangedInt> levelWeightContainer,
        final int minLevel,
        final int maxLevel
    ) {
        super("Weighted Random", minLevel, maxLevel);
        this.levelWeightContainer = levelWeightContainer;
    }

    @Override
    public @NotNull Integer generate(@NotNull Context context) {
        return getLevelWeightContainer()
            .choose()  // choose a weighted ranged int
            .choose(); // choose a random value in between the min-max ranged int
    }

    @Override
    public @NotNull String replaceInFormula(@NotNull String formula, @NotNull Context context) {
        final var placeholder = "%weighted-random-level%";

        if(!formula.contains(placeholder)) {
            return formula;
        }

        return formula.replace(placeholder, Integer.toString(generate(context)));
    }

    @Nonnull
    public static WeightedRandomLevellingStrategy parse(
        final @Nonnull CommentedConfigurationNode node
    ) {
        final Map<RangedInt, Float> levelWeightMap = new HashMap<>();

        // add entries to level weight map
        node.node("tiers").childrenMap().forEach((key, value) -> {
            RangedInt ri;

            if(key instanceof Integer ikey) {
                ri = new RangedInt(ikey);
            } else if(key instanceof String skey) {
                ri = new RangedInt(skey);
            } else {
                throw new IllegalArgumentException("Unable to parse weighted random map as the " +
                    "entry '%s' is a '%s', not a String or Integer".formatted(
                        key,
                        key.getClass().getName()
                    )
                );
            }

            levelWeightMap.put(ri, value.getFloat());
        });

        // determine min and max level
        Integer minLevel = null;
        Integer maxLevel = null;

        for(final RangedInt ri : levelWeightMap.keySet()) {
            if(minLevel == null || ri.getMin() < minLevel) minLevel = ri.getMin();
            if(maxLevel == null || ri.getMax() > maxLevel ) maxLevel = ri.getMax();
        }

        Validate.notNull(minLevel, "Unable to determine min level of weighted random map");
        Validate.notNull(maxLevel, "Unable to determine max level of weighted random map");

        // return strategy object
        return new WeightedRandomLevellingStrategy(
            new WeightedRandomContainer<>(levelWeightMap),
            minLevel,
            maxLevel
        );
    }

    @Nonnull
    public WeightedRandomContainer<RangedInt> getLevelWeightContainer() {
        return levelWeightContainer;
    }
}
