/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.events;

import me.lokka30.levelledmobs.levelling.AdditionalLevelInformation;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * @author lokka30
 * @since v4.0.0
 * This event is fired by LevelledMobs when it
 * is about to level a mob.
 * <p>
 * It can be cancelled as this event implements Cancellable.
 * <p>
 * There is a one-tick delay between when an entity spawns
 * and when this event is processed, since this allows all
 * of our integrations to add mob metadata and so on
 * before LevelledMobs processes the mob, which ensures
 * greater compatibility with other plugins.
 * <p>
 * If the event is called for a mob that is already levelled,
 * but their level is being changed, then the previousLevel
 * will be their previous level. Otherwise, it will be zero
 * (not levelled).
 * @see Event
 * @see Cancellable
 * @see MobPostLevelEvent
 */
public class MobPreLevelEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() { return HANDLERS; }

    private boolean isCancelled = false;

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(final boolean isCancelled) {
        this.isCancelled = isCancelled;
    }

    private final LivingEntity livingEntity;
    private final int previousLevel;
    private int newLevel;
    private final EnumSet<AdditionalLevelInformation> additionalLevelInformation;

    public MobPreLevelEvent(
            final @NotNull LivingEntity livingEntity,
            final int previousLevel,
            final int newLevel,
            final @NotNull EnumSet<AdditionalLevelInformation> additionalLevelInformation
    ) {
        this.livingEntity = livingEntity;
        this.previousLevel = previousLevel;
        this.newLevel = newLevel;
        this.additionalLevelInformation = additionalLevelInformation;
    }

    public @NotNull LivingEntity getLivingEntity() { return livingEntity; }
    public int getPreviousLevel() { return previousLevel; }
    public boolean wasLevelled() { return previousLevel != 0; }
    public int getNewLevel() { return newLevel; }
    public void setNewLevel(final int newLevel) { this.newLevel = newLevel; }
    public @NotNull EnumSet<AdditionalLevelInformation> getAdditionalLevelInformation() { return additionalLevelInformation; }

}
