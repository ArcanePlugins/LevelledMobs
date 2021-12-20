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
 * @see Event
 * @see Cancellable
 * @see MobPreLevelEvent
 * @since v4.0.0
 * This event is fired by LevelledMobs when a
 * levelled mob dies and LevelledMobs wants to
 * change the drops of the entity.
 */
public class MobDropModificationEvent extends Event {

    /*
    TODO
        lokka30: Complete event class.
     */

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return null;
    }
}
