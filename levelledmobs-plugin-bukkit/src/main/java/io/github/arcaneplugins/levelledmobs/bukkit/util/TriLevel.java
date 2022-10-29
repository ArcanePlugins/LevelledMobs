package io.github.arcaneplugins.levelledmobs.bukkit.util;

public final class TriLevel {

    private final int minLevel;
    private final int level;
    private final int maxLevel;

    public TriLevel(
        final int minLevel,
        final int level,
        final int maxLevel
    ) {
        this.maxLevel = maxLevel;
        this.level = level;
        this.minLevel = minLevel;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public int getLevel() {
        return level;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

}
