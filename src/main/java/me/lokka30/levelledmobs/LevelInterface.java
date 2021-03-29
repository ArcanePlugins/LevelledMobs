package me.lokka30.levelledmobs;

import me.lokka30.levelledmobs.events.MobLevelEvent;
import me.lokka30.levelledmobs.events.SummonedMobLevelEvent;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.misc.ModalList;
import me.lokka30.levelledmobs.misc.Utils;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;

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
     * The following entity types MUST be not levellable.
     * Stored as Strings since older versions may not contain certain entity type constants
     */
    public final HashSet<String> FORCED_BLOCKED_ENTITY_TYPES = new HashSet<>(Arrays.asList("PLAYER", "UNKNOWN", "ARMOR_STAND", "NPC"));

    /**
     * The following entity types must be manually ALLOWED in 'getLevellableState'.
     * Stored as Strings since older versions may not contain certain entity type constants
     */
    public final HashSet<String> EXCLUDED_ENTITY_TYPES = new HashSet<>(Arrays.asList("GHAST", "HOGLIN", "SHULKER", "PHANTOM", "ENDER_DRAGON", "SLIME", "MAGMA_CUBE", "ZOMBIFIED_PIGLIN"));

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
        /*
        Certain entity types are force-blocked, regardless of what the user has configured.
        This is also ran in getLevellableState(EntityType), however it is important that this is ensured
        before all other checks are made.
         */
        if (FORCED_BLOCKED_ENTITY_TYPES.contains(livingEntity.getType().toString()))
            return LevellableState.DENIED_FORCE_BLOCKED_ENTITY_TYPE;

        /*
        Compatibility with other plugins: users may want to stop LM from acting on mobs modified by other plugins.
         */
        if (ExternalCompatibilityManager.checkMythicMobs(livingEntity))
            return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_MYTHIC_MOBS;
        if (ExternalCompatibilityManager.checkDangerousCaves(livingEntity))
            return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_DANGEROUS_CAVES;
        if (ExternalCompatibilityManager.checkEliteMobs(livingEntity))
            return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_ELITE_MOBS;
        if (ExternalCompatibilityManager.checkInfernalMobs(livingEntity))
            return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_INFERNAL_MOBS;
        if (ExternalCompatibilityManager.checkCitizens(livingEntity))
            return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_CITIZENS;
        if (ExternalCompatibilityManager.checkShopkeepers(livingEntity))
            return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_SHOPKEEPERS;
        if (ExternalCompatibilityManager.checkWorldGuard(livingEntity.getLocation(), main))
            return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_WORLD_GUARD;

        /*
        Check 'No Level Conditions'
         */
        // Nametagged mobs.
        if (livingEntity.getCustomName() != null && main.settingsCfg.getBoolean("no-level-conditions.nametagged"))
            return LevellableState.DENIED_CONFIGURATION_CONDITION_NAMETAGGED;

        // Tamed mobs.
        if (livingEntity instanceof Tameable && ((Tameable) livingEntity).isTamed() && main.settingsCfg.getBoolean("no-level-conditions.tamed"))
            return LevellableState.DENIED_CONFIGURATION_CONDITION_TAMED;

        /*
        Check Entity Type
         */
        // Overriden entities.
        if (Utils.isBabyMob(livingEntity)) {
            if (!main.settingsCfg.getStringList("overriden-entities").contains("BABY_" + livingEntity.getType().toString()))
                return LevellableState.DENIED_CONFIGURATION_BLOCKED_ENTITY_TYPE;
        }

        // Check ModalList
        if (Utils.isBabyMob(livingEntity)) {
            if (!ModalList.isEnabledInList(main.settingsCfg, "allowed-entities-list", "BABY_" + livingEntity.getType().toString()))
                return LevellableState.DENIED_CONFIGURATION_BLOCKED_ENTITY_TYPE;
        }

        return getLevellableState(livingEntity.getType());
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
        /*
        Certain entity types are force-blocked, regardless of what the user has configured.
         */
        if (FORCED_BLOCKED_ENTITY_TYPES.contains(entityType.toString()))
            return LevellableState.DENIED_FORCE_BLOCKED_ENTITY_TYPE;

        /*
        Check Entity Type
         */
        // Overriden entities.
        if (main.settingsCfg.getStringList("overriden-entities").contains(entityType.toString()))
            return LevellableState.DENIED_CONFIGURATION_BLOCKED_ENTITY_TYPE;

        // Check ModalList
        if (!ModalList.isEnabledInList(main.settingsCfg, "allowed-entities-list", entityType.toString()))
            return LevellableState.DENIED_CONFIGURATION_BLOCKED_ENTITY_TYPE;

        // Entity types that have to be manually checked
        if (EXCLUDED_ENTITY_TYPES.contains(entityType.toString())) return LevellableState.ALLOWED;

        /*
        Check Entity Class
        */
        Class<? extends Entity> entityClass = entityType.getEntityClass();
        if (entityClass == null) return LevellableState.DENIED_CONFIGURATION_BLOCKED_ENTITY_TYPE;

        return Monster.class.isAssignableFrom(entityClass)
                || Boss.class.isAssignableFrom(entityClass)
                || main.settingsCfg.getBoolean("level-passive")
                ? LevellableState.ALLOWED : LevellableState.DENIED_CONFIGURATION_BLOCKED_ENTITY_TYPE;
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

        // Check EntityType
        LevellableState entityTypeState = getLevellableState(entityType);
        if (entityTypeState != LevellableState.ALLOWED) return entityTypeState;

        // Check WorldGuard
        if (ExternalCompatibilityManager.checkWorldGuard(location, main))
            return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_WORLD_GUARD;

        return LevellableState.ALLOWED;
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

    /**
     * Add configured equipment to the levelled mob
     * LivingEntity MUST be a levelled mob
     *
     * @param livingEntity a levelled mob to apply levelled equipment to
     * @param level        the level of the levelled mob
     */
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
        /**
         * The entity is ALLOWED to be levelled.
         * Note to developers: there must only be
         * one 'ALLOWED' constant.
         */
        ALLOWED,

        /**
         * the plugin force blocked an entity type, such as a PLAYER
         * or ARMOR STAND which are not meant to be 'levelled mobs'.
         */
        DENIED_FORCE_BLOCKED_ENTITY_TYPE,

        /**
         * settings.yml has been configured to block mobs
         * spawning in entity's world from being levelled
         */
        DENIED_CONFIGURATION_BLOCKED_WORLD,

        /**
         * settings.yml has been configured to block mobs
         * of such entity type from being levelled
         */
        DENIED_CONFIGURATION_BLOCKED_ENTITY_TYPE,

        /**
         * settings.yml has been configured to block
         * DangerousCaves mobs from being levelled.
         */
        DENIED_CONFIGURATION_COMPATIBILITY_DANGEROUS_CAVES,

        /**
         * settings.yml has been configured to block
         * MythicMobs mobs from being levelled.
         */
        DENIED_CONFIGURATION_COMPATIBILITY_MYTHIC_MOBS,

        /**
         * settings.yml has been configured to block
         * EliteMobs mobs from being levelled.
         */
        DENIED_CONFIGURATION_COMPATIBILITY_ELITE_MOBS,

        /**
         * settings.yml has been configured to block
         * Infernal Mobs mobs from being levelled.
         */
        DENIED_CONFIGURATION_COMPATIBILITY_INFERNAL_MOBS,

        /**
         * settings.yml has been configured to block
         * Citizens NPCs from being levelled.
         */
        DENIED_CONFIGURATION_COMPATIBILITY_CITIZENS,

        /**
         * settings.yml has been configured to block
         * Shopkeepers NPCs from being levelled.
         */
        DENIED_CONFIGURATION_COMPATIBILITY_SHOPKEEPERS,

        /**
         * WorldGuard region flag states that
         * mobs are not levellable in its region
         */
        DENIED_CONFIGURATION_COMPATIBILITY_WORLD_GUARD,

        /**
         * settings.yml has been configured to block
         * nametagged mobs from being levelled.
         */
        DENIED_CONFIGURATION_CONDITION_NAMETAGGED,

        /**
         * settings.yml has been configured to block
         * tamed mobs from being levelled.
         */
        DENIED_CONFIGURATION_CONDITION_TAMED,

        /**
         * When a reason is not applicable, use this.
         * Please contact a lead developer if you
         * believe you must resort to using this.
         */
        DENIED_UNKNOWN
    }
}
