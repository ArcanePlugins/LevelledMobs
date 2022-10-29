package io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class LevellingStrategy {

    private final String name;
    private final int minLevel;
    private final int maxLevel;

    public LevellingStrategy(
        final String name,
        final int minLevel,
        final int maxLevel
    ) {
        this.name = name;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    @Nullable
    public abstract Integer generate(final @NotNull Context context);

    /*
    replaces placeholders like %random-level% or whatever the strategy chooses with the generated
    level for a mob with given context.
     */
    @NotNull
    public abstract String replaceInFormula(final @NotNull String formula, final @NotNull Context context);

    @NotNull
    public String getName() {
        return name;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

}
