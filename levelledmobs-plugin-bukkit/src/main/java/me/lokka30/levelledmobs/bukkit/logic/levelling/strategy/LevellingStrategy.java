package me.lokka30.levelledmobs.bukkit.logic.levelling.strategy;

import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class LevellingStrategy {

    private final int minLevel;
    private final int maxLevel;

    public LevellingStrategy(
        final int minLevel,
        final int maxLevel
    ) {
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    @Nullable
    public abstract Integer generate(final @NotNull Context context);

    public int getMinLevel() {
        return minLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

}
