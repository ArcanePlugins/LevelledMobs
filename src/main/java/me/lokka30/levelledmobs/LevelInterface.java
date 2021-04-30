package me.lokka30.levelledmobs;

import me.lokka30.levelledmobs.events.MobPostLevelEvent;
import me.lokka30.levelledmobs.events.MobPreLevelEvent;
import me.lokka30.levelledmobs.events.SummonedMobPreLevelEvent;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.misc.Addition;
import me.lokka30.levelledmobs.misc.DebugType;
import me.lokka30.levelledmobs.misc.ModalList;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Welcome to the LevelInterface,
 * this class is a 'global' interface
 * for LM itself AND other plugins to
 * apply and modify the main functions
 * of LevelledMobs.
 */
public class LevelInterface {

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
     * The following entity types must be manually ALLOWED in 'getLevellableState',
     * as they are not instanceof Monster or Boss
     * Stored as Strings since older versions may not contain certain entity type constants
     */
    public final HashSet<String> OTHER_HOSTILE_MOBS = new HashSet<>(Arrays.asList("GHAST", "HOGLIN", "SHULKER", "PHANTOM", "ENDER_DRAGON", "SLIME", "MAGMA_CUBE", "ZOMBIFIED_PIGLIN"));

    /**
     * Check if an existing mob is allowed to be levelled, according to the
     * user's configuration.
     * <p>
     * Thread-safety intended, but not tested.
     *
     * @param livingEntity target mob
     * @return if the mob is allowed to be levelled (yes/no), with reason
     */
    @NotNull
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
        if (ExternalCompatibilityManager.hasMythicMobsInstalled() && ExternalCompatibilityManager.checkMythicMobs(livingEntity))
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

            // Check ModalList
        if (Utils.isBabyMob(livingEntity) && shouldBabyMobBeDenied(livingEntity))
            return LevellableState.DENIED_CONFIGURATION_BLOCKED_ENTITY_TYPE;

        /*
        Check spawn reason
         */
        // They might not have the SpawnReasonKey, make sure they do before checking it.
        if (livingEntity.getPersistentDataContainer().has(main.levelManager.spawnReasonKey, PersistentDataType.STRING)) {
            String spawnReason = livingEntity.getPersistentDataContainer().get(main.levelManager.spawnReasonKey, PersistentDataType.STRING);

            if (!ModalList.isEnabledInList(main.settingsCfg, "allowed-spawn-reasons-list", spawnReason)) {
                return LevelInterface.LevellableState.DENIED_CONFIGURATION_BLOCKED_SPAWN_REASON;
            }
        }

