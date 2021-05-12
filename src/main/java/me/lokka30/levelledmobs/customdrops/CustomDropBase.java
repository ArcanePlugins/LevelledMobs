package me.lokka30.levelledmobs.customdrops;

import java.util.Set;
import java.util.TreeSet;

public class CustomDropBase implements Cloneable {
    public CustomDropBase(){}

    public int minLevel;
    public int maxLevel;
    public int priority;
    public int maxDropGroup;
    public double chance;
    public boolean playerCausedOnly;
    public boolean noSpawner;
    public String groupId;
    public final Set<String> excludedMobs = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
}
