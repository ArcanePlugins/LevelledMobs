/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
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
 * mob has become Levelled. It is not Cancellable
 * since MobPreLevelEvent allows plugins to do such.
 */
public class MobPostLevelEvent extends Event {

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
