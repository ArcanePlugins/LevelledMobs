package me.lokka30.levelledmobs.rules.strategies;

import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Holds the configuration and logic for applying a levelling system that is
 * based upon the distance from the world spawn
 *
 * @author stumper66, lokka30
 */
public class SpawnDistanceStrategy implements LevellingStrategy, Cloneable {
    public Integer startDistance;
    public Integer increaseLevelDistance;
    public Integer spawnLocation_X;
    public Integer spawnLocation_Z;
    public Boolean blendedLevellingEnabled;
    public Integer transition_Y_Height;
    public Integer multiplierPeriod;
    public Double lvlMultiplier;
    public Boolean scaleDownward;

    public void mergeRule(final LevellingStrategy levellingStrategy){
        if (levellingStrategy instanceof SpawnDistanceStrategy)
            mergeSpawnDistanceStrategy((SpawnDistanceStrategy) levellingStrategy);
    }

    public void mergeSpawnDistanceStrategy(final SpawnDistanceStrategy sds){
        if (sds == null) return;

        try {
            for (final Field f : sds.getClass().getDeclaredFields()) {
                if (!Modifier.isPublic(f.getModifiers())) continue;
                if (f.get(sds) == null) continue;

                this.getClass().getDeclaredField(f.getName()).set(this, f.get(sds));
            }
        }
        catch (IllegalAccessException | NoSuchFieldException e){
            e.printStackTrace();
        }
    }

    public String toString(){
        if (blendedLevellingEnabled != null && blendedLevellingEnabled) {
            return String.format("sd: %s, ild: %s, t_yHght: %s, mp: %s, lvlMlp: %s, scdown: %s",
                    startDistance == null ? 0 : startDistance,
                    increaseLevelDistance == null ? 0 : increaseLevelDistance,
                    transition_Y_Height == null ? 0 : transition_Y_Height,
                    multiplierPeriod == null ? 0 : multiplierPeriod,
                    lvlMultiplier == null ? 0 : lvlMultiplier,
                    scaleDownward == null || scaleDownward);
        }
        else {
            return String.format("sd: %s, ild: %s",
                    startDistance == null ? 0 : startDistance,
                    increaseLevelDistance == null ? 0 : increaseLevelDistance
            );
        }
    }

    public int generateLevel(@NotNull final LivingEntityWrapper lmEntity, final int minLevel, final int maxLevel) {
        Location spawnLocation = lmEntity.getWorld().getSpawnLocation();

        if (this.spawnLocation_Z != null || this.spawnLocation_X != null) {
            final double useX = this.spawnLocation_X == null ? spawnLocation.getX() : this.spawnLocation_X;
            final double useZ = this.spawnLocation_Z == null ? spawnLocation.getX() : this.spawnLocation_Z;

            spawnLocation = new Location(
                    lmEntity.getLivingEntity().getWorld(),
                    useX,
                    spawnLocation.getY(),
                    useZ);
        }

        final int startDistance = this.startDistance == null ? 0 : this.startDistance;
        final int distanceFromSpawn = (int) spawnLocation.distance(lmEntity.getLocation());
        final int levelDistance = Math.max(distanceFromSpawn - startDistance, 0);

        int variance = lmEntity.getMainInstance().rulesManager.getRule_MaxRandomVariance(lmEntity);
        if (variance > 0)
            variance = ThreadLocalRandom.current().nextInt(0, variance + 1);

        int increaseLevelDistance = this.increaseLevelDistance == null ? 1 : this.increaseLevelDistance;
        if (increaseLevelDistance == 0) increaseLevelDistance = 1;

        //Get the level thats meant to be at a given distance
        final int spawnDistanceAssignment = Math.min((levelDistance / increaseLevelDistance) + minLevel + variance, maxLevel);
        if (this.blendedLevellingEnabled == null || !this.blendedLevellingEnabled)
            return spawnDistanceAssignment;

        return generateBlendedLevel(lmEntity, spawnDistanceAssignment, minLevel, maxLevel);
    }

    private int generateBlendedLevel(@NotNull final LivingEntityWrapper lmEntity, final int spawnDistanceLevelAssignment, final int minLevel, final int maxLevel){
        final int currentYPos = lmEntity.getLocation().getBlockY();

        double result;

        final double transition_Y_Height = this.transition_Y_Height == null ? 0.0 : this.transition_Y_Height;
        final double multiplierPeriod = this.multiplierPeriod == null ? 0.0 : this.multiplierPeriod;
        final double lvlMultiplier = this.lvlMultiplier == null ? 0.0 : this.lvlMultiplier;

        if (this.scaleDownward == null || this.scaleDownward) {
            result = ((((
                    transition_Y_Height - (double) currentYPos) /
                    multiplierPeriod) * lvlMultiplier)
                    * (double) spawnDistanceLevelAssignment);
        }
        else {
            result = ((((
                    transition_Y_Height - (double) currentYPos) /
                    multiplierPeriod) * (lvlMultiplier * -1.0))
                    * (double) spawnDistanceLevelAssignment);
        }

        result = result < 0.0 ?
                Math.ceil(result) + spawnDistanceLevelAssignment :
                Math.floor(result) + spawnDistanceLevelAssignment;
        final int variance = lmEntity.getMainInstance().rulesManager.getRule_MaxRandomVariance(lmEntity);
        if (variance > 0)
            result += ThreadLocalRandom.current().nextInt(0, variance + 1);

        if (result < minLevel) result = minLevel;
        else if (result > maxLevel) result = maxLevel;

        return (int) result;
    }

    public SpawnDistanceStrategy cloneItem() {
        SpawnDistanceStrategy copy = null;
        try {
            copy = (SpawnDistanceStrategy) super.clone();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return copy;
    }
}