        return getLevellableState(livingEntity.getType(), livingEntity.getLocation());
    }

    private boolean shouldBabyMobBeDenied(final LivingEntity livingEntity){
        final boolean babyMobsInheritAdultSetting = main.settingsCfg.getBoolean("allowed-entities-list.babyMobsInheritAdultSetting", true);
        final boolean isBabyVariantNotLevellable = !ModalList.isEnabledInList(main.settingsCfg, "allowed-entities-list", "BABY_" + livingEntity.getType());

        if (!babyMobsInheritAdultSetting)
            return isBabyVariantNotLevellable;

        // if baby is specified, use that setting. otherwise, use adult setting.
        if (main.settingsCfg.getStringList("allowed-entities-list.list").contains("BABY_" + livingEntity.getType()))
            return isBabyVariantNotLevellable;

        return !ModalList.isEnabledInList(main.settingsCfg, "allowed-entities-list", livingEntity.getType().toString());
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
     * Thread-safety intended, but not tested.
     *
     * @param entityType target entity type
     * @return of the mob is allowed to be levelled (yes/no), with reason
     */
    @NotNull
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
        if (main.configUtils.overridenEntities.contains(entityType.toString()))
            return LevellableState.ALLOWED;

        // Check ModalList
        if (!ModalList.isEnabledInList(main.settingsCfg, "allowed-entities-list", entityType.toString()))
            return LevellableState.DENIED_CONFIGURATION_BLOCKED_ENTITY_TYPE;

        // Entity types that have to be manually checked
        if (OTHER_HOSTILE_MOBS.contains(entityType.toString())) return LevellableState.ALLOWED;

        // if override level has been specified for min or max then allow it
        if (main.configUtils.entityTypesLevelOverride_Min.containsKey(entityType.toString()) ||
                main.configUtils.entityTypesLevelOverride_Max.containsKey(entityType.toString()))
            return LevellableState.ALLOWED;

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
     *
     * Thread-safety intended, but not tested.
     *
     * @param entityType target entity type
     * @param location   target location
     * @return if the mob is allowed to be levelled (yes/no), with reason
     */
    @NotNull
    public LevellableState getLevellableState(@NotNull EntityType entityType, @NotNull Location location) {

        // Check EntityType
        LevellableState entityTypeState = getLevellableState(entityType);
        if (entityTypeState != LevellableState.ALLOWED) return entityTypeState;

        // Check worlds
        //noinspection ConstantConditions
        if (!ModalList.isEnabledInList(main.settingsCfg, "allowed-worlds-list", location.getWorld().getName())) {
            return LevellableState.DENIED_CONFIGURATION_BLOCKED_WORLD;
        }

        // Check WorldGuard
        if (ExternalCompatibilityManager.checkWorldGuard(location, main))
            return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_WORLD_GUARD;

        return LevellableState.ALLOWED;
    }

    /**
     * This method generates a level for the mob. It utilises the levelling mode
     * specified by the administrator through the settings.yml configuration.
     *
     * Thread-safety intended, but not tested.
     *
     * @param livingEntity the entity to generate a level for
     * @return a level for the entity
     */
    public int generateLevel(@NotNull LivingEntity livingEntity) {
        return main.levelManager.generateLevel(livingEntity);
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
     * Thread-safety intended, but not tested.
     *
     * @param livingEntity target mob
     * @param level        the level the mob should have
     * @param isSummoned   if the mob was spawned by LevelledMobs, not by the server
     * @param bypassLimits whether LM should disregard max level, etc.
     */
    public void applyLevelToMob(@NotNull LivingEntity livingEntity, int level, boolean isSummoned, boolean bypassLimits, @NotNull HashSet<AdditionalLevelInformation> additionalLevelInformation) {
        assert level >= 0; //Level must be greater than or equal to zero.
        assert bypassLimits || isSummoned || getLevellableState(livingEntity) == LevellableState.ALLOWED; //Mob must be levellable or bypassLimits must be true

        if (isSummoned) {
            SummonedMobPreLevelEvent summonedMobPreLevelEvent = new SummonedMobPreLevelEvent(livingEntity, level);
            Bukkit.getPluginManager().callEvent(summonedMobPreLevelEvent);
            if (summonedMobPreLevelEvent.isCancelled()) return;
        } else {
            MobPreLevelEvent mobPreLevelEvent = new MobPreLevelEvent(livingEntity, level, MobPreLevelEvent.LevelCause.NORMAL, additionalLevelInformation);
            Bukkit.getPluginManager().callEvent(mobPreLevelEvent);
            if (mobPreLevelEvent.isCancelled()) return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (livingEntity.getPersistentDataContainer().has(main.levelManager.noLevelKey, PersistentDataType.STRING)) {
                    Utils.debugLog(main, DebugType.APPLY_LEVEL_FAIL, "Entity " + livingEntity.getType() + " had noLevelKey attached");
                    return;
                }

                livingEntity.getPersistentDataContainer().set(main.levelManager.levelKey, PersistentDataType.INTEGER, level);

                main.levelManager.applyLevelledAttributes(livingEntity, level, Addition.ATTRIBUTE_ATTACK_DAMAGE);
                main.levelManager.applyLevelledAttributes(livingEntity, level, Addition.ATTRIBUTE_MAX_HEALTH);
                main.levelManager.applyLevelledAttributes(livingEntity, level, Addition.ATTRIBUTE_MOVEMENT_SPEED);

                if (livingEntity instanceof Creeper) {
                    main.levelManager.applyCreeperBlastRadius((Creeper) livingEntity, level);
                }

                main.levelManager.updateNametag(livingEntity, level);
                main.levelManager.applyLevelledEquipment(livingEntity, level);

                MobPostLevelEvent.LevelCause levelCause = isSummoned ? MobPostLevelEvent.LevelCause.SUMMONED : MobPostLevelEvent.LevelCause.NORMAL;
                Bukkit.getPluginManager().callEvent(new MobPostLevelEvent(livingEntity, level, levelCause, additionalLevelInformation));

                final StringBuilder sb = new StringBuilder();
                sb.append("entity: ");
                sb.append(livingEntity.getName());
                sb.append(", world: ");
                sb.append(livingEntity.getWorld().getName());
                sb.append(", level: ");
                sb.append(level);
                if (isSummoned) sb.append(" (summoned)");
                if (bypassLimits) sb.append(" (limit bypass)");
                if (Utils.isBabyMob(livingEntity)) sb.append(" (baby)");

                Utils.debugLog(main, DebugType.APPLY_LEVEL_SUCCESS, sb.toString());
            }
        }.runTaskLater(main, 1);
    }

    public enum AdditionalLevelInformation {
        NOT_APPLICABLE,

        FROM_CHUNK_LISTENER,
        FROM_TRANSFORM_LISTENER,
        FROM_TAME_LISTENER
    }

    /**
     * Check if a LivingEntity is a levelled mob or not.
     * This is determined *after* MobPreLevelEvent.
     * <p>
     * Thread-safety intended, but not tested.
     *
     * @param livingEntity living entity to check
     * @return if the mob is levelled or not
     */
    public boolean isLevelled(@NotNull LivingEntity livingEntity) {
        return livingEntity.getPersistentDataContainer().has(main.levelManager.levelKey, PersistentDataType.INTEGER);
    }

    /**
     * Retrieve the level of a levelled mob.
     *
     * Thread-safety intended, but not tested.
     *
     * @param livingEntity the levelled mob to get the level of
     * @return the mob's level
     */
    @SuppressWarnings("ConstantConditions")
    public int getLevelOfMob(@NotNull LivingEntity livingEntity) {
        assert isLevelled(livingEntity);
        return livingEntity.getPersistentDataContainer().get(main.levelManager.levelKey, PersistentDataType.INTEGER);
    }

    /**
     * Un-level a mob.
     *
     * @param livingEntity levelled mob to un-level
     */
    public void removeLevel(@NotNull LivingEntity livingEntity) {
        assert isLevelled(livingEntity);

        // remove PDC value
        if (livingEntity.getPersistentDataContainer().has(main.levelManager.levelKey, PersistentDataType.INTEGER))
            livingEntity.getPersistentDataContainer().remove(main.levelManager.levelKey);

        // reset attributes
        for (Attribute attribute : Attribute.values()) {
            final AttributeInstance attInst = livingEntity.getAttribute(attribute);

            if (attInst == null) continue;

            Object defaultValueObj = main.mobDataManager.getAttributeDefaultValue(livingEntity.getType(), attribute);
            if (defaultValueObj == null) continue;

            double defaultValue;
            if (defaultValueObj instanceof Double) {
                defaultValue = (Double) defaultValueObj;
            } else {
                continue;
            }

            attInst.setBaseValue(defaultValue);
        }

        // update nametag
        main.levelManager.updateNametagWithDelay(livingEntity,
                livingEntity.getCustomName(),
                livingEntity.getWorld().getPlayers(),
                1);
    }

    /**
     * This provides information on if a mob
     * is levellable or not, and if not,
     * a reason is supplied.
     * A mob is levellable if their LevellableState = ALLOW.
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
         * settings.yml has been configured to block mobs
         * that spawn with a specific SpawnReason through
         * CreatureSpawnEvent.
         */
        DENIED_CONFIGURATION_BLOCKED_SPAWN_REASON,

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
        DENIED_OTHER
    }
}
