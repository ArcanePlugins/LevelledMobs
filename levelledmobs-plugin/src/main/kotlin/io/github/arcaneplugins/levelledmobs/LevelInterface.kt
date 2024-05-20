package io.github.arcaneplugins.levelledmobs

import io.github.arcaneplugins.levelledmobs.enums.LevellableState
import org.bukkit.entity.LivingEntity
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

/**
 * This is the interface used in the API and exposes functions that use
 * mostly generic classes to apply and modify the main functions of LevelledMobs.
 *
 * @author stumper66
 * @since 4.0
 */
interface LevelInterface {
    /**
     * Check if an existing mob is allowed to be levelled, according to the user's configuration.
     *
     *
     * Thread-safety intended, but not tested.
     *
     * @param livingEntity target mob
     * @return if the mob is allowed to be levelled (yes/no), with reason
     */
    @NotNull
    fun getLevellableState(@NotNull livingEntity: LivingEntity): LevellableState

    /**
     * Check if a LivingEntity is a levelled mob or not. This is determined *after*
     * MobPreLevelEvent.
     *
     *
     * Thread-safety intended, but not tested.
     *
     * @param livingEntity living entity to check
     * @return if the mob is levelled or not
     */
    fun isLevelled(@NotNull livingEntity: LivingEntity): Boolean

    /**
     * Retrieve the level of a levelled mob.
     *
     *
     * Thread-safety intended, but not tested.
     *
     * @param livingEntity the levelled mob to get the level of
     * @return the mob's level
     */
    fun getLevelOfMob(@NotNull livingEntity: LivingEntity): Int

    /**
     * Un-level a mob.
     *
     * @param livingEntity levelled mob to un-level
     */
    fun removeLevel(@NotNull livingEntity: LivingEntity)

    @Nullable
    fun getMobNametag(@NotNull livingEntity: LivingEntity): String?
}