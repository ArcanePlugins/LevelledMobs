package me.lokka30.levelledmobs.bukkit.logic.levelling.strategy.impl.random;

import java.util.concurrent.ThreadLocalRandom;
import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import me.lokka30.levelledmobs.bukkit.logic.levelling.strategy.LevellingStrategy;
import org.jetbrains.annotations.NotNull;

public class RandomLevellingStrategy extends LevellingStrategy {

    public RandomLevellingStrategy(
        final int minLevel,
        final int maxLevel
    ) {
        super(minLevel, maxLevel);
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

}
