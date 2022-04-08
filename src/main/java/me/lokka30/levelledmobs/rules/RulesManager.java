/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.rules;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.LivingEntityInterface;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.misc.CachedModalList;
import me.lokka30.levelledmobs.misc.DebugType;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.levelledmobs.rules.strategies.LevellingStrategy;
import me.lokka30.levelledmobs.rules.strategies.RandomLevellingStrategy;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages all rules that are parsed from rules.yml and applied to various
 * defined mobs
 *
 * @author stumper66
 * @since 3.0.0
 */
public class RulesManager {
    public RulesManager(final LevelledMobs main) {
        this.main = main;
        this.rulesInEffect = new TreeMap<>();
        this.biomeGroupMappings = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    }

    private final LevelledMobs main;
    @NotNull
    public final SortedMap<Integer, List<RuleInfo>> rulesInEffect;
    @NotNull
    public final Map<String, List<String>> biomeGroupMappings;
    public boolean anyRuleHasChance;

    public boolean getRule_IsWorldAllowedInAnyRule(final World world){
        if (world == null) return false;
        boolean result = false;

        for (final RuleInfo ruleInfo : main.rulesParsingManager.getAllRules()){
            if (!ruleInfo.ruleIsEnabled) continue;
            if (ruleInfo.conditions_Worlds != null && ruleInfo.conditions_Worlds.isEnabledInList(world.getName(), null)){
                result = true;
                break;
            }
        }

        return result;
    }

