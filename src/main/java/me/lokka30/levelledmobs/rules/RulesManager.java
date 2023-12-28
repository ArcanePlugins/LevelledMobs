/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.rules;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.LivingEntityInterface;
import me.lokka30.levelledmobs.managers.DebugManager;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.managers.WorldGuardIntegration;
import me.lokka30.levelledmobs.misc.CachedModalList;
import me.lokka30.levelledmobs.misc.DebugType;
import me.lokka30.levelledmobs.wrappers.LivingEntityWrapper;
import me.lokka30.levelledmobs.result.RuleCheckResult;
import me.lokka30.levelledmobs.rules.strategies.LevellingStrategy;
import me.lokka30.levelledmobs.util.Utils;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages all rules that are parsed from rules.yml and applied to various defined mobs
 *
 * @author stumper66
 * @since 3.0.0
 */
public class RulesManager {

    public RulesManager(final LevelledMobs main) {
        this.main = main;
        this.rulesInEffect = new TreeMap<>();
        this.biomeGroupMappings = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.ruleNameMappings = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.rulesCooldown = new TreeMap<>();
    }

    private final LevelledMobs main;
    public @NotNull final SortedMap<Integer, List<RuleInfo>> rulesInEffect;
    final Map<String, RuleInfo> ruleNameMappings;
    public @NotNull final Map<String, List<String>> biomeGroupMappings;
    final Map<String, List<Instant>> rulesCooldown;
    public boolean anyRuleHasChance;
    public boolean hasAnyWGCondition;
    private Instant lastRulesCheck;
    private @NotNull String currentRulesHash = "";
    final static Object ruleLocker = new Object();

    public boolean getRuleIsWorldAllowedInAnyRule(final @Nullable World world) {
        if (world == null) {
            return false;
        }
        boolean result = false;

        for (final RuleInfo ruleInfo : main.rulesParsingManager.getAllRules()) {
            if (!ruleInfo.ruleIsEnabled) {
                continue;
            }
            if (ruleInfo.conditions_Worlds != null && ruleInfo.conditions_Worlds.isEnabledInList(
                world.getName(), null)) {
                result = true;
                break;
            }
        }

        return result;
    }

    public @NotNull String getCurrentRulesHash(){
        return this.currentRulesHash;
    }

    @SuppressWarnings("unused")
    public void addCustomRule(final @Nullable RuleInfo ri){
        if (ri == null) return;

        main.rulesParsingManager.customRules.add(ri);
        main.rulesParsingManager.checkCustomRules();
    }

