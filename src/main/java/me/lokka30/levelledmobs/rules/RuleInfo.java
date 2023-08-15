/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.rules;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.misc.CachedModalList;
import me.lokka30.levelledmobs.rules.strategies.LevellingStrategy;
import me.lokka30.levelledmobs.util.MessageUtils;
import org.bukkit.Particle;
import org.bukkit.block.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds rules parsed from rules.yml to make up a list of rules
 *
 * @author stumper66
 * @since 3.0.0
 */
public class RuleInfo {

    public RuleInfo(final String id) {
        this.ruleName = id;

        this.ruleIsEnabled = true;
        this.ruleSourceNames = new TreeMap<>();
        this.conditions_MobCustomnameStatus = MobCustomNameStatus.NOT_SPECIFIED;
        this.conditions_MobTamedStatus = MobTamedStatus.NOT_SPECIFIED;
        this.customDrop_DropTableIds = new LinkedList<>();
    }

    private @ExcludeFromHash String ruleName;
    public @DoNotMerge boolean ruleIsEnabled;
    @DoNotMerge @ExcludeFromHash boolean isTempDisabled;
    public @ExcludeFromHash boolean useNoSpawnerParticles;
    public Boolean babyMobsInheritAdultSetting;
    public Boolean mobLevelInheritance;
    public @ExcludeFromHash Boolean customDrops_UseForMobs;
    public Boolean stopProcessingRules;
    public @ExcludeFromHash Boolean mergeEntityNameOverrides;
    public Boolean passengerMatchLevel;
    @ExcludeFromHash Boolean lockEntity;
    @DoNotMerge @ExcludeFromHash int rulePriority;
    public @ExcludeFromHash Integer spawnerParticlesCount;
    public Integer maxRandomVariance;
    public @ExcludeFromHash Integer creeperMaxDamageRadius;
    public Integer conditions_MinLevel;
    public Integer conditions_MaxLevel;
    public Integer restrictions_MinLevel;
    public Integer restrictions_MaxLevel;
    public Integer conditions_ApplyAboveY;
    public Integer conditions_ApplyBelowY;
    public Integer conditions_MinDistanceFromSpawn;
    public Integer conditions_MaxDistanceFromSpawn;
    public @ExcludeFromHash Long nametagVisibleTime;
    public @ExcludeFromHash Integer maximumDeathInChunkThreshold;
    public @ExcludeFromHash Integer chunkMaxCoolDownTime;
    public @ExcludeFromHash Integer maxAdjacentChunks;
    public @ExcludeFromHash Long conditions_CooldownTime;
    public @ExcludeFromHash Integer conditions_TimesToCooldownActivation;
    public @ExcludeFromHash Float conditions_Chance;
    public @ExcludeFromHash Double sunlightBurnAmount;
    public @ExcludeFromHash String nametag;
    public @ExcludeFromHash String nametag_CreatureDeath;
    public @ExcludeFromHash String nametag_Placeholder_Levelled;
    public @ExcludeFromHash String nametag_Placeholder_Unlevelled;
    public @DoNotMerge @ExcludeFromHash String presetName;
    public final @NotNull @ExcludeFromHash List<String> customDrop_DropTableIds;
    public @ExcludeFromHash HealthIndicator healthIndicator;
    public MobCustomNameStatus conditions_MobCustomnameStatus;
    public MobTamedStatus conditions_MobTamedStatus;
    public LevellingStrategy levellingStrategy;
    public PlayerLevellingOptions playerLevellingOptions;
    public @ExcludeFromHash Map<String, List<LevelTierMatching>> entityNameOverrides_Level;
    public @ExcludeFromHash Map<String, LevelTierMatching> entityNameOverrides;
    public @ExcludeFromHash DeathMessages deathMessages;
    public @ExcludeFromHash List<NametagVisibilityEnum> nametagVisibilityEnum;
    public @NotNull @DoNotMerge @ExcludeFromHash final Map<String, String> ruleSourceNames;
    public @ExcludeFromHash Particle spawnerParticle;
    public @ExcludeFromHash List<TieredColoringInfo> tieredColoringInfos;
    public Map<ExternalCompatibilityManager.ExternalCompatibility, Boolean> enabledExtCompats;
    public MergeableStringList mobNBT_Data;
    public CachedModalList<String> allowedEntities;
    public MinAndMax conditions_SkyLightLevel;
    public CachedModalList<String> conditions_Worlds;
    public CachedModalList<String> conditions_Entities;
    public CachedModalList<Biome> conditions_Biomes;
    public CachedModalList<String> conditions_ApplyPlugins;
    public CachedModalList<String> conditions_CustomNames;
    public CachedModalList<String> conditions_NoDropEntities;
    public CachedModalList<String> conditions_WGRegions;
    public CachedModalList<String> conditions_WGRegionOwners;
    public CachedModalList<String> conditions_MM_Names;
    public CachedModalList<String> conditions_SpawnerNames;
    public CachedModalList<String> conditions_SpawnegEggNames;
    public CachedModalList<String> conditions_ScoreboardTags;
    public CachedModalList<MinAndMax> conditions_WorldTickTime;
    public CachedModalList<VanillaBonusEnum> vanillaBonuses;
    public CachedModalList<LevelledMobSpawnReason> conditions_SpawnReasons;
    public CachedModalList<String> conditions_Permission;
    public WithinCoordinates conditions_WithinCoords;
    public @Nullable FineTuningAttributes allMobMultipliers;
    public Map<String, FineTuningAttributes> specificMobMultipliers;
    public @ExcludeFromHash ChunkKillOptions chunkKillOptions;

