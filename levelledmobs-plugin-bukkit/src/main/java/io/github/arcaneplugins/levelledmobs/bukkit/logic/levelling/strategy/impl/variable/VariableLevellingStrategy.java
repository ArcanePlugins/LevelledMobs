package io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.impl.variable;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.LevellingStrategy;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;

public final class VariableLevellingStrategy extends LevellingStrategy {

    // TODO private final String placeholder;

    public VariableLevellingStrategy(
        final int minLevel,
        final int maxLevel
    ) {
        super("Variable", minLevel, maxLevel);
        //TODO
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public @Nullable Integer generate(
        @NotNull final Context context
    ) {
        //TODO
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public @NotNull String replaceInFormula(
        @NotNull final String formula,
        @NotNull final Context context
    ) {
        //TODO
        throw new IllegalStateException("Not implemented");
    }

    public static @Nonnull VariableLevellingStrategy parse(
        @Nonnull final CommentedConfigurationNode node
    ) {
        //TODO
        throw new IllegalStateException("Not implemented");
    }
}
