package io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.impl.variable;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.LevellingStrategy;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VariableLevellingStrategy extends LevellingStrategy {

    // TODO private final String placeholder;

    public VariableLevellingStrategy(int minLevel, int maxLevel) {
        super("Variable", minLevel, maxLevel);
    }

    @Override
    public @Nullable Integer generate(@NotNull Context context) {
        //TODO
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public @NotNull String replaceInFormula(
        @NotNull String formula,
        @NotNull Context context
    ) {
        //TODO
        throw new IllegalStateException("Not implemented");
    }
}
