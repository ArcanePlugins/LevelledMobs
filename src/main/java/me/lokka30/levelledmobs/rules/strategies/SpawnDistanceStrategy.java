package me.lokka30.levelledmobs.rules.strategies;

import scala.Int;

/**
 * TODO Describe...
 *
 * @author stumper66
 * @author lokka30
 */
public class SpawnDistanceStrategy implements LevellingStrategy {
    public int startDistance;
    public int increaseLevelDistance;
    public int variance;
    public Integer spawnLocation_X;
    public Integer spawnLocation_Z;
    public boolean blendedLevellingEnabled;
    public int transition_Y_Height;
    public int multiplierPeriod;
    public double lvlMultiplier;
    public boolean scaleDownward = true;

    public String toString(){
        if (blendedLevellingEnabled) {
            return String.format("sd: %s, ild: %s, var: %s, t_yHght: %s, mp: %s, lvlMlp: %s, scdown: %s",
                    startDistance,
                    increaseLevelDistance,
                    variance,
                    transition_Y_Height,
                    multiplierPeriod,
                    lvlMultiplier,
                    scaleDownward);
        }
        else {
            return String.format("sd: %s, ild: %s, var: %s",
                    startDistance, increaseLevelDistance, variance);
        }
    }
}
