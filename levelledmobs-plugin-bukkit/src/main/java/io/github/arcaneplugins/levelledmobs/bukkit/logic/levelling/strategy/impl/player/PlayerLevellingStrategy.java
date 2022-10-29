package io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.impl.player;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.LevellingStrategy;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
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
