/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * This event is fired *before* a mob has
 * been levelled, but only if it was
 * spawned using the `/lm summon` command.
 *
 * @author lokka30
 * @since 2.5.0
 */
public class SummonedMobPreLevelEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    private boolean cancelled = false;

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(final boolean cancelled) {
        this.cancelled = cancelled;
    }

    private final LivingEntity entity;
    private int level;

    public SummonedMobPreLevelEvent(final LivingEntity entity, final int level) {
        super(!Bukkit.isPrimaryThread());
        this.entity = entity;
        this.level = level;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(final int level) {
        this.level = level;
    }
}
