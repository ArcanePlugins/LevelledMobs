package me.lokka30.levelledmobs.bukkit.logic.levelling.strategy.impl;

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

    @Override
    public @NotNull Integer generate(@NotNull Context context) {
        return ThreadLocalRandom.current().nextInt(getMinLevel(), getMaxLevel());
    }

}
