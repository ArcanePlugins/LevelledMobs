package me.lokka30.levelledmobs.customdrops;

import java.util.Set;
import java.util.TreeSet;

public class CustomDropBase implements Cloneable {
    public CustomDropBase(){}

    public int minLevel;
    public int maxLevel;
    public int priority;
    public double chance;
    public boolean playerCausedOnly;
    public boolean noSpawner;
    public final Set<String> excludedMobs = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
}
