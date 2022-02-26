package me.lokka30.levelledmobs.level.strategy;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.level.LevelledMob;
import me.lokka30.levelledmobs.util.math.ranged.RangedInt;
import org.jetbrains.annotations.NotNull;

public record TimeOfDayLevelling(
        @NotNull RangedInt timeOfDay
) implements LevellingStrategy{
    @Override
    public @NotNull String getName() {
        return "TimeOfDay";
    }

    @Override
    public int calculateLevel(@NotNull LevelledMobs main, @NotNull LevelledMob mob) {
        return 0;
    }
}