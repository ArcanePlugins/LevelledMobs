package io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.impl.basic;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.LevellingStrategy;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import redempt.crunch.Crunch;

public class BasicLevellingStrategy extends LevellingStrategy {

    private final String formula;

    public BasicLevellingStrategy(
        final @Nonnull String formula,
        final int minLevel,
        final int maxLevel
    ) {
        super("Basic", minLevel, maxLevel);
        this.formula = formula;
    }

    @Override
    public @Nullable Integer generate(@NotNull final Context context) {
        return (int) Math.floor(Crunch.evaluateExpression(
            LogicHandler.replacePapiAndContextPlaceholders(getFormula(), context)
        ));
    }

    @Override
    public @NotNull String replaceInFormula(
        @NotNull final String formula,
        @NotNull final Context context
    ) {
        final String placeholder = "%basic-level%";
        if(!formula.contains(placeholder)) return formula;
        final Integer generatedLevel = generate(context);
        if(generatedLevel == null) return formula;
        return formula.replace(placeholder, Integer.toString(generatedLevel));
    }

    public static @Nonnull BasicLevellingStrategy parse(
        final @Nonnull CommentedConfigurationNode node
    ) {
        return new BasicLevellingStrategy(
            Objects.requireNonNull(node.node("formula").getString(), "formula"),
            node.node("min-level").getInt(),
            node.node("max-level").getInt()
        );
    }

    @Nonnull
    public String getFormula() {
        return formula;
    }
}