    public String getRuleName() {
        return this.ruleName;
    }

    void setRuleName(final String name) {
        this.ruleName = name;
    }

    // this is only used for presets
    @SuppressWarnings("unchecked")
    void mergePresetRules(final RuleInfo preset) {
        if (preset == null) {
            return;
        }

        try {
            for (final Field f : preset.getClass().getDeclaredFields()) {
                if (Modifier.isPrivate(f.getModifiers())) {
                    continue;
                }
                if (f.isAnnotationPresent(DoNotMerge.class)) {
                    continue;
                }
                if (f.get(preset) == null) {
                    continue;
                }

                boolean skipSettingValue = false;
                Object presetValue = f.get(preset);
                final Object ruleValue = this.getClass().getDeclaredField(f.getName()).get(this);

                if (f.getName().equals("entityNameOverrides") && this.entityNameOverrides != null
                    && presetValue instanceof Map) {
                    this.entityNameOverrides.putAll((Map<String, LevelTierMatching>) presetValue);
                    skipSettingValue = true;
                }
                else if (presetValue instanceof final MergableRule mergableRule){
                    if (ruleValue != null && mergableRule.doMerge()) {
                        ((MergableRule)ruleValue).merge((MergableRule) mergableRule.cloneItem());
                        skipSettingValue = true;
                    }
                    else{
                        presetValue = mergableRule.cloneItem();
                    }
                }
                else if (f.getName().equals("entityNameOverrides_Level")
                    && this.entityNameOverrides_Level != null) {
                    this.entityNameOverrides_Level.putAll(
                        (Map<String, List<LevelTierMatching>>) presetValue);
                    skipSettingValue = true;
                } else if (f.getName().equals("specificMobMultipliers")) {
                    final Map<String, FineTuningAttributes> mergingPreset = (Map<String, FineTuningAttributes>) presetValue;
                    if (this.specificMobMultipliers == null) {
                        this.specificMobMultipliers = new TreeMap<>();
                    }

                    for (final Map.Entry<String, FineTuningAttributes> entityType : mergingPreset.entrySet()) {
                        this.specificMobMultipliers.put(entityType.getKey(),
                            entityType.getValue().cloneItem());
                    }

                    skipSettingValue = true;
                } else if (f.getName().equals("customDrop_DropTableIds")) {
                    final List<String> mergingPreset = (List<String>) presetValue;
                    this.customDrop_DropTableIds.addAll(mergingPreset);

                    skipSettingValue = true;
                } else if (presetValue instanceof final MergeableStringList mergingPreset
                    && ruleValue != null) {
                    if (mergingPreset.doMerge && !mergingPreset.isEmpty()) {
                        final MergeableStringList current = (MergeableStringList) ruleValue;
                        current.items.addAll(mergingPreset.items);
                        skipSettingValue = true;
                    }
                }

                if (presetValue instanceof final CachedModalList<?> cachedModalList_preset) {
                    final CachedModalList<?> thisCachedModalList = (CachedModalList<?>) ruleValue;

                    if (thisCachedModalList != null && cachedModalList_preset.doMerge) {
                        thisCachedModalList.mergeCachedModal(cachedModalList_preset);
                    } else {
                        this.getClass().getDeclaredField(f.getName())
                            .set(this, cachedModalList_preset.clone());
                    }

                    skipSettingValue = true;
                }
                if (presetValue instanceof LevellingStrategy) {
                    if (this.levellingStrategy != null && this.levellingStrategy.getClass()
                        .equals(presetValue.getClass())) {
                        this.levellingStrategy.mergeRule((LevellingStrategy) presetValue);
                    } else {
                        this.levellingStrategy = (LevellingStrategy) presetValue;
                    }
                    skipSettingValue = true;
                }

                if (presetValue instanceof final TieredColoringInfo tieredColoringInfo) {
                    presetValue = tieredColoringInfo.cloneItem();
                }

                if (presetValue == MobCustomNameStatus.NOT_SPECIFIED) {
                    continue;
                }
                if (presetValue == MobTamedStatus.NOT_SPECIFIED) {
                    continue;
                }

                // skip default values such as false, 0, 0.0
                if (presetValue instanceof Boolean && !((Boolean) presetValue)) {
                    continue;
                }
                if (presetValue instanceof Integer && ((Integer) presetValue == 0)) {
                    continue;
                }
                if (presetValue instanceof Double && ((Double) presetValue == 0.0)) {
                    continue;
                }

                if (!skipSettingValue) {
                    this.getClass().getDeclaredField(f.getName()).set(this, presetValue);
                }
                this.ruleSourceNames.put(f.getName(), preset.ruleName);
            }
        } catch (final IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public @NotNull String formatRulesVisually() {
        return formatRulesVisually(false, null);
    }

    public @NotNull String formatRulesVisually(final boolean isForHash, final @Nullable List<String> excludedKeys) {
        final SortedMap<String, String> values = new TreeMap<>();
        final StringBuilder sb = new StringBuilder();

        if (excludedKeys == null || excludedKeys.contains("id")) {
            sb.append("id: ");
            sb.append(getRuleName());
            sb.append("\n");
        }

        try {
            for (final Field f : this.getClass().getDeclaredFields()) {
                if (Modifier.isPrivate(f.getModifiers())) {
                    continue;
                }
                if (f.get(this) == null) {
                    continue;
                }
                if (f.getName().equals("ruleSourceNames")) {
                    continue;
                }
                if (isForHash && f.isAnnotationPresent(ExcludeFromHash.class)) {
                    continue;
                }
                if (excludedKeys != null && excludedKeys.contains(f.getName())){
                    continue;
                }
                final Object value = f.get(this);
                if (value.toString().equalsIgnoreCase("NOT_SPECIFIED")) {
                    continue;
                }
                if (value.toString().equalsIgnoreCase("{}")) {
                    continue;
                }
                if (value.toString().equalsIgnoreCase("[]")) {
                    continue;
                }
                if (value.toString().equalsIgnoreCase("0") &&
                    f.getName().equals("rulePriority")) {
                    continue;
                }
                if (value.toString().equalsIgnoreCase("0.0")) {
                    continue;
                }
                if (value.toString().equalsIgnoreCase("false") &&
                    !f.getName().equals("ruleIsEnabled")) {
                    continue;
                }
                if (value.toString().equalsIgnoreCase("NONE")) {
                    continue;
                }
                if (value instanceof final CachedModalList<?> cml) {
                    if (cml.isEmpty() && !cml.allowAll && !cml.excludeAll) {
                        continue;
                    }
                }
                final String showValue = "&b" + f.getName() + "&r, value: &b" + value + "&r";
                values.put(f.getName(), showValue);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }

        for (final String s : values.values()) {
            sb.append(MessageUtils.colorizeAll(s));
            sb.append("\n");
        }

        sb.setLength(sb.length() - 1); // remove trailing newline
        return sb.toString();
    }

    public String toString() {
        if (this.ruleName == null || this.ruleName.isEmpty()) {
            return super.toString();
        }

        return this.ruleName;
    }
}