    public @NotNull List<String> getRuleNbtData(final @NotNull LivingEntityWrapper lmEntity) {
        final List<String> nbtData = new LinkedList<>();

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.mobNBT_Data != null) {
                final MergeableStringList nbt = ruleInfo.mobNBT_Data;
                if (!nbt.doMerge) {
                    nbtData.clear();
                }

                nbtData.addAll(nbt.items);
            }
        }

        return nbtData;
    }

    public double getRuleSunlightBurnIntensity(final @NotNull LivingEntityWrapper lmEntity) {
        double result = 0.0;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.sunlightBurnAmount != null) {
                result = ruleInfo.sunlightBurnAmount;
            }
        }

        return result;
    }

    public @Nullable Integer getRuleMaxRandomVariance(final @NotNull LivingEntityWrapper lmEntity) {
        Integer result = null;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.maxRandomVariance != null) {
                result = ruleInfo.maxRandomVariance;
            }
        }

        return result;
    }

    public boolean getRuleCheckIfNoDropMultiplierEntitiy(
            final @NotNull LivingEntityWrapper lmEntity) {
        CachedModalList<String> entitiesList = null;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.conditions_NoDropEntities != null) {
                entitiesList = ruleInfo.conditions_NoDropEntities;
            }
        }

        return entitiesList != null && entitiesList.isEnabledInList(lmEntity.getNameIfBaby(),
            lmEntity);
    }

    public @NotNull CustomDropsRuleSet getRuleUseCustomDropsForMob(
        final @NotNull LivingEntityWrapper lmEntity) {
        final CustomDropsRuleSet dropRules = new CustomDropsRuleSet();

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.customDrops_UseForMobs != null) {
                dropRules.useDrops = ruleInfo.customDrops_UseForMobs;
            }
            if (ruleInfo.chunkKillOptions != null) {
                if (dropRules.chunkKillOptions == null)
                    dropRules.chunkKillOptions = ruleInfo.chunkKillOptions;
                else{
                    dropRules.chunkKillOptions.merge(ruleInfo.chunkKillOptions);
                }
            }
            dropRules.useDropTableIds.addAll(ruleInfo.customDrop_DropTableIds);
        }

        if (lmEntity.lockedCustomDrops != null && !lmEntity.lockedCustomDrops.isEmpty()) {
            dropRules.useDropTableIds.clear();
            dropRules.useDropTableIds.addAll(lmEntity.lockedCustomDrops);
            dropRules.useDrops = true;
        }

        if (dropRules.chunkKillOptions == null)
            dropRules.chunkKillOptions = new ChunkKillOptions();

        if (lmEntity.hasLockedDropsOverride)
            dropRules.chunkKillOptions.disableVanillaDrops = true;

        return dropRules;
    }

    public boolean getRuleDoLockEntity(final @NotNull LivingEntityWrapper lmEntity) {
        boolean result = false;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.lockEntity != null) {
                result = ruleInfo.lockEntity;
            }
        }

        return result;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean getRuleIsMobAllowedInEntityOverride(
        final @NotNull LivingEntityInterface lmInterface) {
        // check if it should be denied thru the entity override list
        boolean babyMobsInheritAdultSetting = true; // default
        CachedModalList<String> allowedEntitiesList = null;
        for (final RuleInfo ruleInfo : lmInterface.getApplicableRules()) {
            if (ruleInfo.allowedEntities != null) {
                allowedEntitiesList = ruleInfo.allowedEntities;
            }
            if (ruleInfo.babyMobsInheritAdultSetting != null) {
                babyMobsInheritAdultSetting = ruleInfo.babyMobsInheritAdultSetting;
            }
        }

        if (lmInterface instanceof final LivingEntityWrapper lmEntity) {
            return (
                allowedEntitiesList == null ||
                    !babyMobsInheritAdultSetting && lmEntity.isBabyMob()
                        && Utils.isLivingEntityInModalList(allowedEntitiesList, lmEntity, true)) ||
                Utils.isLivingEntityInModalList(allowedEntitiesList, lmEntity,
                    babyMobsInheritAdultSetting);
        } else {
            return (
                allowedEntitiesList == null || allowedEntitiesList.isEnabledInList(
                    lmInterface.getTypeName(), null)
            );
        }
    }

    public @Nullable FineTuningAttributes getFineTuningAttributes(
        final @NotNull LivingEntityWrapper lmEntity) {
        FineTuningAttributes allMobAttribs = null;
        FineTuningAttributes thisMobAttribs = null;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.allMobMultipliers != null) {
                if (allMobAttribs == null || ruleInfo.allMobMultipliers.doNotMerge) {
                    allMobAttribs = ruleInfo.allMobMultipliers.cloneItem();
                    if (ruleInfo.allMobMultipliers.doNotMerge) {
                        thisMobAttribs = null;
                    }
                } else {
                    allMobAttribs.merge(ruleInfo.allMobMultipliers);
                }
            }

            if (ruleInfo.specificMobMultipliers != null
                && ruleInfo.specificMobMultipliers.containsKey(lmEntity.getNameIfBaby())) {
                final FineTuningAttributes tempAttribs = ruleInfo.specificMobMultipliers.get(
                    lmEntity.getNameIfBaby());
                if (thisMobAttribs == null || tempAttribs.doNotMerge) {
                    thisMobAttribs = tempAttribs.cloneItem();

                    if (tempAttribs.doNotMerge)
                        allMobAttribs = null;
                    else if (allMobAttribs != null) {
                        allMobAttribs.merge(thisMobAttribs);
                    }
                } else {
                    thisMobAttribs.merge(tempAttribs);
                }
            }
        }

        if (allMobAttribs != null) {
            allMobAttribs.merge(thisMobAttribs);
            return allMobAttribs;
        } else {
            return thisMobAttribs;
        }
    }

    public @NotNull Map<ExternalCompatibilityManager.ExternalCompatibility, Boolean> getRuleExternalCompatibility(
        final @NotNull LivingEntityWrapper lmEntity
    ) {
        final Map<ExternalCompatibilityManager.ExternalCompatibility, Boolean> result = new EnumMap<>(
            ExternalCompatibilityManager.ExternalCompatibility.class);

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.enabledExtCompats != null) {
                result.putAll(ruleInfo.enabledExtCompats);
            }
        }

        return result;
    }

    public boolean isPlayerLevellingEnabled() {
        for (final List<RuleInfo> rules : this.rulesInEffect.values()) {
            if (rules == null) {
                continue;
            }

            for (final RuleInfo ruleInfo : rules) {
                if (ruleInfo.ruleIsEnabled && ruleInfo.playerLevellingOptions != null) {
                    return true;
                }
            }
        }

        return false;
    }

    public int getRuleCreeperMaxBlastRadius(final @NotNull LivingEntityWrapper lmEntity) {
        int maxBlast = 5;
        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.creeperMaxDamageRadius != null) {
                maxBlast = ruleInfo.creeperMaxDamageRadius;
            }
        }

        return maxBlast;
    }

    public @Nullable LevellingStrategy getRuleLevellingStrategy(
        final @NotNull LivingEntityWrapper lmEntity) {
        LevellingStrategy levellingStrategy = null;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.levellingStrategy == null) continue;

            if (levellingStrategy != null && levellingStrategy.getClass()
                .equals(ruleInfo.levellingStrategy.getClass())) {
                levellingStrategy.mergeRule(ruleInfo.levellingStrategy);
            } else {
                levellingStrategy = ruleInfo.levellingStrategy.cloneItem();
            }
        }

        return levellingStrategy;
    }

    public boolean getRuleMobLevelInheritance(final @NotNull LivingEntityWrapper lmEntity) {
        boolean result = true;
        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.mobLevelInheritance != null) {
                result = ruleInfo.mobLevelInheritance;
            }
        }

        return result;
    }

    public MobCustomNameStatus getRuleMobCustomNameStatus(
        final @NotNull LivingEntityWrapper lmEntity) {
        MobCustomNameStatus result = MobCustomNameStatus.NOT_SPECIFIED;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.conditions_MobCustomnameStatus != MobCustomNameStatus.NOT_SPECIFIED) {
                result = ruleInfo.conditions_MobCustomnameStatus;
            }
        }

        return result;
    }

    public MobTamedStatus getRuleMobTamedStatus(final @NotNull LivingEntityWrapper lmEntity) {
        MobTamedStatus result = MobTamedStatus.NOT_SPECIFIED;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.conditions_MobTamedStatus != MobTamedStatus.NOT_SPECIFIED) {
                result = ruleInfo.conditions_MobTamedStatus;
            }
        }

        return result;
    }

    public int getRuleMobMinLevel(final @NotNull LivingEntityInterface lmInterface) {
        if (lmInterface.getSummonedLevel() != null) {
            return lmInterface.getSummonedLevel();
        }

        int minLevel = 1;

        for (final RuleInfo ruleInfo : lmInterface.getApplicableRules()) {
            if (ruleInfo.restrictions_MinLevel != null) {
                minLevel = ruleInfo.restrictions_MinLevel;
            }
        }

        return minLevel;
    }

    public int getRuleMobMaxLevel(final @NotNull LivingEntityInterface lmInterface) {
        int maxLevel = 0;
        int firstMaxLevel = -1;

        for (final RuleInfo ruleInfo : lmInterface.getApplicableRules()) {
            if (ruleInfo.restrictions_MaxLevel != null) {
                maxLevel = ruleInfo.restrictions_MaxLevel;
                if (firstMaxLevel < 0 && maxLevel > 0) {
                    firstMaxLevel = maxLevel;
                }
            }
        }

        if (maxLevel <= 0 && lmInterface.getSummonedLevel() != null) {
            if (maxLevel == 0 && firstMaxLevel > 0) {
                maxLevel = firstMaxLevel;
            }

            int summonedLevel = lmInterface.getSummonedLevel();
            if (summonedLevel > maxLevel) {
                maxLevel = summonedLevel;
            }
        }

        return maxLevel;
    }

    public @Nullable PlayerLevellingOptions getRulePlayerLevellingOptions(
        final @NotNull LivingEntityWrapper lmEntity) {
        PlayerLevellingOptions levellingOptions = null;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.playerLevellingOptions != null) {
                if (levellingOptions == null || !levellingOptions.doMerge)
                    levellingOptions = ruleInfo.playerLevellingOptions.cloneItem();
                else
                    levellingOptions.mergeRule(ruleInfo.playerLevellingOptions);
            }
        }

        return levellingOptions;
    }

    public @NotNull String getRuleNametag(final @NotNull LivingEntityWrapper lmEntity) {
        String nametag = "";
        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (!Utils.isNullOrEmpty(ruleInfo.nametag)) {
                nametag = "disabled".equalsIgnoreCase(ruleInfo.nametag) ?
                    "" : ruleInfo.nametag;
            }
        }

        return nametag;
    }

    public @Nullable String getRuleNametagPlaceholder(final @NotNull LivingEntityWrapper lmEntity) {
        String nametag = null;
        final boolean isLevelled = lmEntity.isLevelled();

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo == null) {
                continue;
            }
            final String nametagRule = isLevelled ?
                ruleInfo.nametag_Placeholder_Levelled : ruleInfo.nametag_Placeholder_Unlevelled;
            if (nametagRule != null) {
                nametag = nametagRule;
            }
        }

        return nametag;
    }

    public @NotNull String getRuleNametagCreatureDeath(final @NotNull LivingEntityWrapper lmEntity) {
        String nametag = "";
        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (!Utils.isNullOrEmpty(ruleInfo.nametag_CreatureDeath)) {
                nametag = ruleInfo.nametag_CreatureDeath;
            }
        }

        return nametag;
    }

    public @Nullable HealthIndicator getRuleNametagIndicator(final @NotNull LivingEntityWrapper lmEntity) {
        HealthIndicator indicator = null;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.healthIndicator != null) {
                if (indicator == null || !ruleInfo.healthIndicator.doMerge()) {
                    indicator = ruleInfo.healthIndicator.cloneItem();
                } else {
                    indicator.merge(ruleInfo.healthIndicator.cloneItem());
                }
            }
        }

        return indicator;
    }

    public @NotNull List<NametagVisibilityEnum> getRuleCreatureNametagVisbility(
        final @NotNull LivingEntityWrapper lmEntity) {
        List<NametagVisibilityEnum> result = null;

        try {
            for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
                if (ruleInfo == null) {
                    continue;
                }
                if (ruleInfo.nametagVisibilityEnum != null) {
                    result = ruleInfo.nametagVisibilityEnum;
                }
            }
        } catch (ConcurrentModificationException e) {
            Utils.logger.info(
                "Got ConcurrentModificationException in getRule_CreatureNametagVisbility");
        }

        if (result == null || result.isEmpty()) {
            return List.of(NametagVisibilityEnum.MELEE);
        } else {
            return result;
        }
    }

    public long getRuleNametagVisibleTime(final @NotNull LivingEntityWrapper lmEntity) {
        long result = 4000L;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.nametagVisibleTime != null) {
                result = ruleInfo.nametagVisibleTime;
            }
        }

        return result;
    }

    public @Nullable String getRuleTieredPlaceholder(final @NotNull LivingEntityWrapper lmEntity) {
        List<TieredColoringInfo> coloringInfo = null;
        String tieredText = null;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.tieredColoringInfos != null) {
                coloringInfo = ruleInfo.tieredColoringInfos;
            }
        }

        if (coloringInfo == null) {
            return null;
        }

        final int mobLevel = lmEntity.getMobLevel();
        for (final TieredColoringInfo info : coloringInfo) {
            if (info.isDefault) {
                tieredText = info.text;
            }
            if (mobLevel >= info.minLevel && mobLevel <= info.maxLevel) {
                tieredText = info.text;
                break;
            }
        }

        return tieredText;
    }

    public boolean getRulePassengerMatchLevel(final @NotNull LivingEntityWrapper lmEntity) {
        boolean result = false;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.passengerMatchLevel != null) {
                result = ruleInfo.passengerMatchLevel;
            }
        }

        return result;
    }

    @SuppressWarnings("deprecation")
    public @Nullable String getRuleEntityOverriddenName(final @NotNull LivingEntityWrapper lmEntity,
                                                        final boolean forceCustomName) {
        Map<String, List<LevelTierMatching>> entityNameOverrides_Level = null;
        Map<String, LevelTierMatching> entityNameOverrides = null;

        if (lmEntity.hasOverridenEntityName()) {
            return lmEntity.getOverridenEntityName();
        }

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            final boolean doMerge =
                ruleInfo.mergeEntityNameOverrides != null && ruleInfo.mergeEntityNameOverrides;
            if (ruleInfo.entityNameOverrides != null) {
                if (entityNameOverrides != null && doMerge) {
                    entityNameOverrides.putAll(ruleInfo.entityNameOverrides);
                } else {
                    entityNameOverrides = ruleInfo.entityNameOverrides;
                }
            }

            if (ruleInfo.entityNameOverrides_Level != null) {
                if (entityNameOverrides_Level != null && doMerge) {
                    entityNameOverrides_Level.putAll(ruleInfo.entityNameOverrides_Level);
                } else {
                    entityNameOverrides_Level = ruleInfo.entityNameOverrides_Level;
                }
            }
        }

        if (entityNameOverrides == null && entityNameOverrides_Level == null) {
            return null;
        }

        List<String> namesInfo = null;
        final LevelTierMatching matchedTiers = getEntityNameOverrideLevel(entityNameOverrides_Level,
            lmEntity);
        if (matchedTiers != null) {
            namesInfo = matchedTiers.names;
        } else if (entityNameOverrides != null) {
            if (entityNameOverrides.containsKey("all_entities")) {
                namesInfo = entityNameOverrides.get("all_entities").names;
            } else if (entityNameOverrides.containsKey(lmEntity.getNameIfBaby())) {
                namesInfo = entityNameOverrides.get(lmEntity.getNameIfBaby()).names;
            }
        }

        if (namesInfo == null || namesInfo.isEmpty()) {
            return null;
        } else if (namesInfo.size() > 1) {
            Collections.shuffle(namesInfo);
        }

        final boolean useCustomNameForNametags = main.helperSettings.getBoolean(
                main.settingsCfg, "use-customname-for-mob-nametags");
        final String entityName = Utils.capitalize(lmEntity.getNameIfBaby().replaceAll("_", " "));
        String result = namesInfo.get(0);
        result = result.replace("%entity-name%", entityName);
        result = result.replace("%displayname%",
            (lmEntity.getLivingEntity().getCustomName() == null || forceCustomName || useCustomNameForNametags ?
                entityName : lmEntity.getLivingEntity().getCustomName()));

        if (namesInfo.size() > 1) {
            // set a PDC key with the name otherwise the name will constantly change
            lmEntity.setOverridenEntityName(result);
        }

        return result;
    }

    private @Nullable LevelTierMatching getEntityNameOverrideLevel(
        final Map<String, List<LevelTierMatching>> entityNameOverrides_Level,
        final LivingEntityWrapper lmEntity
    ) {
        if (entityNameOverrides_Level == null) {
            return null;
        }

        LevelTierMatching allEntities = null;
        LevelTierMatching thisMob = null;

        for (final List<LevelTierMatching> tiers : entityNameOverrides_Level.values()) {
            for (final LevelTierMatching tier : tiers) {
                if (tier.isApplicableToMobLevel(lmEntity.getMobLevel())) {
                    if ("all_entities".equalsIgnoreCase(tier.mobName)
                        && tier.isApplicableToMobLevel(lmEntity.getMobLevel())) {
                        allEntities = tier;
                    } else if (lmEntity.getNameIfBaby().equalsIgnoreCase(tier.mobName)
                        && tier.isApplicableToMobLevel(lmEntity.getMobLevel())) {
                        thisMob = tier;
                    }
                }
            }
        }

        if (thisMob != null) {
            return thisMob;
        } else {
            return allEntities;
        }
    }

    public @Nullable Particle getSpawnerParticle(final @NotNull LivingEntityWrapper lmEntity) {
        Particle result = Particle.SOUL;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.spawnerParticle != null) {
                result = ruleInfo.spawnerParticle;
            } else if (ruleInfo.useNoSpawnerParticles) {
                result = null;
            }
        }

        return result;
    }

    public int getSpawnerParticleCount(final @NotNull LivingEntityWrapper lmEntity) {
        int result = 10;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.spawnerParticlesCount != null) {
                result = ruleInfo.spawnerParticlesCount;
            }
        }

        // max limit of 100 counts which would take 5 seconds to show
        if (result > 100) {
            result = 100;
        }

        return result;
    }

    public @NotNull CachedModalList<VanillaBonusEnum> getAllowedVanillaBonuses(final @NotNull LivingEntityWrapper lmEntity){
        CachedModalList<VanillaBonusEnum> result = null;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.vanillaBonuses != null){
                result = ruleInfo.vanillaBonuses;
            }
        }

        return result != null ? result : new CachedModalList<>();
    }

    public int getMaximumDeathInChunkThreshold(final @NotNull LivingEntityWrapper lmEntity) {
        int result = 0;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.maximumDeathInChunkThreshold != null) {
                result = ruleInfo.maximumDeathInChunkThreshold;
            }
        }

        return result;
    }

    public int getMaxChunkCooldownTime(final @NotNull LivingEntityWrapper lmEntity) {
        int result = 0;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.chunkMaxCoolDownTime != null) {
                result = ruleInfo.chunkMaxCoolDownTime;
            }
        }

        return result;
    }

    public int getAdjacentChunksToCheck(final @NotNull LivingEntityWrapper lmEntity) {
        int result = 0;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.maxAdjacentChunks != null) {
                result = ruleInfo.maxAdjacentChunks;
            }
        }

        return result;
    }

    public @Nullable String getDeathMessage(final @NotNull LivingEntityWrapper lmEntity){
        DeathMessages deathMessages = null;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.deathMessages != null) {
                deathMessages = ruleInfo.deathMessages;
            }
        }

        return deathMessages == null ?
                null :
                deathMessages.getDeathMessage();
    }

    public @NotNull ApplicableRulesResult getApplicableRules(final LivingEntityInterface lmInterface) {
        final ApplicableRulesResult applicableRules = new ApplicableRulesResult();

        if (this.lastRulesCheck == null
            || Duration.between(this.lastRulesCheck, Instant.now()).toMillis() > 100) {
            // check temp disabled rules every 100ms minimum
            checkTempDisabledRules();
            this.lastRulesCheck = Instant.now();
        }

        for (final List<RuleInfo> rules : rulesInEffect.values()) {
            for (final RuleInfo ruleInfo : rules) {
                if (!ruleInfo.ruleIsEnabled || ruleInfo.isTempDisabled) {
                    continue;
                }

                if (lmInterface instanceof LivingEntityWrapper && !isRuleApplicableEntity(
                    (LivingEntityWrapper) lmInterface, ruleInfo)) {
                    continue;
                }

                final RuleCheckResult checkResult = isRuleApplicableInterface(lmInterface,
                    ruleInfo);
                if (!checkResult.useResult) {
                    if (checkResult.ruleMadeChance != null && !checkResult.ruleMadeChance) {
                        applicableRules.allApplicableRules_DidNotMakeChance.add(ruleInfo);
                    }
                    continue;
                } else if (checkResult.ruleMadeChance != null && checkResult.ruleMadeChance) {
                    applicableRules.allApplicableRules_MadeChance.add(ruleInfo);
                }

                applicableRules.allApplicableRules.add(ruleInfo);
                checkIfRuleShouldBeTempDisabled(ruleInfo, lmInterface);

                if (ruleInfo.stopProcessingRules != null) {
                    final boolean result = ruleInfo.stopProcessingRules;
                    DebugManager.log(DebugType.SETTING_STOP_PROCESSING,
                            ruleInfo, lmInterface, result,
                            () -> String.format("&b%s&7, mob: &b%s&7, rule count: &b%s",
                                    ruleInfo.getRuleName(), lmInterface.getTypeName(),
                                    applicableRules.allApplicableRules.size()));
                    if (!result) break;
                }
            }
        }

        boolean hasWorldListSpecified = false;
        for (final RuleInfo ri : applicableRules.allApplicableRules) {
            if (ri.conditions_Worlds != null && (!ri.conditions_Worlds.isEmpty()
                || ri.conditions_Worlds.allowAll)) {
                hasWorldListSpecified = true;
                break;
            }
        }

        return hasWorldListSpecified ?
            applicableRules : new ApplicableRulesResult();
    }

    private void checkIfRuleShouldBeTempDisabled(final @NotNull RuleInfo ruleInfo,
        final @NotNull LivingEntityInterface lmInterface) {
        if (!(lmInterface instanceof final LivingEntityWrapper lmEntity)) {
            return;
        }

        // don't increment the count when just checking nametags, etc
        if (!lmEntity.isNewlySpawned && !lmEntity.isRulesForceAll) {
            return;
        }

        synchronized (ruleLocker) {
            if (!this.rulesCooldown.containsKey(ruleInfo.getRuleName())) {
                this.rulesCooldown.put(ruleInfo.getRuleName(), new LinkedList<>());
            }
            final List<Instant> instants = this.rulesCooldown.get(ruleInfo.getRuleName());
            instants.add(Instant.now());

            if (ruleInfo.conditions_TimesToCooldownActivation == null
                || instants.size() >= ruleInfo.conditions_TimesToCooldownActivation) {
                if (ruleInfo.conditions_CooldownTime == null
                    || ruleInfo.conditions_CooldownTime <= 0) {
                    return;
                }
                DebugManager.log(DebugType.SETTING_COOLDOWN, () ->
                        ruleInfo.getRuleName() + ": cooldown reached, disabling rule");
                ruleInfo.isTempDisabled = true;
            }
        }
    }

    @SuppressWarnings("deprecation")
    private boolean isRuleApplicableEntity(final LivingEntityWrapper lmEntity,
                                           final @NotNull RuleInfo ri) {
        if (ri.conditions_MinLevel != null) {
            final boolean result = (lmEntity.isLevelled() &&
                    lmEntity.getMobLevel() >= ri.conditions_MinLevel);

            DebugManager.log(DebugType.CONDITION_MAXLEVEL, ri, lmEntity, result,
                    () -> String.format("&b%s&7, mob: &b%s&7, mob lvl: &b%s&7, rule minlvl: &b%s&7",
                    ri.getRuleName(), lmEntity.getTypeName(), lmEntity.getMobLevel(),
                    ri.conditions_MinLevel));
            if (!result) return false;
        }

        if (ri.conditions_MaxLevel != null) {
            final boolean result = (lmEntity.isLevelled() &&
                    lmEntity.getMobLevel() <= ri.conditions_MaxLevel);
            DebugManager.log(DebugType.CONDITION_MAXLEVEL, ri, lmEntity, result,
                    () -> String.format("&b%s&7, mob: &b%s&7, mob lvl: &b%s&7, rule maxlvl: &b%s&7",
                    ri.getRuleName(), lmEntity.getTypeName(), lmEntity.getMobLevel(),
                    ri.conditions_MaxLevel));
            if (!result) return false;
        }

        if (ri.conditions_WithinCoords != null && !ri.conditions_WithinCoords.isEmpty() &&
            !meetsMaxDistanceCriteria(lmEntity, ri)){
            // debug entries are inside the last function
            return false;
        }

        if (ri.conditions_CustomNames != null) {
            final String customName = lmEntity.getLivingEntity().getCustomName() != null ?
                lmEntity.getLivingEntity().getCustomName() : "(none)";

            final boolean result = ri.conditions_CustomNames.isEnabledInList(customName, lmEntity);

            DebugManager.log(DebugType.CONDITION_CUSTOM_NAME, ri, lmEntity, result,
                    () -> String.format("&b%s&7, mob: &b%s&7, name: &b%s&7",
                    ri.getRuleName(), lmEntity.getTypeName(), customName));

            if (!result) return false;
        }

        if (ri.conditions_SpawnReasons != null) {
            final boolean result = ri.conditions_SpawnReasons.isEnabledInList(
                    lmEntity.getSpawnReason(), lmEntity);
            DebugManager.log(DebugType.CONDITION_SPAWN_REASON, ri, lmEntity, result,
                    () -> String.format("&b%s&7, mob: &b%s&7, spawn reason: &b%s&7",
                    ri.getRuleName(), lmEntity.getTypeName(), lmEntity.getSpawnReason()));
            if (!result) return false;
        }

        if (ri.conditions_ApplyPlugins != null){
            ExternalCompatibilityManager.updateAllExternalCompats(lmEntity);
            final List<ExternalCompatibilityManager.ExternalCompatibility> mobCompats = lmEntity.getMobExternalTypes();
            if (!lmEntity.isMobOfExternalType()) mobCompats.add(ExternalCompatibilityManager.ExternalCompatibility.NOT_APPLICABLE);

            boolean madeIt = false;
            for (ExternalCompatibilityManager.ExternalCompatibility compat : mobCompats){
                if (ri.conditions_ApplyPlugins.isEnabledInList(compat.name(), lmEntity)){
                    madeIt = true;
                    break;
                }
            }

            DebugManager.log(DebugType.CONDITION_PLUGIN_COMPAT, ri, lmEntity, madeIt,
                    () -> String.format("&b%s&7, mob: &b%s&7, mob plugins: &b%s&7",
                            ri.getRuleName(), lmEntity.getNameIfBaby(), mobCompats));
                if (!madeIt) return false;

        }

        if (ri.conditions_MM_Names != null) {
            String mmName = ExternalCompatibilityManager.getMythicMobInternalName(lmEntity);
            if (mmName.isEmpty()) {
                mmName = "(none)";
            }

            final boolean result = ri.conditions_MM_Names.isEnabledInList(mmName, lmEntity);
            final String mmNameFinal = mmName;
            DebugManager.log(DebugType.CONDITION_MYTHICMOBS_INTERNAL_NAME, ri, lmEntity, result,
                    () -> String.format("&b%s&7, mob: &b%s&7, mm_name: &b%s&7",
                    ri.getRuleName(), lmEntity.getNameIfBaby(), mmNameFinal));

            if (!result) return false;
        }

        if (ri.conditions_SpawnerNames != null) {
            final String checkName = lmEntity.getSourceSpawnerName() != null ?
                    lmEntity.getSourceSpawnerName() : "(none)";

            final boolean result = ri.conditions_SpawnerNames.isEnabledInList(checkName, lmEntity);
            DebugManager.log(DebugType.CONDITION_SPAWNER_NAME, ri, lmEntity, result,
                    () -> String.format("&b%s&7, mob: &b%s&7, spawner: &b%s&7",
                    ri.getRuleName(), lmEntity.getNameIfBaby(), checkName));

            if (!result) return false;
        }

        if (ri.conditions_SpawnegEggNames != null) {
            final String checkName = lmEntity.getSourceSpawnEggName() != null ?
                    lmEntity.getSourceSpawnEggName() : "(none)";

            final boolean result = ri.conditions_SpawnegEggNames.isEnabledInList(checkName, lmEntity);
            DebugManager.log(DebugType.CONDITION_SPAWNER_NAME, ri, lmEntity, result,
                    () -> String.format("&b%s&7, mob: &b%s&7, spawn_egg: &b%s&7",
                    ri.getRuleName(), lmEntity.getNameIfBaby(), checkName));

            if (!result) return false;
        }

        if (ri.conditions_Permission != null) {

            if (lmEntity.associatedPlayer == null) {
                DebugManager.log(DebugType.CONDITION_PERMISSION, ri, lmEntity, false,
                        () -> String.format("&b%s&7, mob: &b%s&7, no player was provided",
                        ri.getRuleName(), lmEntity.getNameIfBaby()));
                return false;
            }

            if (!doesPlayerPassPermissionChecks(ri.conditions_Permission,
                lmEntity.associatedPlayer)) {
                DebugManager.log(DebugType.CONDITION_PERMISSION, ri, lmEntity, false,
                        () -> String.format("&b%s&7, mob: &b%s&7, player: &b%s&7, permission denied",
                        ri.getRuleName(), lmEntity.getNameIfBaby(),
                        lmEntity.associatedPlayer.getName()));
                return false;
            }

            DebugManager.log(DebugType.CONDITION_PERMISSION, ri, lmEntity, true,
                    () -> String.format("&b%s&7, mob: &b%s&7, player: &b%s&7, permission granted",
                        ri.getRuleName(), lmEntity.getNameIfBaby(),
                        lmEntity.associatedPlayer.getName()));
        }

        if (ri.conditions_MobCustomnameStatus != MobCustomNameStatus.NOT_SPECIFIED
                && ri.conditions_MobCustomnameStatus != MobCustomNameStatus.EITHER) {
            final boolean hasCustomName = lmEntity.getLivingEntity().getCustomName() != null;

            if (hasCustomName && ri.conditions_MobCustomnameStatus == MobCustomNameStatus.NOT_NAMETAGGED ||
                    !hasCustomName && ri.conditions_MobCustomnameStatus == MobCustomNameStatus.NAMETAGGED) {
                DebugManager.log(DebugType.CONDITION_CUSTOM_NAME, ri, lmEntity, false,
                        () -> String.format("&b%s&7, mob: &b%s&7, nametag: %s, rule: %s",
                                ri.getRuleName(), lmEntity.getNameIfBaby(), lmEntity.getLivingEntity().getCustomName(),
                                ri.conditions_MobCustomnameStatus));
                return false;
            }

            DebugManager.log(DebugType.CONDITION_CUSTOM_NAME, ri, lmEntity, true,
                    () -> String.format("&b%s&7, mob: &b%s&7, nametag: %s, rule: %s",
                            ri.getRuleName(), lmEntity.getNameIfBaby(), lmEntity.getLivingEntity().getCustomName(),
                            ri.conditions_MobCustomnameStatus));
        }

        if (ri.conditions_MobTamedStatus != MobTamedStatus.NOT_SPECIFIED
            && ri.conditions_MobTamedStatus != MobTamedStatus.EITHER) {
            if (lmEntity.isMobTamed() && ri.conditions_MobTamedStatus == MobTamedStatus.NOT_TAMED ||
                !lmEntity.isMobTamed() && ri.conditions_MobTamedStatus == MobTamedStatus.TAMED) {
                DebugManager.log(DebugType.ENTITY_TAME, ri, lmEntity, false,
                        () -> String.format("&b%s&7, mob: &b%s&7, tamed: %s, rule: %s",
                        ri.getRuleName(), lmEntity.getNameIfBaby(), lmEntity.isMobTamed(),
                        ri.conditions_MobTamedStatus));
                return false;
            }

            DebugManager.log(DebugType.ENTITY_TAME, ri, lmEntity, true,
                    () -> String.format("&b%s&7, mob: &b%s&7, tamed: %s, rule: %s",
                        ri.getRuleName(), lmEntity.getNameIfBaby(), lmEntity.isMobTamed(),
                        ri.conditions_MobTamedStatus));
        }

        if (ri.conditions_ScoreboardTags != null) {
            final Set<String> tags = lmEntity.getLivingEntity().getScoreboardTags();
            if (tags.isEmpty()) {
                tags.add("(none)");
            }

            boolean madeCriteria = false;
            for (final String tag : tags) {
                if (ri.conditions_ScoreboardTags.isEnabledInList(tag, lmEntity)) {
                    madeCriteria = true;
                }
            }

            DebugManager.log(DebugType.SCOREBOARD_TAGS, ri, lmEntity, madeCriteria,
                    () -> String.format("&b%s&7, mob: &b%s&7",
                ri.getRuleName(), lmEntity.getNameIfBaby()));

            if (!madeCriteria) return false;
        }

        if (ri.conditions_SkyLightLevel != null) {
            final int lightLevel = lmEntity.getSkylightLevel();
            final boolean result = (lightLevel >= ri.conditions_SkyLightLevel.min
                    && lightLevel <= ri.conditions_SkyLightLevel.max);
            DebugManager.log(DebugType.SKYLIGHT_LEVEL, ri, lmEntity, result,
                    () -> String.format("&b%s&7, mob: &b%s&7, skylight: %s, criteria: %s",
                ri.getRuleName(), lmEntity.getNameIfBaby(), lightLevel,
                ri.conditions_SkyLightLevel));
            return result;
        }

        return true;
    }

    private boolean meetsMaxDistanceCriteria(final @NotNull LivingEntityWrapper lmEntity, final @NotNull RuleInfo rule){
        final WithinCoordinates mdr = rule.conditions_WithinCoords;

        if (mdr.getHasX() && !mdr.isLocationWithinRange(lmEntity.getLocation().getBlockX(), WithinCoordinates.Axis.X)){
            DebugManager.log(DebugType.CONDITION_WITH_COORDINATES, rule, lmEntity, false, () -> String.format(
                    "entity: %s, xCoord: %s, startX: %s, endX: %s",
                    lmEntity.getNameIfBaby(), lmEntity.getLocation().getBlockX(), mdr.startX, mdr.endX));
            return false;
        }

        if (mdr.getHasY() && !mdr.isLocationWithinRange(lmEntity.getLocation().getBlockY(), WithinCoordinates.Axis.Y)){
            DebugManager.log(DebugType.CONDITION_WITH_COORDINATES, rule, lmEntity, false, () -> String.format(
                    "entity: %s, yCoord: %s, startY: %s, endY: %s",
                    lmEntity.getNameIfBaby(), lmEntity.getLocation().getBlockY(), mdr.startY, mdr.endY));
            return false;
        }

        if (mdr.getHasZ() && !mdr.isLocationWithinRange(lmEntity.getLocation().getBlockZ(), WithinCoordinates.Axis.Z)){
            DebugManager.log(DebugType.CONDITION_WITH_COORDINATES, rule, lmEntity, false, () -> String.format(
                    "entity: %s, zCoord: %s, startZ: %s, endZ: %s",
                    lmEntity.getNameIfBaby(), lmEntity.getLocation().getBlockZ(), mdr.startZ, mdr.endZ));
            return false;
        }

        DebugManager.log(DebugType.CONDITION_WITH_COORDINATES, rule, lmEntity, true, () -> String.format(
                "entity: %s, zCoord: %s, startZ: %s, endZ: %s",
                lmEntity.getNameIfBaby(), lmEntity.getLocation().getBlockZ(), mdr.startZ, mdr.endZ));

        return true;
    }

    @Contract("_, _ -> new")
    private @NotNull RuleCheckResult isRuleApplicableInterface(
        final LivingEntityInterface lmInterface, final @NotNull RuleInfo ri) {

        if (ri.conditions_Entities != null){
            if (lmInterface instanceof final LivingEntityWrapper lmEntity) {
                final boolean result = Utils.isLivingEntityInModalList(
                        ri.conditions_Entities, lmEntity, true);
                DebugManager.log(DebugType.CONDITION_ENTITIES_LIST, ri, lmInterface, result,
                        () -> String.format("&b%s&7, mob: &b%s&7", ri.getRuleName(),
                                lmEntity.getNameIfBaby()));

                if (!result) return new RuleCheckResult(false);
            } else {
                // can't check groups if not a living entity wrapper
                final boolean result = ri.conditions_Entities.isEnabledInList(
                        lmInterface.getTypeName(), null);

                DebugManager.log(DebugType.CONDITION_ENTITIES_LIST, ri, lmInterface, result,
                        () -> String.format("&b%s&7, mob: &b%s&7", ri.getRuleName(),
                                lmInterface.getTypeName()));

                if (!result) return new RuleCheckResult(false);
            }
        }

        if (ri.conditions_Worlds != null){
            final boolean result = (lmInterface.isWasSummoned() ||
                ri.conditions_Worlds.isEnabledInList(lmInterface.getWorld().getName(), null));
            DebugManager.log(DebugType.CONDITION_WORLD_LIST, ri, lmInterface, result,
                    () -> String.format("&b%s&7, mob: &b%s&7, mob world: &b%s&7",
                            ri.getRuleName(), lmInterface.getTypeName(), lmInterface.getWorld().getName()));
            if (!result) return new RuleCheckResult(false);
        }

        if (ri.conditions_Biomes != null) {
            final boolean result = Utils.isBiomeInModalList(ri.conditions_Biomes,
                    lmInterface.getLocation().getBlock().getBiome(), main.rulesManager);
            DebugManager.log(DebugType.CONDITION_BIOME_LIST, ri, lmInterface, result,
                    () -> String.format("&b%s&7, mob: &b%s&7, mob biome: &b%s&7",
                    ri.getRuleName(), lmInterface.getTypeName(),
                    lmInterface.getLocation().getBlock().getBiome().name()));
            if (!result) return new RuleCheckResult(false);
        }

        if (ri.conditions_WGRegions != null
            && ExternalCompatibilityManager.hasWorldGuardInstalled()) {
            boolean isInList = false;
            final List<String> wgRegions = ExternalCompatibilityManager.getWGRegionsAtLocation(
                lmInterface);
            if (wgRegions.isEmpty()) {
                wgRegions.add("(none)");
            }

            for (final String regionName : wgRegions) {
                if (ri.conditions_WGRegions.isEnabledInList(regionName, null)) {
                    isInList = true;
                    break;
                }
            }

            DebugManager.log(DebugType.CONDITION_WG_REGION, ri, lmInterface, isInList,
                    () -> String.format("&b%s&7, mob: &b%s&7, wg_regions: &b%s&7",
                    ri.getRuleName(), lmInterface.getTypeName(), wgRegions));
            if (!isInList) return new RuleCheckResult(false);
        }

        if (ri.conditions_WGRegionOwners != null
            && ExternalCompatibilityManager.hasWorldGuardInstalled()) {
            boolean isInList = false;
            final List<String> wgRegionOwners = WorldGuardIntegration.getWorldGuardRegionOwnersForLocation(
                lmInterface);
            if (wgRegionOwners.isEmpty()) {
                wgRegionOwners.add("(none)");
            }

            for (final String ownerName : wgRegionOwners) {
                if (ri.conditions_WGRegionOwners.isEnabledInList(ownerName, null)) {
                    isInList = true;
                    break;
                }
            }

            DebugManager.log(DebugType.CONDITION_WG_REGION_OWNER, ri, lmInterface, isInList,
                    () -> String.format("&b%s&7, mob: &b%s&7, wg_owners: &b%s&7",
                    ri.getRuleName(), lmInterface.getTypeName(), wgRegionOwners));

            if (!isInList) return new RuleCheckResult(false);
        }

        if (ri.conditions_ApplyAboveY != null) {
            final boolean result = lmInterface.getLocation().getBlockY() > ri.conditions_ApplyAboveY;
            DebugManager.log(DebugType.CONDITION_Y_LEVEL, ri, lmInterface, result,
                    () -> String.format("&b%s&7, mob: &b%s&7, y-level: &b%s&7, max-y: &b%s&7",
                    ri.getRuleName(), lmInterface.getTypeName(),
                    lmInterface.getLocation().getBlockY(), ri.conditions_ApplyAboveY));
            if (!result) return new RuleCheckResult(false);
        }

        if (ri.conditions_ApplyBelowY != null) {
            final boolean result = lmInterface.getLocation().getBlockY() < ri.conditions_ApplyBelowY;
            DebugManager.log(DebugType.CONDITION_Y_LEVEL, ri, lmInterface, result,
                    () -> String.format("&b%s&7, mob: &b%s&7, y-level: &b%s&7, min-y: &b%s&7",
                    ri.getRuleName(), lmInterface.getTypeName(),
                    lmInterface.getLocation().getBlockY(), ri.conditions_ApplyBelowY));
            if (!result) return new RuleCheckResult(false);
        }

        if (ri.conditions_MinDistanceFromSpawn != null) {
            final boolean result = lmInterface.getDistanceFromSpawn() >= ri.conditions_MinDistanceFromSpawn;
            DebugManager.log(DebugType.CONDITION_MIN_SPAWN_DISTANCE, ri, lmInterface, result,
                    () -> String.format("&b%s&7, mob: &b%s&7, spawn-distance: &b%s&7, min-sd: &b%s&7",
                    ri.getRuleName(), lmInterface.getTypeName(),
                    Utils.round(lmInterface.getDistanceFromSpawn()),
                    ri.conditions_MinDistanceFromSpawn));

            if (!result) return new RuleCheckResult(false);
        }

        if (ri.conditions_MaxDistanceFromSpawn != null) {
            final boolean result = lmInterface.getDistanceFromSpawn() <= ri.conditions_MaxDistanceFromSpawn;
            DebugManager.log(DebugType.CONDITION_MAX_SPAWN_DISTANCE, ri, lmInterface, result,
                    () -> String.format("&b%s&7, mob: &b%s&7, spawn-distance: &b%s&7, min-sd: &b%s&7",
                    ri.getRuleName(), lmInterface.getTypeName(),
                    Utils.round(lmInterface.getDistanceFromSpawn()),
                    ri.conditions_MaxDistanceFromSpawn));

            if (!result) return new RuleCheckResult(false);
        }

        if (ri.conditions_WorldTickTime != null) {
            final int currentWorldTickTime = lmInterface.getSpawnedTimeOfDay();
            final boolean result = Utils.isIntegerInModalList(ri.conditions_WorldTickTime, currentWorldTickTime);
            DebugManager.log(DebugType.CONDITION_WORLD_TIME_TICK, ri, lmInterface, result,
                    () -> String.format("&b%s&7, mob: &b%s&7, tick time: &b%s&7",
                    ri.getRuleName(), lmInterface.getTypeName(), currentWorldTickTime));

            if (!result) return new RuleCheckResult(false, false);
        }

        Boolean ruleMadeChance = null;

        if (ri.conditions_Chance != null && ri.conditions_Chance < 1.0) {
            if (lmInterface instanceof final LivingEntityWrapper lmEntity) {
                // find out if this entity previously lost or won the chance previously and use that result if present
                final Map<String, Boolean> prevChanceResults = lmEntity.getPrevChanceRuleResults();
                if (prevChanceResults != null && prevChanceResults.containsKey(ri.getRuleName())) {
                    final boolean prevResult = prevChanceResults.get(ri.getRuleName());
                    return new RuleCheckResult(prevResult);
                }
            }

            final float chanceRole =
                (float) ThreadLocalRandom.current().nextInt(0, 100001) * 0.00001F;
            final boolean result = chanceRole >= (1.0F - ri.conditions_Chance);
            DebugManager.log(DebugType.CONDITION_CHANCE, ri, lmInterface, result,
                    () -> String.format("&b%s&7, mob: &b%s&7, chance: &b%s&7, chance role: &b%s&7",
                    ri.getRuleName(), lmInterface.getTypeName(), ri.conditions_Chance,
                    Utils.round(chanceRole, 4)));

            if (!result) return new RuleCheckResult(false, false);

            ruleMadeChance = true;
        }

        return new RuleCheckResult(true, ruleMadeChance);
    }

    private boolean doesPlayerPassPermissionChecks(final @NotNull CachedModalList<String> perms,
        final @NotNull Player player) {
        if (perms.allowAll) {
            return true;
        }
        if (perms.excludeAll) {
            return false;
        }
        if (perms.isEmpty()) {
            return true;
        }

        for (final String perm : perms.excludedList) {
            final String permCheck = "levelledmobs.permission." + perm;
            if (player.hasPermission(permCheck)) {
                return false;
            }
        }

        for (final String perm : perms.allowedList) {
            final String permCheck = "levelledmobs.permission." + perm;
            if (player.hasPermission(permCheck)) {
                return true;
            }
        }

        return perms.isBlacklist();
    }

    public void buildBiomeGroupMappings(final Map<String, Set<String>> customBiomeGroups) {
        this.biomeGroupMappings.clear();

        if (customBiomeGroups == null) {
            return;
        }

        for (final Map.Entry<String, Set<String>> groupName : customBiomeGroups.entrySet()) {
            final Set<String> groupMembers = groupName.getValue();
            final List<String> newList = new ArrayList<>(groupMembers.size());
            newList.addAll(groupMembers);
            this.biomeGroupMappings.put(groupName.getKey(), newList);
        }
    }

    public void clearTempDisabledRulesCounts() {
        synchronized (ruleLocker) {
            this.rulesCooldown.clear();
        }
    }

    void checkTempDisabledRules() {
        synchronized (ruleLocker) {
            if (this.rulesCooldown.isEmpty()) {
                return;
            }

            final Iterator<String> iterator = this.rulesCooldown.keySet().iterator();
            while (iterator.hasNext()) {
                final String ruleName = iterator.next();
                final RuleInfo rule = this.ruleNameMappings.get(ruleName);
                if (rule == null || rule.conditions_CooldownTime == null
                    || rule.conditions_CooldownTime <= 0) {
                    if (rule != null) {
                        rule.isTempDisabled = false;
                    }
                    iterator.remove();
                    continue;
                }

                final List<Instant> instants = this.rulesCooldown.get(ruleName);
                final int preCount = instants.size();
                if (instants.removeIf(k -> Duration.between(k, Instant.now()).toMillis()
                    > rule.conditions_CooldownTime)) {
                    DebugManager.log(DebugType.SETTING_COOLDOWN, () ->
                            String.format("rule: %s, removed cooldown entries, pre: %s, post: %s",
                            rule.getRuleName(), preCount, instants.size()));
                    if (instants.isEmpty()) {
                        rule.isTempDisabled = false;
                        iterator.remove();
                    }
                }
            }
        }
    }

    public @NotNull String showTempDisabledRules(final boolean isFromConsole) {
        synchronized (ruleLocker) {
            if (this.rulesCooldown.isEmpty()) {
                final String message = "No rules are currently temporarily disabled";
                if (isFromConsole) {
                    return String.format("%s %s", main.configUtils.getPrefix(), message);
                } else {
                    return message;
                }
            }

            checkTempDisabledRules();

            final StringBuilder sb = new StringBuilder();
            if (isFromConsole) {
                sb.append(main.configUtils.getPrefix());
                sb.append(
                    String.format(" %s rule(s) currently disabled:", this.rulesCooldown.size()));
            }

            for (final String ruleName : this.rulesCooldown.keySet()) {
                final RuleInfo rule = this.ruleNameMappings.get(ruleName);
                if (rule == null || rule.conditions_CooldownTime == null) {
                    continue;
                }
                sb.append(System.lineSeparator());

                sb.append(ruleName);
                sb.append(": seconds left: ");
                final Instant instant = this.rulesCooldown.get(ruleName).get(0);
                final long millisecondsSince = Duration.between(instant, Instant.now()).toMillis();
                final Duration duration = Duration.ofMillis(
                    rule.conditions_CooldownTime - millisecondsSince);
                sb.append(duration.toSeconds());
            }

            return sb.toString();
        }
    }

    void updateRulesHash(){
        final StringBuilder sb = new StringBuilder();

        synchronized (ruleLocker) {
            for (final int rulePri : this.rulesInEffect.keySet()) {
                final List<RuleInfo> rules = this.rulesInEffect.get(rulePri);
                for (final RuleInfo rule : rules){
                    if (rule == null || !rule.ruleIsEnabled) {
                        continue;
                    }
                    if (!sb.isEmpty()) sb.append("\n");
                    sb.append(rule.formatRulesVisually(true, List.of("id")));
                }
            }
        }

        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA3-256");
            final byte[] hashbytes = digest.digest(
                    sb.toString().getBytes(StandardCharsets.UTF_8));
            this.currentRulesHash = bytesToHex(hashbytes);
        } catch (NoSuchAlgorithmException e) {
            Utils.logger.error("Unable to run SHA-256 hash: " + e.getMessage());
            this.currentRulesHash = "1234";
        }
    }

    // taken from https://www.baeldung.com/sha-256-hashing-java
    private static @NotNull String bytesToHex(final byte @NotNull [] hash) {
        final StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            final String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}