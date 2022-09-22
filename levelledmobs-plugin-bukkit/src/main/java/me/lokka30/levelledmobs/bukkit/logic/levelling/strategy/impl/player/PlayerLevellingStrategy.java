package me.lokka30.levelledmobs.bukkit.logic.levelling.strategy.impl.player;

import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import me.lokka30.levelledmobs.bukkit.logic.levelling.strategy.LevellingStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerLevellingStrategy extends LevellingStrategy {

    // TODO private final String placeholder;

    public PlayerLevellingStrategy(int minLevel, int maxLevel) {
        super("Player", minLevel, maxLevel);
    }

    @Override
    public @Nullable Integer generate(@NotNull Context context) {
        //TODO
        return null;
    }

    @Override
    public @NotNull String replaceInFormula(
        @NotNull String formula,
        @NotNull Context context
    ) {
        //TODO
        return null;
    }
}
