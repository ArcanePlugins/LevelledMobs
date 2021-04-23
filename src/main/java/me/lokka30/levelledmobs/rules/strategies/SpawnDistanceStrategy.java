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
}
