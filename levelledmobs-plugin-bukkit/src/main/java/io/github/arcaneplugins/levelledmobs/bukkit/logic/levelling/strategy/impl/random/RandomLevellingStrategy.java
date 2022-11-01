package io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.impl.random;

import java.util.concurrent.ThreadLocalRandom;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.LevellingStrategy;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class RandomLevellingStrategy extends LevellingStrategy {

    public RandomLevellingStrategy(
        final int minLevel,
        final int maxLevel
    ) {
        super("Random", minLevel, maxLevel);
    }

    /*
    `Integer` (object instead of primitive) is being used here
    as it's possible that other levelling strategies aren't able to generate a
    level for a mob at a context or perhaps user error in the config.
    This particular levelling strategy will always return a non-null integer.
    */
    @Override
    public @NotNull Integer generate(@NotNull Context context) {
        return ThreadLocalRandom.current().nextInt(getMinLevel(), getMaxLevel());
    }

    @Override
    public @NotNull String replaceInFormula(@NotNull String formula, @NotNull Context context) {
        final var placeholder = "%random-level%";

        if(!formula.contains(placeholder)) {
            return formula;
        }

        return formula.replace(placeholder, Integer.toString(generate(context)));
    }

    @NotNull
    public static RandomLevellingStrategy parse(final CommentedConfigurationNode node) {

        /* error checking */
        final boolean declaresMinLevel = node.hasChild("min-level");
        final boolean declaresMaxLevel = node.hasChild("max-level");
        String whatToFix = null;
        if(!declaresMaxLevel && !declaresMinLevel) {
            whatToFix = "min and max";
        } else if(!declaresMinLevel) {
            whatToFix = "min";
        } else if(!declaresMaxLevel) {
            whatToFix = "max";
        }
        if(whatToFix != null) {
            Log.sev(
                String.format("""
                Detected an invalid configuration for a Random Levelling strategy: you didn't specify a %s level. The strategy can't be parsed until this is fixed.""",
                whatToFix),

                true
            );
            throw new IllegalStateException();
        }

        /* looks good, let's parse it */
        /* TODO if confirmed working then use this smaller one instead.
        return new RandomLevellingStrategy(
            node.node("min-level").getInt(),
            node.node("max-level").getInt()
        );
         */

        final int minLevel = node.node("min-level").getInt();
        final int maxLevel = node.node("max-level").getInt();

        return new RandomLevellingStrategy(minLevel, maxLevel);
    }

}