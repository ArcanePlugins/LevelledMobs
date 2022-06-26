package me.lokka30.levelledmobs.bukkit.logic.levelling.strategy.impl;

import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import me.lokka30.levelledmobs.bukkit.logic.levelling.strategy.LevellingStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerLevellingStrategy extends LevellingStrategy {

    public PlayerLevellingStrategy(int minLevel, int maxLevel) {
        super(minLevel, maxLevel);
    }

    @Override
    public @Nullable Integer generate(@NotNull Context context) {
        //TODO
        return null;
    }
}
