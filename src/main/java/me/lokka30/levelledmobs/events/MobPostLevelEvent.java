/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.events;

import me.lokka30.levelledmobs.misc.AdditionalLevelInformation;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;

/**
 * This event is fired *after* a mob is levelled.
 * Other plugins can cancel this event.
 *
 * @author lokka30
 * @since 2.5.0
 */
public class MobPostLevelEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
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
        CHANGED_LEVEL,
        SUMMONED
    }

    private final LivingEntityWrapper lmEntity;
    private final LevelCause levelCause;
    private final HashSet<AdditionalLevelInformation> additionalInformation;

    public MobPostLevelEvent(@NotNull final LivingEntityWrapper lmEntity, @NotNull final LevelCause levelCause, @Nullable final HashSet<AdditionalLevelInformation> additionalInformation) {
        super(!Bukkit.isPrimaryThread());
        this.lmEntity = lmEntity;
        this.levelCause = levelCause;
        this.additionalInformation = additionalInformation;
    }

    public LivingEntity getEntity() {
        return lmEntity.getLivingEntity();
    }

    public LivingEntityWrapper getLmEntity() { return lmEntity; }

    public int getLevel() {
        return lmEntity.getMobLevel();
    }

    public LevelCause getCause() {
        return levelCause;
    }

    public HashSet<AdditionalLevelInformation> getAdditionalInformation() {
        return additionalInformation;
    }
}
