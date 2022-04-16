/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.plugin.old.event;

import java.util.EnumSet;
import me.lokka30.levelledmobs.plugin.old.level.AdditionalLevelInformation;
import me.lokka30.levelledmobs.plugin.old.level.LevelledMob;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class MobPostLevelEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    private final LevelledMob levelledMob;
    private final int level;
    private final EnumSet<AdditionalLevelInformation> additionalLevelInformation;

    public MobPostLevelEvent(
        final @NotNull LevelledMob levelledMob,
        final int level,
        final @NotNull EnumSet<AdditionalLevelInformation> additionalLevelInformation
    ) {
        this.levelledMob = levelledMob;
        this.level = level;
        this.additionalLevelInformation = additionalLevelInformation;
    }

    public @NotNull
    LevelledMob getLevelledMob() {
        return levelledMob;
    }

    public int getLevel() {
        return level;
    }

    public @NotNull
    EnumSet<AdditionalLevelInformation> getAdditionalLevelInformation() {
        return additionalLevelInformation;
    }
}
