/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.events;

import me.lokka30.levelledmobs.misc.AdditionalLevelInformation;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

/**
 * This event is fired *before*
 * a mob has been levelled. Note
 * that it does not fire when the
 * mob was spawned using `/lm
 * summon`, instead see Summoned-
 * -MobPreLevelEvent.
 *
 * @author lokka30
 * @since 2.5.0
 */
public class MobPreLevelEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    @Override
    public HandlerList getHandlers() {
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
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * When a mob is levelled, the following enum is used to
     * allow plugins to find the cause of the mob being
     * levelled.
     * <p>
     * NORMAL: Spawned naturally, by a spawn egg, etc.
     * CHANGED_LEVEL: When an existing levelled mob has its level changed.
     */
    public enum LevelCause {
        NORMAL,
        CHANGED_LEVEL
    }

    private final LivingEntity entity;
    private int level;
    private final LevelCause levelCause;
    private final HashSet<AdditionalLevelInformation> additionalInformation;
    private boolean showLM_Nametag;

    public MobPreLevelEvent(@NotNull final LivingEntity entity, final int level, @NotNull final LevelCause levelCause, final @NotNull HashSet<AdditionalLevelInformation> additionalInformation) {
        super(!Bukkit.isPrimaryThread());
        this.entity = entity;
        this.level = level;
        this.levelCause = levelCause;
        this.additionalInformation = additionalInformation;
        this.showLM_Nametag = true;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public int getLevel() {
        return level;
    }

    public LevelCause getCause() {
        return levelCause;
    }

    public HashSet<AdditionalLevelInformation> getAdditionalInformation() {
        return additionalInformation;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setShowLM_Nametag(final boolean doRemove){
        this.showLM_Nametag = doRemove;
    }

    public boolean getShowLM_Nametag(){
        return this.showLM_Nametag;
    }
}
