package me.lokka30.levelledmobs.rules.strategies;

import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.Location;

import java.util.concurrent.ThreadLocalRandom;

/**
 * TODO Describe...
 *
 * @author stumper66
 * @author lokka30
 */
public class SpawnDistanceStrategy implements LevellingStrategy {
    public int startDistance;
    public int increaseLevelDistance;
    public Integer spawnLocation_X;
    public Integer spawnLocation_Z;
    public boolean blendedLevellingEnabled;
    public int transition_Y_Height;
    public int multiplierPeriod;
    public double lvlMultiplier;
    public boolean scaleDownward = true;

    public String toString(){
        if (blendedLevellingEnabled) {
            return String.format("sd: %s, ild: %s, t_yHght: %s, mp: %s, lvlMlp: %s, scdown: %s",
                    startDistance,
                    increaseLevelDistance,
                    transition_Y_Height,
                    multiplierPeriod,
                    lvlMultiplier,
                    scaleDownward);
        }
        else {
            return String.format("sd: %s, ild: %s",
                    startDistance, increaseLevelDistance);
        }
    }

    public int generateLevel(final LivingEntityWrapper lmEntity, final int minLevel, final int maxLevel) {
        Location spawnLocation = lmEntity.getLivingEntity().getWorld().getSpawnLocation();

        if (this.spawnLocation_Z != null || this.spawnLocation_X != null) {
            final double useX = this.spawnLocation_X == null ? spawnLocation.getX() : this.spawnLocation_X;
            final double useZ = this.spawnLocation_Z == null ? spawnLocation.getX() : this.spawnLocation_Z;

            spawnLocation = new Location(
                    lmEntity.getLivingEntity().getWorld(),
                    useX,
                    spawnLocation.getY(),
                    useZ);
        }

        final int distanceFromSpawn = (int) spawnLocation.distance(lmEntity.getLivingEntity().getLocation());
        final int levelDistance = Math.max(distanceFromSpawn - startDistance, 0);

        int variance = lmEntity.getMainInstance().rulesManager.getRule_MaxRandomVariance(lmEntity);
        if (variance > 0) {
            variance = ThreadLocalRandom.current().nextInt(0, variance + 1);
        }

        //Get the level thats meant to be at a given distance
        final int spawnDistanceAssignment = Math.min((levelDistance / increaseLevelDistance) + minLevel + variance, maxLevel);
        if (!this.blendedLevellingEnabled)
            return spawnDistanceAssignment;

        return generateBlendedLevel(lmEntity, spawnDistanceAssignment, minLevel, maxLevel);
    }

    private int generateBlendedLevel(final LivingEntityWrapper lmEntity, final int spawnDistanceLevelAssignment, final int minLevel, final int maxLevel){
        final int currentYPos = lmEntity.getLivingEntity().getLocation().getBlockY();
        final Location spawnLocation = lmEntity.getLivingEntity().getWorld().getSpawnLocation();

        double result;

        if (this.scaleDownward) {
            result = ((((
                    (double) this.transition_Y_Height - (double) currentYPos) /
                    (double) this.multiplierPeriod) * this.lvlMultiplier)
                    * (double) spawnDistanceLevelAssignment);
        }
        else {
            result = ((((
                    (double) this.transition_Y_Height - (double) currentYPos) /
                    (double) this.multiplierPeriod) * (this.lvlMultiplier * -1.0))
                    * (double) spawnDistanceLevelAssignment);
        }

        result = Utils.round(result + spawnDistanceLevelAssignment);
        final int variance = lmEntity.getMainInstance().rulesManager.getRule_MaxRandomVariance(lmEntity);
        if (variance > 0)
            result += ThreadLocalRandom.current().nextInt(0, variance + 1);

        if (result < minLevel) result = minLevel;
        else if (result > maxLevel) result = maxLevel;

        return (int) result;
    }
}
