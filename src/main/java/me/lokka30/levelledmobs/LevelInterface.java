package me.lokka30.levelledmobs;

import me.lokka30.levelledmobs.events.MobLevelEvent;
import me.lokka30.levelledmobs.events.SummonedMobLevelEvent;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class LevelInterface {

    /*
     * Work in progress!
     * Other developers, please do not use
     * anything from this class yet. Cheers
     */

    private final LevelledMobs main;

    public LevelInterface(@NotNull final LevelledMobs main) {
        this.main = main;
    }

    /**
     * Check if an existing mob is allowed to be levelled, according to the
     * user's configuration.
     * <p>
     * Thread-safe (intended, but not tested)
     *
     * @param livingEntity target mob
     * @return if the mob is allowed to be levelled (yes/no), with reason
     */
    public LevellableState getLevellableState(@NotNull LivingEntity livingEntity) {
        //TODO temporary code
        return main.levelManager.isLevellable(livingEntity) ? LevellableState.ALLOWED : LevellableState.DENIED_BLOCKED_ENTITY_TYPE;
    }

    /**
     * Check if a mob is allowed to be levelled, according to the
     * user's configuration.
     * Developers, please ensure you understand that this method
     * does not account for certain things such as 'is mob tamed?',
     * WorldGuard regions, blocked worlds, etc,
     * which the user may have disabled, and this method is unable to
     * factor that in. Where possible, use isLevellable(LivingEntity).
     * <p>
     * Thread-safe (intended, but not tested)
     *
     * @param entityType target entity type
     * @return of the mob is allowed to be levelled (yes/no), with reason
     */
    public LevellableState getLevellableState(@NotNull EntityType entityType) {
        //TODO
        return null;
    }

    /**
     * Check if a mob is allowed to be levelled, according to the
     * user's configuration.
     * Developers, please ensure you understand that this method
     * does not account for certain things such as 'is mob tamed?',
     * which the user may have disabled, and this method is unable to
     * factor that in. Where possible, use isLevellable(LivingEntity).
     * <p>
     * Thread-safe (intended, but not tested)
     *
     * @param entityType target entity type
     * @param location   target location
     * @return if the mob is allowed to be levelled (yes/no), with reason
     */
    public LevellableState getLevellableState(@NotNull EntityType entityType, @NotNull Location location) {
        //TODO
        return null;
    }

    /**
     * This method generates a level for the mob. It utilises the levelling mode
     * specified by the administrator through the settings.yml configuration.
     *
     * @param livingEntity the entity to generate a level for
     * @return a level for the entity
     */
    public int generateLevel(@NotNull LivingEntity livingEntity) {
        //TODO
        return -1;
    }

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
     * Thread-safe (intended, but not tested)
     *
     * @param livingEntity target mob
     * @param level        the level the mob should have
     * @param wasSummoned  if the mob was spawned using '/lm summon'
     * @param bypassLimits whether LM should disregard max level, etc.
     */
    public void applyLevelToMob(@NotNull LivingEntity livingEntity, int level, boolean wasSummoned, boolean bypassLimits) {
        Validate.isTrue(level >= 0, "Level must be greater than or equal to zero.");

        if (wasSummoned) {
            SummonedMobLevelEvent summonedMobLevelEvent = new SummonedMobLevelEvent(livingEntity, level);
            Bukkit.getPluginManager().callEvent(summonedMobLevelEvent);
            if (summonedMobLevelEvent.isCancelled()) return;
        } else {
            MobLevelEvent mobLevelEvent = new MobLevelEvent(livingEntity, level, MobLevelEvent.LevelCause.NORMAL, null);
            Bukkit.getPluginManager().callEvent(mobLevelEvent);
            if (mobLevelEvent.isCancelled()) return;
        }

        //TODO process mob spawn
        livingEntity.getPersistentDataContainer().set(main.levelManager.levelKey, PersistentDataType.INTEGER, 1); //todo check if this is the right awy

        applyLevelledEquipment(livingEntity, level);
    }

    /**
     * Check if a LivingEntity is a levelled mob or not.
     * This is determined *after* MobLevelEvent.
     * <p>
     * Thread-safe (intended, but not tested)
     *
     * @param livingEntity living entity to check
     * @return if the mob is levelled or not
     */
    public boolean isLevelled(@NotNull LivingEntity livingEntity) {
        return livingEntity.getPersistentDataContainer().has(main.levelManager.levelKey, PersistentDataType.INTEGER);
    }

    /**
     * Retrieve the level of a levelled mob.
     * <p>
     * Thread-safe (intended, but not tested)
     *
     * @param livingEntity the levelled mob to get the level of
     * @return the mob's level
     */
    @SuppressWarnings("ConstantConditions")
    public int getLevelOfMob(@NotNull LivingEntity livingEntity) {
        if (livingEntity.getPersistentDataContainer().has(main.levelManager.levelKey, PersistentDataType.STRING)) {
            return livingEntity.getPersistentDataContainer().get(main.levelManager.levelKey, PersistentDataType.INTEGER);
        } else {
            throw new IllegalStateException("Mob is not levelled!");
        }
    }

    public void applyLevelledEquipment(@NotNull LivingEntity livingEntity, int level) {
        Validate.isTrue(isLevelled(livingEntity), "Entity must be levelled.");
        Validate.isTrue(level >= 0, "Level must be greater than or equal to zero.");

        //TODO add the rest
    }

    /**
     * This provides information on if a mob
     * is levellable or not, and if not,
     * a reason is supplied.
     */
    public enum LevellableState {
        ALLOWED,
        DENIED_BLOCKED_WORLD,
        DENIED_BLOCKED_ENTITY_TYPE
        //TODO add the rest.
    }
}
