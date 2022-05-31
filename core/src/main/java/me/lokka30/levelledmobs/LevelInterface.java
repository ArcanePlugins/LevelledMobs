/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs;

import me.lokka30.levelledmobs.misc.AdditionalLevelInformation;
import me.lokka30.levelledmobs.misc.LevellableState;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

/**
 * Welcome to the LevelInterface,
 * this class is a 'global' interface
 * for LM itself AND other plugins to
 * apply and modify the main functions
 * of LevelledMobs.
 *
 * @author lokka30, stumper66
 * @since 2.5
 */
public interface LevelInterface {

    /**
     * Check if an existing mob is allowed to be levelled, according to the
     * user's configuration.
     * <p>
     * Thread-safety intended, but not tested.
     *
     * @param lmInterface target mob
     * @return if the mob is allowed to be levelled (yes/no), with reason
     */
    @NotNull
    LevellableState getLevellableState(@NotNull final LivingEntityInterface lmInterface);

    /**
     * This method generates a level for the mob. It utilises the levelling mode
     * specified by the administrator through the settings.yml configuration.
     *
     * Thread-safety intended, but not tested.
     *
     * @param lmEntity the entity to generate a level for
     * @return a level for the entity
     */
    int generateLevel(@NotNull final LivingEntityWrapper lmEntity);

    /**
     * This method generates a level for the mob. It utilises the levelling mode
     * specified by the administrator through the settings.yml configuration.
     *
     * Thread-safety intended, but not tested.
     *
     * @param lmEntity the entity to generate a level for
     * @param minLevel the minimum level to be used for the mob
     * @param maxLevel the maximum level to be used for the mob
     * @return a level for the entity
     */
    int generateLevel(@NotNull final LivingEntityWrapper lmEntity, final int minLevel, final int maxLevel);

    /**
     * This method applies a level to the target mob.
     * <p>
     * You can run this method on a mob regardless if
     * they are already levelled or not.
     * <p>
     * This method DOES NOT check if it is LEVELLABLE. It is
     * assumed that plugins make sure this is the case (unless
     * they intend otherwise).
     * <p>
     * It is highly recommended to leave bypassLimits = false,
     * unless the desired behaviour is to override the
     * user-configured limits.
     * <p>
     * Thread-safety intended, but not tested.
     *
     * @param lmEntity target mob
     * @param level        the level the mob should have
     * @param isSummoned   if the mob was spawned by LevelledMobs, not by the server
     * @param bypassLimits whether LM should disregard max level, etc.
     * @param additionalLevelInformation used to determine the source event
     */
    void applyLevelToMob(@NotNull final LivingEntityWrapper lmEntity, int level, final boolean isSummoned, final boolean bypassLimits,
                         @NotNull final HashSet<AdditionalLevelInformation> additionalLevelInformation);

    /**
     * Check if a LivingEntity is a levelled mob or not.
     * This is determined *after* MobPreLevelEvent.
     * <p>
     * Thread-safety intended, but not tested.
     *
     * @param livingEntity living entity to check
     * @return if the mob is levelled or not
     */
    boolean isLevelled(@NotNull final LivingEntity livingEntity);

    /**
     * Retrieve the level of a levelled mob.
     *
     * Thread-safety intended, but not tested.
     *
     * @param livingEntity the levelled mob to get the level of
     * @return the mob's level
     */
    int getLevelOfMob(@NotNull final LivingEntity livingEntity);

    /**
     * Un-level a mob.
     *
     * @param lmEntity levelled mob to un-level
     */
    void removeLevel(@NotNull final LivingEntityWrapper lmEntity);
}
