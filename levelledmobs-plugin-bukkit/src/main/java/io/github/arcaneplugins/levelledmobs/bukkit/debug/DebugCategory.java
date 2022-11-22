package io.github.arcaneplugins.levelledmobs.bukkit.debug;

import io.github.arcaneplugins.levelledmobs.bukkit.listener.impl.EntityDeathListener;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.setbuffs.SetBuffsAction;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.impl.spawndistance.SpawnDistanceLevellingStrategy;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.impl.yaxis.YAxisLevellingStrategy;

public enum DebugCategory {

    /**
     * Displays a bunch of statistics about a levelled mob when hit with a melee.
     * Inspecting player requires the {@code levelledmobs.debug} permission (default=op).
     */
    ENTITY_INSPECTOR,

    /**
     * Shows the buff application process for {@link SetBuffsAction}.
     */
    BUFFS,

    /**
     * Shows information about drop calculation in {@link EntityDeathListener}.
     */
    DEATH_DROPS,

    /**
     * Shows information about logic in the {@link SpawnDistanceLevellingStrategy}.
     */
    SPAWN_DISTANCE_STRATEGY,

    /**
     * Shows information about logic in the {@link YAxisLevellingStrategy}.
     */
    Y_AXIS_STRATEGY,

}
