package io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.impl.yaxis;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.setlevel.SetLevelAction;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.LevellingStrategy;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import io.github.arcaneplugins.levelledmobs.bukkit.util.math.RangedInt;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import javax.annotation.Nonnull;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class YAxisLevellingStrategy extends LevellingStrategy {

    private final int defaultLevel;
    private final boolean inverse;
    private final Map<RangedInt, RangedInt> yLevelTiersMap;

    public YAxisLevellingStrategy(
        final int minLevel,
        final int maxLevel,
        @Nonnull final Map<RangedInt, RangedInt> yLevelTiersMap,
        final int defaultLevel,
        final boolean inverse
    ) {
        super("Y-Axis", minLevel, maxLevel);

        this.yLevelTiersMap = yLevelTiersMap;
        this.defaultLevel = defaultLevel;
        this.inverse = inverse;

        // warn user if distance keys overlap
        Integer previousMax = null;
        for(final RangedInt distanceRange : getYLevelTiersMap().keySet()) {
            if(previousMax != null && previousMax > distanceRange.getMin()) {
                Log.war("[Y-Axis] Tier overlap detected in '" +
                    distanceRange.getMin() + "-" + distanceRange.getMax() +
                    "'. This will cause issues!");
            }

            previousMax = distanceRange.getMax();
        }
    }

    @Override
    public @Nullable Integer generate(
        @NotNull final Context context
    ) {
        if(context.getEntity() == null || !(context.getEntity() instanceof LivingEntity lent))
            return null;

        /*
        determine y-level tier
         */
        final int y = lent.getLocation().getBlockY();

        for(final Entry<RangedInt, RangedInt> yLevelRanges :
            getYLevelTiersMap().entrySet().stream().sorted().toList()
        ) {
            final RangedInt yRange = yLevelRanges.getKey();
            final RangedInt levelRange = yLevelRanges.getValue();

            Log.tmpdebug("Y range: %s-%s, Level range: %s-%s".formatted(
                yRange.getMin(), yRange.getMax(),
                levelRange.getMin(), levelRange.getMax()
            ));

            if(yRange.contains(y)) {
                final int levelMin = levelRange.getMin();
                final int levelMax = levelRange.getMax();

                final int yMin = yRange.getMin();
                final int yMax = yRange.getMax();

                if(inverse) {
                    return (int) Math.floor(
                        (levelMax - levelMin) * ((y - yMax) * 1.0f / (yMin - yMax)) + levelMin
                    );
                } else {
                    return (int) Math.floor(
                        ((levelMax - levelMin) * ((y - yMin) * 1.0f / (yMax - yMin))) + levelMin
                    );
                }
            }
        }

        // Mob's Y level was not factored in the tiers. Return default level instead
        return defaultLevel;
    }

    @Override
    public @NotNull String replaceInFormula(
        @NotNull final String formula,
        @NotNull final Context context
    ) {
        final String placeholder = "%y-axis-level%";
        if(!formula.contains(placeholder)) return formula;
        final Integer generatedLevel = generate(context);
        if(generatedLevel == null) return formula;
        return formula.replace(placeholder, Integer.toString(generatedLevel));
    }

    public static @Nonnull YAxisLevellingStrategy parse(
        @Nonnull final CommentedConfigurationNode node
    ) {
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
        final Function<Object, RangedInt> parseRngInt = (obj) -> {
            if(obj instanceof Integer i) {
                return new RangedInt(i);
            } else if(obj instanceof String str) {
                return new RangedInt(str);
            } else {
                throw new IllegalArgumentException(
                    "[Y-Axis] Unable to parse ranged int as the " +
                        "entry '%s' is a '%s', not a String or Integer".formatted(
                            obj,
                            obj.getClass().getName()
                        )
                );
            }
        };

        final Map<RangedInt, RangedInt> yLevelTiersMap = new HashMap<>();

        final Map<Object, ? extends CommentedConfigurationNode> tiersNodeMap =
            node.node("tiers").childrenMap();

        if(tiersNodeMap.size() == 0) {
            throw new IllegalArgumentException(
                "Y-Axis levelling strategy requires one or more Y-to-Level tiers."
            );
        }

        AtomicInteger defaultLevel = new AtomicInteger(SetLevelAction.getMinPossibleLevel());
        AtomicBoolean inverse = new AtomicBoolean(true);

        tiersNodeMap.forEach((key, value) -> {
            if(key.toString().equalsIgnoreCase("undefined")) {
                defaultLevel.set(value.getInt());
            } else if(key.toString().equalsIgnoreCase("inverse")) {
                inverse.set(value.getBoolean());
            } else {
                yLevelTiersMap.put(parseRngInt.apply(key), parseRngInt.apply(value));
            }
        });

        /*
        build strategy object
         */

        // determine min and max level out of the distance-level tiers
        Integer minLevel = null, maxLevel = null;

        for(final RangedInt levelRange : yLevelTiersMap.values()) {
            // determine min level
            if(minLevel == null) {
                minLevel = levelRange.getMin();
            } else {
                if(minLevel > levelRange.getMin()) minLevel = levelRange.getMin();
            }

            // determine max level
            if(maxLevel == null) {
                maxLevel = levelRange.getMax();
            } else {
                if(maxLevel > levelRange.getMax()) maxLevel = levelRange.getMax();
            }
        }

        if(minLevel == null || minLevel > defaultLevel.get()) {
            minLevel = defaultLevel.get();
        }

        if(maxLevel == null || maxLevel < defaultLevel.get()) {
            maxLevel = defaultLevel.get();
        }

        return new YAxisLevellingStrategy(
            minLevel,
            maxLevel,
            yLevelTiersMap,
            defaultLevel.get(),
            inverse.get()
        );
    }

    @Nonnull
    public Map<RangedInt, RangedInt> getYLevelTiersMap() {
        return yLevelTiersMap;
    }

}
