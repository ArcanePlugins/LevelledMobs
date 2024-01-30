package io.github.arcaneplugins.levelledmobs

import io.github.arcaneplugins.levelledmobs.result.AdditionalLevelInformation
import io.github.arcaneplugins.levelledmobs.enums.LevellableState
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import org.bukkit.entity.LivingEntity

/**
 * Welcome to the LevelInterface, this class is a 'global' interface for LM itself AND other plugins
 * to apply and modify the main functions of LevelledMobs.
 *
 * @author lokka30, stumper66
 * @since 2.5
 */
interface LevelInterface {
    /**
     * Check if an existing mob is allowed to be levelled, according to the user's configuration.
     *
     *
     * Thread-safety intended, but not tested.
     *
     * @param lmInterface target mob
     * @return if the mob is allowed to be levelled (yes/no), with reason
     */
    fun getLevellableState(lmInterface: LivingEntityInterface): LevellableState

    /**
     * This method generates a level for the mob. It utilises the levelling mode specified by the
     * administrator through the settings.yml configuration.
     *
     *
     * Thread-safety intended, but not tested.
     *
     * @param lmEntity the entity to generate a level for
     * @return a level for the entity
     */
    fun generateLevel(lmEntity: LivingEntityWrapper): Int

    /**
     * This method generates a level for the mob. It utilises the levelling mode specified by the
     * administrator through the settings.yml configuration.
     *
     *
     * Thread-safety intended, but not tested.
     *
     * @param lmEntity the entity to generate a level for
     * @param minLevel the minimum level to be used for the mob
     * @param maxLevel the maximum level to be used for the mob
     * @return a level for the entity
     */
    fun generateLevel(
        lmEntity: LivingEntityWrapper,
        minLevel: Int,
        maxLevel: Int
    ): Int

    /**
     * This method applies a level to the target mob.
     *
     * You can run this method on a mob regardless if they are already levelled or not.
     *
     * This method DOES NOT check if it is LEVELLABLE. It is assumed that plugins make sure this is
     * the case (unless they intend otherwise).
     *
     * It is highly recommended to leave bypassLimits = false, unless the desired behaviour is to
     * override the user-configured limits.
     *
     * Thread-safety intended, but not tested.
     *
     * @param lmEntity                   target mob
     * @param level                      the level the mob should have
     * @param isSummoned                 if the mob was spawned by LevelledMobs, not by the server
     * @param bypassLimits               whether LM should disregard max level, etc.
     * @param additionalLevelInformation used to determine the source event
     */
    fun applyLevelToMob(
        lmEntity: LivingEntityWrapper,
        level: Int,
        isSummoned: Boolean,
        bypassLimits: Boolean,
        additionalLevelInformation: MutableSet<AdditionalLevelInformation?>
    )

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
    fun isLevelled(livingEntity: LivingEntity): Boolean

    /**
     * Retrieve the level of a levelled mob.
     *
     *
     * Thread-safety intended, but not tested.
     *
     * @param livingEntity the levelled mob to get the level of
     * @return the mob's level
     */
    fun getLevelOfMob(livingEntity: LivingEntity): Int

    /**
     * Un-level a mob.
     *
     * @param lmEntity levelled mob to un-level
     */
    fun removeLevel(lmEntity: LivingEntityWrapper)
}