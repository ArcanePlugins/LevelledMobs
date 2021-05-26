package me.lokka30.levelledmobs;

import me.lokka30.levelledmobs.events.MobPostLevelEvent;
import me.lokka30.levelledmobs.events.MobPreLevelEvent;
import me.lokka30.levelledmobs.events.SummonedMobPreLevelEvent;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.misc.*;
import me.lokka30.levelledmobs.rules.MobCustomNameStatusEnum;
import me.lokka30.levelledmobs.rules.MobTamedStatusEnum;
import me.lokka30.levelledmobs.rules.RulesManager;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;

/**
 * Welcome to the LevelInterface,
 * this class is a 'global' interface
 * for LM itself AND other plugins to
 * apply and modify the main functions
 * of LevelledMobs.
 *
 * @author lokka30
 * @since 2.5
 */
public class LevelInterface {

    private final LevelledMobs main;
    private final RulesManager rulesManager;
    public LevelInterface(@NotNull final LevelledMobs main) {
        this.main = main;
        this.rulesManager = main.rulesManager;
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
     * @param lmInterface target mob
     * @return if the mob is allowed to be levelled (yes/no), with reason
     */
    @NotNull
    public LevellableState getLevellableState(@NotNull final LivingEntityInterface lmInterface) {
        /*
        Certain entity types are force-blocked, regardless of what the user has configured.
        This is also ran in getLevellableState(EntityType), however it is important that this is ensured
        before all other checks are made.
         */
        if (FORCED_BLOCKED_ENTITY_TYPES.contains(lmInterface.getTypeName()))
            return LevellableState.DENIED_FORCE_BLOCKED_ENTITY_TYPE;

        if (lmInterface.getApplicableRules().isEmpty())
            return LevellableState.DENIED_NO_APPLICABLE_RULES;

        // Check WorldGuard
        if (ExternalCompatibilityManager.checkWorldGuard(lmInterface.getLocation(), main))
            return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_WORLD_GUARD;

        if (!rulesManager.getRule_IsMobAllowedInEntityOverride(lmInterface))
            return LevellableState.DENIED_CONFIGURATION_BLOCKED_ENTITY_TYPE;

        if (!(lmInterface instanceof LivingEntityWrapper))
            return LevellableState.ALLOWED;

        LivingEntityWrapper lmEntity = (LivingEntityWrapper) lmInterface;

        /*
        Compatibility with other plugins: users may want to stop LM from acting on mobs modified by other plugins.
         */
        if (ExternalCompatibilityManager.hasMythicMobsInstalled() && ExternalCompatibilityManager.checkMythicMobs(lmEntity))
            return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_MYTHIC_MOBS;
        if (ExternalCompatibilityManager.checkDangerousCaves(lmEntity))
            return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_DANGEROUS_CAVES;
        if (ExternalCompatibilityManager.checkEliteMobs(lmEntity))
            return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_ELITE_MOBS;
        if (ExternalCompatibilityManager.checkInfernalMobs(lmEntity))
            return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_INFERNAL_MOBS;
        if (ExternalCompatibilityManager.checkCitizens(lmEntity))
            return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_CITIZENS;
        if (ExternalCompatibilityManager.checkShopkeepers(lmEntity))
            return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_SHOPKEEPERS;
        if (ExternalCompatibilityManager.checkWorldGuard(lmEntity.getLivingEntity().getLocation(), main))
            return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_WORLD_GUARD;

        if (lmEntity.isMobOfExternalType()) {
            lmEntity.invalidateCache();

            if (!rulesManager.getRule_IsMobAllowedInEntityOverride(lmInterface))
                return LevellableState.DENIED_CONFIGURATION_BLOCKED_ENTITY_TYPE;
        }

        /*
        Check 'No Level Conditions'
         */
        // Nametagged mobs.
        if (lmEntity.getLivingEntity().getCustomName() != null &&
                rulesManager.getRule_MobCustomNameStatus(lmEntity) == MobCustomNameStatusEnum.NOT_NAMETAGGED)
            return LevellableState.DENIED_CONFIGURATION_CONDITION_NAMETAGGED;

        // Tamed mobs.
        if (lmEntity.isMobTamed() &&
                rulesManager.getRule_MobTamedStatus(lmEntity) == MobTamedStatusEnum.NOT_TAMED)
            return LevellableState.DENIED_CONFIGURATION_CONDITION_TAMED;

        return LevellableState.ALLOWED;
    }

    /**
     * This method generates a level for the mob. It utilises the levelling mode
     * specified by the administrator through the settings.yml configuration.
     *
     * Thread-safety intended, but not tested.
     *
     * @param lmEntity the entity to generate a level for
     * @return a level for the entity
     */
    public int generateLevel(@NotNull final LivingEntityWrapper lmEntity) {
        return main.levelManager.generateLevel(lmEntity);
    }

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
    public int generateLevel(@NotNull final LivingEntityWrapper lmEntity, final int minLevel, final int maxLevel) {
        return main.levelManager.generateLevel(lmEntity, minLevel, maxLevel);
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
     * @param lmEntity target mob
     * @param level        the level the mob should have
     * @param isSummoned   if the mob was spawned by LevelledMobs, not by the server
     * @param bypassLimits whether LM should disregard max level, etc.
     * @param additionalLevelInformation used to determine the source event
     */
    public void applyLevelToMob(@NotNull final LivingEntityWrapper lmEntity, final int level, final boolean isSummoned, final boolean bypassLimits, @NotNull final HashSet<AdditionalLevelInformation> additionalLevelInformation) {
        assert level >= 0;
        assert bypassLimits || isSummoned || getLevellableState(lmEntity) == LevellableState.ALLOWED;

        if (isSummoned) {
            SummonedMobPreLevelEvent summonedMobPreLevelEvent = new SummonedMobPreLevelEvent(lmEntity.getLivingEntity(), level);

            final BukkitRunnable runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    Bukkit.getPluginManager().callEvent(summonedMobPreLevelEvent);
                }
            };
            runnable.runTask(main);

            if (summonedMobPreLevelEvent.isCancelled()) return;
        } else {
            MobPreLevelEvent mobPreLevelEvent = new MobPreLevelEvent(lmEntity.getLivingEntity(), level, MobPreLevelEvent.LevelCause.NORMAL, additionalLevelInformation);
            final BukkitRunnable runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    Bukkit.getPluginManager().callEvent(mobPreLevelEvent);
                }
            };
            runnable.runTask(main);
            if (mobPreLevelEvent.isCancelled()) return;
        }

        boolean hasNoLevelKey;
        synchronized (lmEntity.pdcSyncObject) {
            hasNoLevelKey = lmEntity.getPDC().has(main.levelManager.noLevelKey, PersistentDataType.STRING);
        }

        if (hasNoLevelKey) {
            Utils.debugLog(main, DebugType.APPLY_LEVEL_FAIL, "Entity " + lmEntity.getTypeName() + " had noLevelKey attached");
            return;
        }

        synchronized (lmEntity.pdcSyncObject) {
            lmEntity.getPDC().set(main.levelManager.levelKey, PersistentDataType.INTEGER, level);
        }
        lmEntity.invalidateCache();

        // setting attributes should be only done in the main thread.
        final BukkitRunnable applyAttribs = new BukkitRunnable() {
            @Override
            public void run() {
                synchronized (main.attributeSyncObject) {
                    main.levelManager.applyLevelledAttributes(lmEntity, Addition.ATTRIBUTE_ATTACK_DAMAGE);
                    main.levelManager.applyLevelledAttributes(lmEntity, Addition.ATTRIBUTE_MAX_HEALTH);
                    main.levelManager.applyLevelledAttributes(lmEntity, Addition.ATTRIBUTE_MOVEMENT_SPEED);
                }

                if (lmEntity.getLivingEntity() instanceof Creeper) {
                    main.levelManager.applyCreeperBlastRadius(lmEntity, level);
                }
            }
        };
        applyAttribs.runTask(main);

        main.levelManager.updateNametag_WithDelay(lmEntity);
        main.levelManager.applyLevelledEquipment(lmEntity, lmEntity.getMobLevel());

        MobPostLevelEvent.LevelCause levelCause = isSummoned ? MobPostLevelEvent.LevelCause.SUMMONED : MobPostLevelEvent.LevelCause.NORMAL;
        final BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.getPluginManager().callEvent(new MobPostLevelEvent(lmEntity, levelCause, additionalLevelInformation));
            }
        };
        runnable.runTask(main);

        final StringBuilder sb = new StringBuilder();
        sb.append("entity: ");
        sb.append(lmEntity.getLivingEntity().getName());
        sb.append(", world: ");
        sb.append(lmEntity.getWorldName());
        sb.append(", level: ");
        sb.append(level);
        if (isSummoned) sb.append(" (summoned)");
        if (bypassLimits) sb.append(" (limit bypass)");
        if (lmEntity.isBabyMob()) sb.append(" (baby)");

        Utils.debugLog(main, DebugType.APPLY_LEVEL_SUCCESS, sb.toString());
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
    public boolean isLevelled(@NotNull final LivingEntity livingEntity) {
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
    public int getLevelOfMob(@NotNull final LivingEntity livingEntity) {
        assert isLevelled(livingEntity);
        return Objects.requireNonNull(livingEntity.getPersistentDataContainer().get(main.levelManager.levelKey, PersistentDataType.INTEGER), "levelKey was null");
    }

    /**
     * Un-level a mob.
     *
     * @param lmEntity levelled mob to un-level
     */
    public void removeLevel(@NotNull final LivingEntityWrapper lmEntity) {
        assert lmEntity.isLevelled();

        // remove PDC value
        synchronized (lmEntity.pdcSyncObject) {
            if (lmEntity.getPDC().has(main.levelManager.levelKey, PersistentDataType.INTEGER))
                lmEntity.getPDC().remove(main.levelManager.levelKey);
            if (lmEntity.getPDC().has(main.levelManager.overridenEntityNameKey, PersistentDataType.STRING))
                lmEntity.getPDC().remove(main.levelManager.overridenEntityNameKey);
        }

        // reset attributes
        synchronized (main.attributeSyncObject) {
            for (Attribute attribute : Attribute.values()) {
                final AttributeInstance attInst = lmEntity.getLivingEntity().getAttribute(attribute);

                if (attInst == null) continue;

                attInst.getModifiers().clear();
            }
        }

        if (lmEntity.getLivingEntity() instanceof Creeper)
            ((Creeper) lmEntity.getLivingEntity()).setExplosionRadius(3);

        lmEntity.invalidateCache();

        // update nametag
        main.levelManager.updateNametag(lmEntity, lmEntity.getLivingEntity().getCustomName());
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
         * If no rules in the rule list applied to the mob
         * then it will be denied
         */
        DENIED_NO_APPLICABLE_RULES,

        /**
         * When a reason is not applicable, use this.
         * Please contact a lead developer if you
         * believe you must resort to using this.
         */
        DENIED_OTHER
    }
}