    @NotNull
    public List<String> getRule_NBT_Data(final @NotNull LivingEntityWrapper lmEntity){
        final List<String> nbtData = new LinkedList<>();

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.mobNBT_Data != null) {
                final MergeableStringList nbt = ruleInfo.mobNBT_Data;
                if (!nbt.doMerge)
                    nbtData.clear();

                nbtData.addAll(nbt.items);
            }
        }

        return nbtData;
    }

    public double getRule_SunlightBurnIntensity(final @NotNull LivingEntityWrapper lmEntity){
        double result = 0.0;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.sunlightBurnAmount != null)
                result = ruleInfo.sunlightBurnAmount;
        }

        return result;
    }

    public int getRule_MaxRandomVariance(@NotNull final LivingEntityWrapper lmEntity){
        int result = 0;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.maxRandomVariance != null) result = ruleInfo.maxRandomVariance;
        }

        return result;
    }

    public boolean getRule_CheckIfNoDropMultiplierEntitiy(@NotNull final LivingEntityWrapper lmEntity){
        CachedModalList<String> entitiesList = null;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.conditions_NoDropEntities != null)
                entitiesList = ruleInfo.conditions_NoDropEntities;
        }

        return entitiesList != null && entitiesList.isEnabledInList(lmEntity.getNameIfBaby(), lmEntity);
    }

    @NotNull
    public CustomDropsRuleSet getRule_UseCustomDropsForMob(@NotNull final LivingEntityWrapper lmEntity){
        final CustomDropsRuleSet dropRules = new CustomDropsRuleSet();
        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.customDrops_UseForMobs != null) dropRules.useDrops = ruleInfo.customDrops_UseForMobs;
            if (ruleInfo.customDrops_UseOverride != null) dropRules.override = ruleInfo.customDrops_UseOverride;
            if (ruleInfo.customDrop_DropTableId != null) dropRules.useDropTableId = ruleInfo.customDrop_DropTableId;
        }

        return dropRules;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean getRule_IsMobAllowedInEntityOverride(@NotNull final LivingEntityInterface lmInterface){
        // check if it should be denied thru the entity override list
        boolean babyMobsInheritAdultSetting = true; // default
        CachedModalList<String> allowedEntitiesList = null;
        for (final RuleInfo ruleInfo : lmInterface.getApplicableRules()){
            if (ruleInfo.allowedEntities != null)
                allowedEntitiesList = ruleInfo.allowedEntities;
            if (ruleInfo.babyMobsInheritAdultSetting != null)
                babyMobsInheritAdultSetting = ruleInfo.babyMobsInheritAdultSetting;
        }

        if (lmInterface instanceof LivingEntityWrapper) {
            final LivingEntityWrapper lmEntity = (LivingEntityWrapper) lmInterface;
            return (
                    allowedEntitiesList == null ||
                            !babyMobsInheritAdultSetting && lmEntity.isBabyMob() && Utils.isLivingEntityInModalList(allowedEntitiesList, lmEntity, true)) ||
                    Utils.isLivingEntityInModalList(allowedEntitiesList, lmEntity, babyMobsInheritAdultSetting);
        } else {
            return (
                    allowedEntitiesList == null || allowedEntitiesList.isEnabledInList(lmInterface.getTypeName(), null)
            );
        }
    }

    @Nullable
    public FineTuningAttributes getFineTuningAttributes(@NotNull final LivingEntityWrapper lmEntity){
        FineTuningAttributes allMobAttribs = null;
        FineTuningAttributes thisMobAttribs = null;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.allMobMultipliers != null) {
                if (allMobAttribs == null || ruleInfo.allMobMultipliers.doNotMerge) {
                    allMobAttribs = ruleInfo.allMobMultipliers.cloneItem();
                    if (ruleInfo.allMobMultipliers.doNotMerge)
                        thisMobAttribs = null;
                }
                else
                    allMobAttribs.mergeAttributes(ruleInfo.allMobMultipliers);
            }

            if (ruleInfo.specificMobMultipliers != null && ruleInfo.specificMobMultipliers.containsKey(lmEntity.getNameIfBaby())){
                final FineTuningAttributes tempAttribs = ruleInfo.specificMobMultipliers.get(lmEntity.getNameIfBaby());
                if (thisMobAttribs == null || tempAttribs.doNotMerge) {
                    thisMobAttribs = tempAttribs.cloneItem();
                    if (tempAttribs.doNotMerge)
                        allMobAttribs = null;
                }
                else
                    thisMobAttribs.mergeAttributes(tempAttribs);
            }
        }

        if (allMobAttribs != null) {
            allMobAttribs.mergeAttributes(thisMobAttribs);
            return allMobAttribs;
        } else
            return thisMobAttribs;
    }

    @NotNull
    public Map<ExternalCompatibilityManager.ExternalCompatibility, Boolean> getRule_ExternalCompatibility(@NotNull final LivingEntityWrapper lmEntity){
        final Map<ExternalCompatibilityManager.ExternalCompatibility, Boolean> result = new EnumMap<>(ExternalCompatibilityManager.ExternalCompatibility.class);

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.enabledExtCompats != null) {
                result.putAll(ruleInfo.enabledExtCompats);
            }
        }

        return result;
    }

    public boolean isPlayerLevellingEnabled(){
        for (final List<RuleInfo> rules : this.rulesInEffect.values()){
            if (rules == null) continue;

            for (final RuleInfo ruleInfo : rules){
                if (ruleInfo.ruleIsEnabled && ruleInfo.playerLevellingOptions != null)
                    return true;
            }
        }

        return false;
    }

    public int getRule_CreeperMaxBlastRadius(@NotNull final LivingEntityWrapper lmEntity){
        int maxBlast = 5;
        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.creeperMaxDamageRadius != null) maxBlast = ruleInfo.creeperMaxDamageRadius;
        }

        return maxBlast;
    }

    @Nullable
    public LevellingStrategy getRule_LevellingStrategy(@NotNull final LivingEntityWrapper lmEntity){
        LevellingStrategy levellingStrategy = null;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.useRandomLevelling != null && ruleInfo.useRandomLevelling)
                levellingStrategy = new RandomLevellingStrategy();
            else if (ruleInfo.levellingStrategy != null) {
                if (levellingStrategy != null && levellingStrategy.getClass().equals(ruleInfo.levellingStrategy.getClass()))
                    levellingStrategy.mergeRule(ruleInfo.levellingStrategy);
                else
                    levellingStrategy = ruleInfo.levellingStrategy.cloneItem();
            }
        }

        return levellingStrategy;
    }

    public boolean getRule_MobLevelInheritance(@NotNull final LivingEntityWrapper lmEntity) {
        boolean result = true;
        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.mobLevelInheritance != null) result = ruleInfo.mobLevelInheritance;
        }

        return result;
    }

    public MobCustomNameStatus getRule_MobCustomNameStatus(@NotNull final LivingEntityWrapper lmEntity) {
        MobCustomNameStatus result = MobCustomNameStatus.NOT_SPECIFIED;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules())
            if (ruleInfo.conditions_MobCustomnameStatus != MobCustomNameStatus.NOT_SPECIFIED)
                result = ruleInfo.conditions_MobCustomnameStatus;

        return result;
    }

    public MobTamedStatus getRule_MobTamedStatus(@NotNull final LivingEntityWrapper lmEntity) {
        MobTamedStatus result = MobTamedStatus.NOT_SPECIFIED;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules())
            if (ruleInfo.conditions_MobTamedStatus != MobTamedStatus.NOT_SPECIFIED)
                result = ruleInfo.conditions_MobTamedStatus;

        return result;
    }

    public int getRule_MobMinLevel(@NotNull final LivingEntityInterface lmInterface) {
        if (lmInterface.getSummonedLevel() != null)
            return lmInterface.getSummonedLevel();

        int minLevel = 1;

        for (final RuleInfo ruleInfo : lmInterface.getApplicableRules()) {
            if (ruleInfo.restrictions_MinLevel != null) minLevel = ruleInfo.restrictions_MinLevel;
        }

        return minLevel;
    }

    public int getRule_MobMaxLevel(@NotNull final LivingEntityInterface lmInterface){
        int maxLevel = 0;
        int firstMaxLevel = -1;

        for (final RuleInfo ruleInfo : lmInterface.getApplicableRules()) {
            if (ruleInfo.restrictions_MaxLevel != null) {
                maxLevel = ruleInfo.restrictions_MaxLevel;
                if (firstMaxLevel < 0 && maxLevel > 0) firstMaxLevel = maxLevel;
            }
        }

        if (maxLevel <= 0 && lmInterface.getSummonedLevel() != null){
            if (maxLevel == 0 && firstMaxLevel > 0)
                maxLevel = firstMaxLevel;

            int summonedLevel = lmInterface.getSummonedLevel();
            if (summonedLevel > maxLevel) maxLevel = summonedLevel;
        }

        return maxLevel;
    }

    @Nullable
    public PlayerLevellingOptions getRule_PlayerLevellingOptions(@NotNull final LivingEntityWrapper lmEntity){
        PlayerLevellingOptions levellingOptions = null;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.playerLevellingOptions != null) levellingOptions = ruleInfo.playerLevellingOptions;
        }

        return levellingOptions;
    }

    @NotNull
    public String getRule_Nametag(@NotNull final LivingEntityWrapper lmEntity){
        String nametag = "";
        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (!Utils.isNullOrEmpty(ruleInfo.nametag)) {
                nametag = "disabled".equalsIgnoreCase(ruleInfo.nametag) ?
                        "" : ruleInfo.nametag;
            }
        }

        return nametag;
    }

    @Nullable
    public String getRule_Nametag_Placeholder(@NotNull final LivingEntityWrapper lmEntity){
        String nametag = null;
        final boolean isLevelled = lmEntity.isLevelled();

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo == null) continue;
            final String nametagRule = isLevelled ?
                    ruleInfo.nametag_Placeholder_Levelled : ruleInfo.nametag_Placeholder_Unlevelled;
            if (nametagRule != null)
                nametag = nametagRule;
        }

        return nametag;
    }

    @NotNull
    public String getRule_Nametag_CreatureDeath(@NotNull final LivingEntityWrapper lmEntity){
        String nametag = "";
        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (!Utils.isNullOrEmpty(ruleInfo.nametag_CreatureDeath)) nametag = ruleInfo.nametag_CreatureDeath;
        }

        return nametag;
    }

    @Nullable
    public HealthIndicator getRule_NametagIndicator(@NotNull final LivingEntityWrapper lmEntity){
        HealthIndicator indicator = null;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.healthIndicator != null) {
                if (indicator == null || ruleInfo.healthIndicator.doMerge == null || !ruleInfo.healthIndicator.doMerge)
                    indicator = ruleInfo.healthIndicator;
                else
                    indicator.mergeIndicator(ruleInfo.healthIndicator);
            }
        }

        return indicator;
    }

    @NotNull
    public List<NametagVisibilityEnum> getRule_CreatureNametagVisbility(@NotNull final LivingEntityWrapper lmEntity){
        List<NametagVisibilityEnum> result = null;

        try {
            for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
                if (ruleInfo == null) continue;
                if (ruleInfo.nametagVisibilityEnum != null)
                    result = ruleInfo.nametagVisibilityEnum;
            }
        }
        catch (ConcurrentModificationException e){
            Utils.logger.info("Got ConcurrentModificationException in getRule_CreatureNametagVisbility");
        }

        if (result == null || result.isEmpty())
            return List.of(NametagVisibilityEnum.ATTACKED, NametagVisibilityEnum.TARGETED, NametagVisibilityEnum.TRACKING);
        else
            return result;
    }

    public int getRule_nametagVisibleTime(@NotNull final LivingEntityWrapper lmEntity){
        int result = 4000;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.nametagVisibleTime != null)
                result = ruleInfo.nametagVisibleTime;
        }

        return result;
    }

    @Nullable
    public String getRule_TieredPlaceholder(@NotNull final LivingEntityWrapper lmEntity){
        List<TieredColoringInfo> coloringInfo = null;
        String tieredText = null;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.tieredColoringInfos != null) coloringInfo = ruleInfo.tieredColoringInfos;
        }

        if (coloringInfo == null) return  null;

        final int mobLevel = lmEntity.getMobLevel();
        for (final TieredColoringInfo info : coloringInfo){
            if (info.isDefault) tieredText = info.text;
            if (mobLevel >= info.minLevel && mobLevel <= info.maxLevel){
                tieredText = info.text;
                break;
            }
        }

        return tieredText;
    }

    public boolean getRule_PassengerMatchLevel(@NotNull final LivingEntityWrapper lmEntity){
        boolean result = false;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.passengerMatchLevel != null)
                result = ruleInfo.passengerMatchLevel;
        }

        return result;
    }

    @Nullable
    public String getRule_EntityOverriddenName(@NotNull final LivingEntityWrapper lmEntity, final boolean useCustomNameForNametags){
        Map<String, List<LevelTierMatching>> entityNameOverrides_Level = null;
        Map<String, LevelTierMatching> entityNameOverrides = null;

        if (lmEntity.hasOverridenEntityName())
            return lmEntity.getOverridenEntityName();

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            final boolean doMerge = ruleInfo.mergeEntityNameOverrides != null && ruleInfo.mergeEntityNameOverrides;
            if (ruleInfo.entityNameOverrides != null){
                if (entityNameOverrides != null && doMerge)
                    entityNameOverrides.putAll(ruleInfo.entityNameOverrides);
                else
                    entityNameOverrides = ruleInfo.entityNameOverrides;
            }

            if (ruleInfo.entityNameOverrides_Level != null) {
                if (entityNameOverrides_Level != null && doMerge)
                    entityNameOverrides_Level.putAll(ruleInfo.entityNameOverrides_Level);
                else
                    entityNameOverrides_Level = ruleInfo.entityNameOverrides_Level;
            }
        }

        if (entityNameOverrides == null && entityNameOverrides_Level == null) return null;

        List<String> namesInfo = null;
        final LevelTierMatching matchedTiers = getEntityNameOverrideLevel(entityNameOverrides_Level, lmEntity);
        if (matchedTiers != null)
            namesInfo = matchedTiers.names;
        else if (entityNameOverrides != null){
            if (entityNameOverrides.containsKey("all_entities"))
                namesInfo = entityNameOverrides.get("all_entities").names;
            else if (entityNameOverrides.containsKey(lmEntity.getNameIfBaby()))
                namesInfo = entityNameOverrides.get(lmEntity.getNameIfBaby()).names;
        }

        if (namesInfo == null || namesInfo.isEmpty())
            return null;
        else if (namesInfo.size() > 1)
            Collections.shuffle(namesInfo);

        final String entityName = Utils.capitalize(lmEntity.getNameIfBaby().replaceAll("_", " "));
        String result = namesInfo.get(0);
        result = result.replace("%entity-name%", entityName);
        result = result.replace("%displayname%", (lmEntity.getLivingEntity().getCustomName() == null || useCustomNameForNametags ?
                entityName : lmEntity.getLivingEntity().getCustomName()));

        if (namesInfo.size() > 1){
            // set a PDC key with the name otherwise the name will constantly change
            lmEntity.setOverridenEntityName(result);
        }

        return result;
    }

    @Nullable
    private LevelTierMatching getEntityNameOverrideLevel(final Map<String, List<LevelTierMatching>> entityNameOverrides_Level, final LivingEntityWrapper lmEntity){
        if (entityNameOverrides_Level == null) return null;

        LevelTierMatching allEntities = null;
        LevelTierMatching thisMob = null;

        for (final List<LevelTierMatching> tiers : entityNameOverrides_Level.values()) {
            for (final LevelTierMatching tier : tiers) {
                if (tier.isApplicableToMobLevel(lmEntity.getMobLevel())) {
                    if ("all_entities".equalsIgnoreCase(tier.mobName) && tier.isApplicableToMobLevel(lmEntity.getMobLevel()))
                        allEntities = tier;
                    else if (lmEntity.getNameIfBaby().equalsIgnoreCase(tier.mobName) && tier.isApplicableToMobLevel(lmEntity.getMobLevel()))
                        thisMob = tier;
                }
            }
        }

        if (thisMob != null)
            return thisMob;
        else
            return allEntities;
    }

    @Nullable
    public Particle getSpawnerParticle(final @NotNull LivingEntityWrapper lmEntity){
        Particle result = Particle.SOUL;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.spawnerParticle != null)
                result = ruleInfo.spawnerParticle;
            else if (ruleInfo.useNoSpawnerParticles)
                result = null;
        }

        return result;
    }

    public int getSpawnerParticleCount(final @NotNull LivingEntityWrapper lmEntity){
        int result = 10;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.spawnerParticlesCount != null)
                result = ruleInfo.spawnerParticlesCount;
        }

        // max limit of 100 counts which would take 5 seconds to show
        if (result > 100) result = 100;

        return result;
    }

    public int getMaximumDeathInChunkThreshold(final @NotNull LivingEntityWrapper lmEntity){
        int result = 0;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.maximumDeathInChunkThreshold != null) result = ruleInfo.maximumDeathInChunkThreshold;
        }

        return result;
    }

    public int getMaxChunkCooldownTime(final @NotNull LivingEntityWrapper lmEntity){
        int result = 0;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.chunkMaxCoolDownTime != null) result = ruleInfo.chunkMaxCoolDownTime;
        }

        return result;
    }

    public boolean disableVanillaDropsOnChunkMax(final @NotNull LivingEntityWrapper lmEntity){
        boolean result = false;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.disableVanillaDropsOnChunkMax != null) result = ruleInfo.disableVanillaDropsOnChunkMax;
        }

        return result;
    }

    public int getAdjacentChunksToCheck(final @NotNull LivingEntityWrapper lmEntity){
        int result = 0;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.maxAdjacentChunks != null) result = ruleInfo.maxAdjacentChunks;
        }

        return result;
    }

    @NotNull
    public ApplicableRulesResult getApplicableRules(final LivingEntityInterface lmInterface){
        final ApplicableRulesResult applicableRules = new ApplicableRulesResult();

        for (final List<RuleInfo> rules : rulesInEffect.values()) {
            for (final RuleInfo ruleInfo : rules) {

                if (!ruleInfo.ruleIsEnabled) continue;

                if (lmInterface instanceof LivingEntityWrapper && !isRuleApplicable_Entity((LivingEntityWrapper) lmInterface, ruleInfo))
                    continue;

                final RuleCheckResult checkResult = isRuleApplicable_Interface(lmInterface, ruleInfo);
                if (!checkResult.useResult) {
                    if (checkResult.ruleMadeChance != null && !checkResult.ruleMadeChance)
                        applicableRules.allApplicableRules_DidNotMakeChance.add(ruleInfo);
                    continue;
                }
                else if (checkResult.ruleMadeChance != null && checkResult.ruleMadeChance)
                    applicableRules.allApplicableRules_MadeChance.add(ruleInfo);

                applicableRules.allApplicableRules.add(ruleInfo);

                if (ruleInfo.stopProcessingRules != null && ruleInfo.stopProcessingRules) {
                    Utils.debugLog(main, DebugType.DENIED_RULE_STOP_PROCESSING, String.format("&b%s&7, mob: &b%s&7, rule count: &b%s",
                            ruleInfo.getRuleName(), lmInterface.getTypeName(), applicableRules.allApplicableRules.size()));
                    break;
                }
            }
        }

        boolean hasWorldListSpecified = false;
        for (final RuleInfo ri : applicableRules.allApplicableRules) {
            if (ri.conditions_Worlds != null && (!ri.conditions_Worlds.isEmpty() || ri.conditions_Worlds.allowAll)){
                hasWorldListSpecified = true;
                break;
            }
        }

        return hasWorldListSpecified ?
                applicableRules : new ApplicableRulesResult();
    }

    private boolean isRuleApplicable_Entity(final LivingEntityWrapper lmEntity, @NotNull final RuleInfo ri){
        if (ri.conditions_MinLevel != null && (!lmEntity.isLevelled() || lmEntity.getMobLevel() < ri.conditions_MinLevel)) {
            Utils.debugLog(main, DebugType.DENIED_RULE_MAXLEVEL, String.format("&b%s&7, mob: &b%s&7, mob lvl: &b%s&7, rule minlvl: &b%s&7",
                    ri.getRuleName(), lmEntity.getTypeName(), lmEntity.getMobLevel(), ri.conditions_MinLevel));
            return false;
        }

        if (ri.conditions_MaxLevel != null && (!lmEntity.isLevelled() ||  lmEntity.getMobLevel() > ri.conditions_MaxLevel)) {
            Utils.debugLog(main, DebugType.DENIED_RULE_MAXLEVEL, String.format("&b%s&7, mob: &b%s&7, mob lvl: &b%s&7, rule maxlvl: &b%s&7",
                    ri.getRuleName(), lmEntity.getTypeName(), lmEntity.getMobLevel(), ri.conditions_MaxLevel));
            return false;
        }

        if (ri.conditions_CustomNames != null){
            final String customName = lmEntity.getLivingEntity().getCustomName() != null ?
                    lmEntity.getLivingEntity().getCustomName() : "(none)";

            if (!ri.conditions_CustomNames.isEnabledInList(customName, lmEntity)){
                Utils.debugLog(main, DebugType.DENIED_RULE_CUSTOM_NAME, String.format("&b%s&7, mob: &b%s&7, name: &b%s&7",
                        ri.getRuleName(), lmEntity.getTypeName(), customName));
                return false;
            }
        }

        if (ri.conditions_SpawnReasons != null && !ri.conditions_SpawnReasons.isEnabledInList(lmEntity.getSpawnReason(), lmEntity)){
            Utils.debugLog(main, DebugType.DENIED_RULE_SPAWN_REASON, String.format("&b%s&7, mob: &b%s&7, spawn reason: &b%s&7",
                    ri.getRuleName(), lmEntity.getTypeName(), lmEntity.getSpawnReason()));
            return false;
        }

        if (lmEntity.isMobOfExternalType() && ri.conditions_ApplyPlugins != null && !ri.conditions_ApplyPlugins.isEnabledInList(lmEntity.getTypeName(), lmEntity)){
            Utils.debugLog(main, DebugType.DENIED_RULE_PLUGIN_COMPAT, String.format("&b%s&7, mob: &b%s&7, mob plugin: &b%s&7",
                    ri.getRuleName(), lmEntity.getTypeName(), lmEntity.getMobExternalTypes()));
            return false;
        }

        if (ri.conditions_MM_Names != null){
            String mm_Name = ExternalCompatibilityManager.getMythicMobInternalName(lmEntity);
            if (mm_Name.isEmpty()) mm_Name = "(none)";

            if (!ri.conditions_MM_Names.isEnabledInList(mm_Name, lmEntity)) {
                Utils.debugLog(main, DebugType.DENIED_RULE_MYTHIC_MOBS_INTERNAL_NAME, String.format("&b%s&7, mob: &b%s&7, mm_name: &b%s&7",
                        ri.getRuleName(), lmEntity.getTypeName(), mm_Name));
                return false;
            }
        }

        if (ri.conditions_SpawnerNames != null) {
            String checkName = lmEntity.getSourceSpawnerName();
            if (checkName == null) checkName = "(none)";

            if (!ri.conditions_SpawnerNames.isEnabledInList(checkName, lmEntity)) {
                Utils.debugLog(main, DebugType.DENIED_RULE_SPAWN_REASON, String.format("&b%s&7, mob: &b%s&7, spawner: &b%s&7",
                        ri.getRuleName(), lmEntity.getNameIfBaby(), checkName));
                return false;
            }
        }

        if (ri.conditions_SpawnegEggNames != null) {
            String checkName = lmEntity.getSourceSpawnEggName();
            if (checkName == null) checkName = "(none)";

            if (!ri.conditions_SpawnegEggNames.isEnabledInList(checkName, lmEntity)) {
                Utils.debugLog(main, DebugType.DENIED_RULE_SPAWNER_NAME, String.format("&b%s&7, mob: &b%s&7, spawn_egg: &b%s&7",
                        ri.getRuleName(), lmEntity.getNameIfBaby(), checkName));
                return false;
            }
        }

        if (ri.conditions_Permission != null){
            if (lmEntity.playerForPermissionsCheck == null){
                Utils.debugLog(main, DebugType.DENIED_RULE_PERMISSION, String.format("&b%s&7, mob: &b%s&7, no player was provided",
                        ri.getRuleName(), lmEntity.getNameIfBaby()));
                return false;
            }

            if (!doesPlayerPassPermissionChecks(ri.conditions_Permission, lmEntity.playerForPermissionsCheck)){
                Utils.debugLog(main, DebugType.DENIED_RULE_PERMISSION, String.format("&b%s&7, mob: &b%s&7, player: &b%s&7, permission denied",
                        ri.getRuleName(), lmEntity.getNameIfBaby(), lmEntity.playerForPermissionsCheck.getName()));
                return false;
            }
        }

        if (ri.conditions_MobTamedStatus != MobTamedStatus.NOT_SPECIFIED && ri.conditions_MobTamedStatus != MobTamedStatus.EITHER &&
            lmEntity.getLivingEntity() instanceof Tameable){
            if (lmEntity.isMobTamed() && ri.conditions_MobTamedStatus == MobTamedStatus.NOT_TAMED ||
                !lmEntity.isMobTamed() && ri.conditions_MobTamedStatus == MobTamedStatus.TAMED){
                Utils.debugLog(main, DebugType.ENTITY_TAME, String.format("&b%s&7, mob: &b%s&7, tamed: %s, rule: %s",
                        ri.getRuleName(), lmEntity.getNameIfBaby(), lmEntity.isMobTamed(), ri.conditions_MobTamedStatus));
                return false;
            }
        }

        if (ri.conditions_ScoreboardTags != null){
            final Set<String> tags = lmEntity.getLivingEntity().getScoreboardTags();
            if (tags.isEmpty()) tags.add("(none)");

            boolean madeCriteria = false;
            for (final String tag : tags){
                if (ri.conditions_ScoreboardTags.isEnabledInList(tag, lmEntity))
                    madeCriteria = true;
            }
            if (!madeCriteria){
                Utils.debugLog(main, DebugType.SCOREBOARD_TAGS, String.format("&b%s&7, mob: &b%s&7",
                        ri.getRuleName(), lmEntity.getNameIfBaby()));
                return false;
            }
        }

        if (ri.conditions_SkyLightLevel != null){
            final int lightLevel = lmEntity.getSkylightLevel();
            if (lightLevel < ri.conditions_SkyLightLevel.min || lightLevel > ri.conditions_SkyLightLevel.max){
                Utils.debugLog(main, DebugType.SKYLIGHT_LEVEL, String.format(
                        "&b%s&7, mob: &b%s&7, skylight: %s, criteria: %s",
                        ri.getRuleName(), lmEntity.getNameIfBaby(), lightLevel, ri.conditions_SkyLightLevel));
                return false;
            }
        }

        return true;
    }

    @Contract("_, _ -> new")
    private @NotNull RuleCheckResult isRuleApplicable_Interface(final LivingEntityInterface lmInterface, final RuleInfo ri){

        if (lmInterface instanceof LivingEntityWrapper) {
            if (ri.conditions_Entities != null && !Utils.isLivingEntityInModalList(ri.conditions_Entities, (LivingEntityWrapper) lmInterface)) {
                Utils.debugLog(main, DebugType.DENIED_RULE_ENTITIES_LIST, String.format("&b%s&7, mob: &b%s&7", ri.getRuleName(), lmInterface.getTypeName()));
                return new RuleCheckResult(false);
            }
        } else {
            // can't check groups if not a living entity wrapper
            if (ri.conditions_Entities != null && !ri.conditions_Entities.isEnabledInList(lmInterface.getTypeName(), null)) {
                Utils.debugLog(main, DebugType.DENIED_RULE_ENTITIES_LIST, String.format("&b%s&7, mob: &b%s&7", ri.getRuleName(), lmInterface.getTypeName()));
                return new RuleCheckResult(false);
            }
        }

        if (!(lmInterface.isWasSummoned()) && ri.conditions_Worlds != null && !ri.conditions_Worlds.isEnabledInList(lmInterface.getWorld().getName(), null)) {
            Utils.debugLog(main, DebugType.DENIED_RULE_WORLD_LIST, String.format("&b%s&7, mob: &b%s&7, mob world: &b%s&7",
                    ri.getRuleName(), lmInterface.getTypeName(), lmInterface.getWorld().getName()));
            return new RuleCheckResult(false);
        }

        if (ri.conditions_Biomes != null && !Utils.isBiomeInModalList(ri.conditions_Biomes, lmInterface.getLocation().getBlock().getBiome(), main.rulesManager)) {
            Utils.debugLog(main, DebugType.DENIED_RULE_BIOME_LIST, String.format("&b%s&7, mob: &b%s&7, mob biome: &b%s&7",
                    ri.getRuleName(), lmInterface.getTypeName(), lmInterface.getLocation().getBlock().getBiome().name()));
            return new RuleCheckResult(false);
        }

        if (ri.conditions_WGRegions != null){
            boolean isInList = false;
            final List<String> wgRegions = ExternalCompatibilityManager.getWGRegionsAtLocation(lmInterface);
            if (wgRegions.isEmpty()) wgRegions.add("(none)");

            for (final String regionName : wgRegions) {
                if (ri.conditions_WGRegions.isEnabledInList(regionName, null)) {
                    isInList = true;
                    break;
                }
            }

            if (!isInList){
                Utils.debugLog(main, DebugType.DENIED_RULE_WG_REGION, String.format("&b%s&7, mob: &b%s&7, wg_regions: &b%s&7",
                        ri.getRuleName(), lmInterface.getTypeName(), wgRegions));
                return new RuleCheckResult(false);
            }
        }

        if (ri.conditions_ApplyAboveY != null && lmInterface.getLocation().getBlockY() < ri.conditions_ApplyAboveY){
            Utils.debugLog(main, DebugType.DENIED_RULE_Y_LEVEL, String.format("&b%s&7, mob: &b%s&7, y-level: &b%s&7, max-y: &b%s&7",
                    ri.getRuleName(), lmInterface.getTypeName(), lmInterface.getLocation().getBlockY(), ri.conditions_ApplyAboveY));
            return new RuleCheckResult(false);
        }

        if (ri.conditions_ApplyBelowY != null && lmInterface.getLocation().getBlockY() > ri.conditions_ApplyBelowY){
            Utils.debugLog(main, DebugType.DENIED_RULE_Y_LEVEL, String.format("&b%s&7, mob: &b%s&7, y-level: &b%s&7, min-y: &b%s&7",
                    ri.getRuleName(), lmInterface.getTypeName(), lmInterface.getLocation().getBlockY(), ri.conditions_ApplyBelowY));
            return new RuleCheckResult(false);
        }

        if (ri.conditions_MinDistanceFromSpawn != null){
            if (lmInterface.getDistanceFromSpawn() < ri.conditions_MinDistanceFromSpawn){
                Utils.debugLog(main, DebugType.DENIED_RULE_MIN_SPAWN_DISTANCE, String.format("&b%s&7, mob: &b%s&7, spawn-distance: &b%s&7, min-sd: &b%s&7",
                        ri.getRuleName(), lmInterface.getTypeName(), Utils.round(lmInterface.getDistanceFromSpawn()), ri.conditions_MinDistanceFromSpawn));
                return new RuleCheckResult(false);
            }
        }

        if (ri.conditions_MaxDistanceFromSpawn != null){
            if (lmInterface.getDistanceFromSpawn() > ri.conditions_MaxDistanceFromSpawn){
                Utils.debugLog(main, DebugType.DENIED_RULE_MAX_SPAWN_DISTANCE, String.format("&b%s&7, mob: &b%s&7, spawn-distance: &b%s&7, min-sd: &b%s&7",
                        ri.getRuleName(), lmInterface.getTypeName(), Utils.round(lmInterface.getDistanceFromSpawn()), ri.conditions_MaxDistanceFromSpawn));
                return new RuleCheckResult(false);
            }
        }

        if (ri.conditions_WorldTickTime != null){
            final int currentWorldTickTime = lmInterface.getSpawnedTimeOfDay();

            if (!Utils.isIntegerInModalList(ri.conditions_WorldTickTime, currentWorldTickTime)){
                Utils.debugLog(main, DebugType.DENIED_RULE_WORLD_TIME_TICK, String.format("&b%s&7, mob: &b%s&7, tick time: &b%s&7",
                        ri.getRuleName(), lmInterface.getTypeName(), currentWorldTickTime));
                return new RuleCheckResult(false, false);
            }
        }

        Boolean ruleMadeChance = null;

        if (ri.conditions_Chance != null && ri.conditions_Chance < 1.0){
            if (lmInterface instanceof LivingEntityWrapper){
                final LivingEntityWrapper lmEntity = (LivingEntityWrapper) lmInterface;
                // find out if this entity previously lost or won the chance previously and use that result if present
                final Map<String, Boolean> prevChanceResults = lmEntity.getPrevChanceRuleResults();
                if (prevChanceResults != null && prevChanceResults.containsKey(ri.getRuleName())){
                    final boolean prevResult = prevChanceResults.get(ri.getRuleName());
                    return new RuleCheckResult(prevResult);
                }
            }

            final float chanceRole = (float) ThreadLocalRandom.current().nextInt(0, 100001) * 0.00001F;
            if (chanceRole < (1.0F - ri.conditions_Chance)){
                Utils.debugLog(main, DebugType.DENIED_RULE_CHANCE, String.format("&b%s&7, mob: &b%s&7, chance: &b%s&7, chance role: &b%s&7",
                        ri.getRuleName(), lmInterface.getTypeName(), ri.conditions_Chance, Utils.round(chanceRole, 4)));
                return new RuleCheckResult(false, false);
            }

            ruleMadeChance = true;
        }

        return new RuleCheckResult(true, ruleMadeChance);
    }

    private boolean doesPlayerPassPermissionChecks(final @NotNull CachedModalList<String> perms, final @NotNull Player player){
        if (perms.allowAll) return true;
        if (perms.excludeAll) return false;
        if (perms.isEmpty()) return true;

        for (final String perm : perms.excludedList){
            final String permCheck = "levelledmobs.permission." + perm;
            if (player.hasPermission(permCheck))
                return false;
        }

        for (final String perm : perms.allowedList){
            final String permCheck = "levelledmobs.permission." + perm;
            if (player.hasPermission(permCheck))
                return true;
        }

        return perms.isBlacklist();
    }

    public void buildBiomeGroupMappings(final Map<String, Set<String>> customBiomeGroups){
        this.biomeGroupMappings.clear();

        this.biomeGroupMappings.put("SNOWY_BIOMES", List.of("SNOWY_TUNDRA", "ICE_SPIKES", "SNOWY_TAIGA", "SNOWY_TAIGA_MOUNTAINS",
                "SNOWY_TAIGA_HILLS", "FROZEN_RIVER", "SNOWY_BEACH", "SNOWY_MOUNTAINS"));

        this.biomeGroupMappings.put("COLD_BIOMES", List.of("MOUNTAINS", "GRAVELLY_MOUNTAINS", "MODIFIED_GRAVELLY_MOUNTAINS",
                "WOODED_MOUNTAINS", "TAIGA", "TAIGA_MOUNTAINS", "TAIGA_HILLS", "GIANT_TREE_TAIGA", "GIANT_TREE_TAIGA_HILLS",
                "GIANT_SPRUCE_TAIGA", "GIANT_SPRUCE_TAIGA_HILLS", "STONE_SHORE"));

        this.biomeGroupMappings.put("TEMPERATE_BIOMES", List.of("PLAINS", "SUNFLOWER_PLAINS", "FOREST", "FLOWER_FOREST",
                "BIRCH_FOREST", "BIRCH_FOREST_HILLS", "TALL_BIRCH_FOREST", "TALL_BIRCH_HILLS", "DARK_FOREST", "DARK_FOREST_HILLS",
                "SWAMP", "SWAMP_HILLS", "JUNGLE", "MODIFIED_JUNGLE", "JUNGLE_HILLS", "MODIFIED_JUNGLE_EDGE", "BAMBOO_JUNGLE",
                "BAMBOO_JUNGLE_HILLS", "RIVER", "BEACH", "MUSHROOM_FIELDS", "MUSHROOM_FIELD_SHORE", "WOODED_HILLS"));

        this.biomeGroupMappings.put("DRY_BIOMES", List.of("DESERT", "DESERT_LAKES", "DESERT_HILLS", "SAVANNA",
                "SHATTERED_SAVANNA", "SHATTERED_SAVANNA_PLATEAU", "BADLANDS", "ERODED_BADLANDS", "WOODED_BADLANDS_PLATEAU",
                "BADLANDS_PLATEAU", "SAVANNA_PLATEAU", "MODIFIED_BADLANDS_PLATEAU", "MODIFIED_WOODED_BADLANDS_PLATEAU", "MODIFIED_SAVANNA_PLATEAU"));

        this.biomeGroupMappings.put("OCEAN_BIOMES", List.of("WARM_OCEAN", "DEEP_WARM_OCEAN", "LUKEWARM_OCEAN", "DEEP_LUKEWARM_OCEAN", "OCEAN",
                "DEEP_OCEAN", "COLD_OCEAN", "DEEP_COLD_OCEAN", "FROZEN_OCEAN", "DEEP_FROZEN_OCEAN"));

        this.biomeGroupMappings.put("NETHER_BIOMES", List.of("NETHER_WASTES", "CRIMSON_FOREST", "WARPED_FOREST", "SOUL_SAND_VALLEY", "BASALT_DELTAS"));

        this.biomeGroupMappings.put("END_BIOMES", List.of("THE_END", "SMALL_END_ISLANDS", "END_MIDLANDS", "END_HIGHLANDS", "END_BARRENS"));

        if (customBiomeGroups == null) return;

        for (final Map.Entry<String, Set<String>> groupName : customBiomeGroups.entrySet()){
            final Set<String> groupMembers = groupName.getValue();
            final List<String> newList = new ArrayList<>(groupMembers.size());
            newList.addAll(groupMembers);
            this.biomeGroupMappings.put(groupName.getKey(), newList);
        }
    }
}
