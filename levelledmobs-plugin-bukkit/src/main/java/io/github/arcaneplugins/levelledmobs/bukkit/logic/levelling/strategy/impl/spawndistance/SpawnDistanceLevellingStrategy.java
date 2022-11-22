package io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.impl.spawndistance;

import static io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugCategory.SPAWN_DISTANCE_STRATEGY;

import io.github.arcaneplugins.levelledmobs.bukkit.api.util.Pair;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.setlevel.SetLevelAction;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.LevellingStrategy;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import io.github.arcaneplugins.levelledmobs.bukkit.util.math.RangedInt;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import javax.annotation.Nonnull;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class SpawnDistanceLevellingStrategy extends LevellingStrategy {

    private final int defaultLevel;
    private final Map<RangedInt, RangedInt> distanceLevelTiersMap;
    private final Map<String, Pair<Integer, Integer>> spawnLocationsMap;

    public SpawnDistanceLevellingStrategy(
        final int minLevel,
        final int maxLevel,
        @Nonnull final Map<RangedInt, RangedInt> distanceLevelTiersMap,
        final int defaultLevel,
        @Nonnull final Map<String, Pair<Integer, Integer>> spawnLocationsMap
    ) {
        super("Spawn-Distance", minLevel, maxLevel);

        this.distanceLevelTiersMap = distanceLevelTiersMap;
        this.defaultLevel = defaultLevel;
        this.spawnLocationsMap = spawnLocationsMap;

        // warn user if distance keys overlap
        Integer previousMax = null;
        for(final RangedInt distanceRange : getDistanceLevelTiersMap().keySet()) {
            if(previousMax != null && previousMax > distanceRange.getMin()) {
                Log.war("[Spawn-Distance] Tier overlap detected in '" +
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
        determine spawn location
         */
        final World world = lent.getWorld();
        final Pair<Integer, Integer> spawnXZ = getSpawnLocationsMap().computeIfAbsent(
            world.getName(),
            worldName -> {
                final Location spawnLocation = world.getSpawnLocation();
                return new Pair<>(spawnLocation.getBlockX(), spawnLocation.getBlockY());
            }
        );
        final int spawnX = spawnXZ.getLeft();
        final int spawnZ = spawnXZ.getRight();

        /*
        determine distance from spawn
         */
        final int lentX = lent.getLocation().getBlockX();
        final int lentZ = lent.getLocation().getBlockZ();

        final int distance = (int) Math.floor(Math.sqrt(
            Math.pow(lentX - spawnX, 2) + Math.pow(lentZ - spawnZ, 2)
        ));

        /*
        determine distane-level tier
         */
        for(final Entry<RangedInt, RangedInt> distanceLevelRanges :
            getDistanceLevelTiersMap().entrySet().stream().sorted().toList()
        ) {
            final RangedInt distanceRange = distanceLevelRanges.getKey();
            final RangedInt levelRange = distanceLevelRanges.getValue();

            Log.debug(SPAWN_DISTANCE_STRATEGY, () -> "Distance range: %s-%s, Level range: %s-%s"
                .formatted(
                    distanceRange.getMin(), distanceRange.getMax(),
                    levelRange.getMin(), levelRange.getMax()
                )
            );

            if(distanceRange.contains(distance)) {
                final float distanceRatio = ((distance - distanceRange.getMin()) * 1.0f /
                    (distanceRange.getMax() - distanceRange.getMin()));

                final int levelMin = levelRange.getMin();
                final int levelMax = levelRange.getMax();

                return (int) Math.floor(((levelMax - levelMin) * distanceRatio) + levelMin);
            }
        }

        // Mob's distance from spawn was not factored in the tiers. Return default level instead
        return defaultLevel;
    }

    @Override
    public @NotNull String replaceInFormula(
        @NotNull final String formula,
        @NotNull final Context context
    ) {
        final String placeholder = "%spawn-distance-level%";
        if(!formula.contains(placeholder)) return formula;
        final Integer generatedLevel = generate(context);
        if(generatedLevel == null) return formula;
        return formula.replace(placeholder, Integer.toString(generatedLevel));
    }

    public static @Nonnull SpawnDistanceLevellingStrategy parse(
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
                    "[Spawn-Distance] Unable to parse ranged int as the " +
                    "entry '%s' is a '%s', not a String or Integer".formatted(
                        obj,
                        obj.getClass().getName()
                    )
                );
            }
        };

        final Map<RangedInt, RangedInt> distanceLevelTiersMap = new HashMap<>();

        final Map<Object, ? extends CommentedConfigurationNode> tiersNodeMap =
            node.node("tiers").childrenMap();

        if(tiersNodeMap.size() == 0) {
            throw new IllegalArgumentException(
                "Spawn distance strategy requires one or more distance-to-level tiers."
            );
        }

        AtomicInteger defaultLevel = new AtomicInteger(SetLevelAction.getMinPossibleLevel());

        tiersNodeMap.forEach((key, value) -> {
            if(key.toString().equalsIgnoreCase("undefined")) {
                defaultLevel.set(value.getInt());
            } else {
                distanceLevelTiersMap.put(parseRngInt.apply(key), parseRngInt.apply(value));
            }
        });

        /*
        parse spawn locations
         */

        final Map<String, Pair<Integer, Integer>> spawnLocationsMap = new HashMap<>();

        node.node("spawn-locations").childrenMap().forEach((key, value) -> {
            spawnLocationsMap.put(key.toString(), new Pair<>(
                value.node("x").getInt(),
                value.node("z").getInt()
            ));
        });

        /*
        build strategy object
         */

        // determine min and max level out of the distance-level tiers
        Integer minLevel = null, maxLevel = null;

        for(final RangedInt levelRange : distanceLevelTiersMap.values()) {
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

        return new SpawnDistanceLevellingStrategy(
            minLevel,
            maxLevel,
            distanceLevelTiersMap,
            defaultLevel.get(),
            spawnLocationsMap
        );
    }

    @Nonnull
    public Map<RangedInt, RangedInt> getDistanceLevelTiersMap() {
        return distanceLevelTiersMap;
    }

    @Nonnull
    public Map<String, Pair<Integer, Integer>> getSpawnLocationsMap() {
        return spawnLocationsMap;
    }
}
