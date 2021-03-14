package me.lokka30.levelledmobs;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;

public class LevelInterface {

    private final LevelledMobs main;

    public LevelInterface(final LevelledMobs main) {
        this.main = main;
    }

    /**
     * Check if an existing mob is allowed to be levelled, according to the
     * user's configuration.
     *
     * @param livingEntity target mob
     * @return if the mob is allowed to be levelled (yes/no), with reason
     */
    public LevellableState isLevellable(LivingEntity livingEntity) {
        //TODO
        return null;
    }

    /**
     * Check if a mob is allowed to be levelled, according to the
     * user's configuration.
     * Developers, please ensure you understand that this method
     * does not account for certain things such as 'is mob tamed?',
     * WorldGuard regions, blocked worlds, etc,
     * which the user may have disabled, and this method is unable to
     * factor that in. Where possible, use isLevellable(LivingEntity).
     *
     * @param entityType target entity type
     * @return of the mob is allowed to be levelled (yes/no), with reason
     */
    public LevellableState isLevellable(EntityType entityType) {
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
     *
     * @param entityType target entity type
     * @param location   target location
     * @return if the mob is allowed to be levelled (yes/no), with reason
     */
    public LevellableState isLevellable(EntityType entityType, Location location) {
        //TODO
        return null;
    }

    public int generateLevelForMob(LivingEntity livingEntity) {
        //TODO
        return -1;
    }

    /**
     * This method applies a level to the target mob.
     * Note: it is highly recommended to leave bypassLimits = false,
     * unless the desired behaviour is to override LevelledMobs'
     * configured limits.
     *
     * @param livingEntity target mob
     * @param level        the level the mob should have
     * @param bypassLimits whether LM should disregard max level, etc.
     */
    public void applyLevelToMob(LivingEntity livingEntity, int level, boolean bypassLimits) {
        //TODO
    }

    /**
     * Check if a LivingEntity is a levelled mob or not.
     * This is determined *after* MobLevelEvent.
     *
     * @param livingEntity living entity to check
     * @return if the mob is levelled or not
     */
    public boolean isLevelled(LivingEntity livingEntity) {
        return livingEntity.getPersistentDataContainer().has(main.levelManager.levelKey, PersistentDataType.INTEGER);
    }

    /**
     * Retrieve the level of a levelled mob.
     *
     * @param livingEntity the levelled mob to get the level of
     * @return the mob's level
     */
    @SuppressWarnings("ConstantConditions")
    public int getLevelOfMob(LivingEntity livingEntity) {
        if (livingEntity.getPersistentDataContainer().has(main.levelManager.levelKey, PersistentDataType.STRING)) {
            return livingEntity.getPersistentDataContainer().get(main.levelManager.levelKey, PersistentDataType.INTEGER);
        } else {
            throw new IllegalStateException("Mob is not levelled!");
        }
    }

    /**
     * This object is returned by 'isLevellable' methods in this class.
     * If isLevellable is false, developers are expected to supply a
     * reason as to why this is the case. Otherwise, a reason should
     * not be supplied since it is completely unnecessary to do so.
     */
    public static class LevellableState {
        final boolean isLevellable;
        final String reason;

        public LevellableState(boolean isLevellable, String reason) {
            this.isLevellable = isLevellable;
            this.reason = reason;
        }

        public String getReason() {
            if (!isLevellable) return reason;
            return null;
        }
    }
}
