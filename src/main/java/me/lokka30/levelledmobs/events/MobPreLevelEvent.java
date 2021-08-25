/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * @author lokka30
 * @since v4.0.0
 * This event is fired by LevelledMobs when it
 * is about to level a mob. It can be cancelled
 * as this event implements Cancellable.
 * There is a one-tick delay (can be increased
 * through user configuration) before this event
 * actually fires, since this allows all of our
 * integrations to add mob metadata and so on
 * before LevelledMobs processes the mob,
 * maximising the plugin's reliability.
 * @see Event
 * @see Cancellable
 * @see MobPostLevelEvent
 */
public class MobPreLevelEvent extends Event implements Cancellable {

    /*
    TODO
        lokka30: Complete event class.
     */

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public void setCancelled(boolean b) {

    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return null;
    }
}
