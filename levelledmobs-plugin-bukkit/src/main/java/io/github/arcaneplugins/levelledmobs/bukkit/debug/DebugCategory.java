package io.github.arcaneplugins.levelledmobs.bukkit.debug;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.setbuffs.SetBuffsAction;

public enum DebugCategory {

    /**
     * Displays a bunch of statistics about a levelled mob when hit with a melee.
     * Inspecting player requires the {@code levelledmobs.debug} permission (default=op).
     */
    ENTITY_INSPECTOR,

    /**
     * Shows the buff application process for {@link SetBuffsAction}.
     */
    BUFFS
}
