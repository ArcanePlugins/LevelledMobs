/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs;

import java.util.List;
import me.lokka30.levelledmobs.rules.RuleInfo;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

/**
 * Interace used for wrapping LivingEntity to provide additions common commands and information
 *
 * @author stumper66
 * @since 3.0.0
 */
public interface LivingEntityInterface {

    @NotNull EntityType getEntityType();

    Location getLocation();

    World getWorld();

    @NotNull String getTypeName();

    @NotNull List<RuleInfo> getApplicableRules();

    @SuppressWarnings("unused")
    @NotNull LevelledMobs getMainInstance();

    double getDistanceFromSpawn();

    int getSpawnedTimeOfDay();

    void setSpawnedTimeOfDay(final int ticks);

    Integer getSummonedLevel();

    boolean isWasSummoned();

    void clearEntityData();

    void free();
}
