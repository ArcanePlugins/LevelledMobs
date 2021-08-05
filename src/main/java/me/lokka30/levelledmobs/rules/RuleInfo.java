/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.rules;

import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.misc.CachedModalList;
import me.lokka30.levelledmobs.rules.strategies.LevellingStrategy;
import org.bukkit.block.Biome;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Holds rules parsed from rules.yml to make up a list of rules
 *
 * @author stumper66
 */
public class RuleInfo {
    public RuleInfo(final String id){
        this.ruleName = id;

        this.ruleIsEnabled = true;
        this.ruleSourceNames = new TreeMap<>();
        this.conditions_MobCustomnameStatus = MobCustomNameStatus.NOT_SPECIFIED;
        this.conditions_MobTamedStatus = MobTamedStatus.NOT_SPECIFIED;
    }

    private String ruleName;
    public boolean ruleIsEnabled;
    public Boolean CreatureNametagAlwaysVisible;
    public Boolean babyMobsInheritAdultSetting;
    public Boolean mobLevelInheritance;
    public Boolean customDrops_UseForMobs;
    public Boolean customDrops_UseOverride;
    public Boolean stopProcessingRules;
    public Boolean useRandomLevelling;
    public Boolean mergeEntityNameOverrides;
    public int rulePriority;
    public Integer maxRandomVariance;
    public Integer creeperMaxDamageRadius;
    public Integer conditions_MinLevel;
    public Integer conditions_MaxLevel;
    public Integer restrictions_MinLevel;
    public Integer restrictions_MaxLevel;
    public Integer lowerMobLevelBiasFactor;
    public Integer conditions_ApplyAboveY;
    public Integer conditions_ApplyBelowY;
    public Integer conditions_MinDistanceFromSpawn;
    public Integer conditions_MaxDistanceFromSpawn;
    public Double conditions_Chance;
    public Double sunlightBurnAmount;
    public String nametag;
    public String nametag_CreatureDeath;
    public String presetName;
    public String customDrop_DropTableId;
    public String mobNBT_Data;
    public HealthIndicator healthIndicator;
    public MobCustomNameStatus conditions_MobCustomnameStatus;
    public MobTamedStatus conditions_MobTamedStatus;
    public LevellingStrategy levellingStrategy;
    public PlayerLevellingOptions playerLevellingOptions;
    public Map<String, List<LevelTierMatching>> entityNameOverrides_Level;
    public Map<String, LevelTierMatching> entityNameOverrides;
    @NotNull
    public final Map<String, String> ruleSourceNames;
    public List<TieredColoringInfo> tieredColoringInfos;
    public Map<ExternalCompatibilityManager.ExternalCompatibility, Boolean> enabledExtCompats;
    public CachedModalList<String> allowedEntities;
    public CachedModalList<String> conditions_Worlds;
    public CachedModalList<String> conditions_Entities;
    public CachedModalList<Biome> conditions_Biomes;
    public CachedModalList<String> conditions_ApplyPlugins;
    public CachedModalList<String> conditions_CustomNames;
    public CachedModalList<String> conditions_NoDropEntities;
    public CachedModalList<String> conditions_WGRegions;
    public CachedModalList<String> conditions_MM_Names;
    public CachedModalList<CreatureSpawnEvent.SpawnReason> conditions_SpawnReasons;
    @Nullable
    public FineTuningAttributes allMobMultipliers;
    public Map<String, FineTuningAttributes> specificMobMultipliers;

    public String getRuleName(){
        return this.ruleName;
    }

    public void setRuleName(final String name){
        this.ruleName = name;
    }

    // this is only used for presets
    public void mergePresetRules(final RuleInfo preset){
        if (preset == null) return;

        try {
            for (final Field f : preset.getClass().getDeclaredFields()) {
                if (!Modifier.isPublic(f.getModifiers())) continue;
                if (f.getName().equals("ruleIsEnabled")) continue;
                if (f.getName().equals("presetName")) continue;
                if (f.getName().equals("ruleSourceNames")) continue;
                if (f.get(preset) == null) continue;

                boolean skipSettingValue = false;
                final Object presetValue = f.get(preset);

                if (f.getName().equals("entityNameOverrides") && this.entityNameOverrides != null) {
                    this.entityNameOverrides.putAll((Map<String, LevelTierMatching>) presetValue);
                    skipSettingValue = true;
                } else if (f.getName().equals("entityNameOverrides_Level") && this.entityNameOverrides_Level != null) {
                    this.entityNameOverrides_Level.putAll((Map<String, List<LevelTierMatching>>) presetValue);
                    skipSettingValue = true;
                } else if (f.getName().equals("healthIndicator")) {
                    final HealthIndicator mergingPreset = (HealthIndicator) presetValue;
                    if (this.healthIndicator == null || mergingPreset.doMerge == null || !mergingPreset.doMerge)
                        this.healthIndicator = mergingPreset;
                    else
                        this.healthIndicator.mergeIndicator(mergingPreset.cloneItem());

                    skipSettingValue = true;
                } else if (f.getName().equals("allMobMultipliers")) {
                    final FineTuningAttributes mergingPreset = (FineTuningAttributes) presetValue;
                    if (this.allMobMultipliers == null)
                        this.allMobMultipliers = mergingPreset.cloneItem();
                    else
                        this.allMobMultipliers.mergeAttributes(mergingPreset);
                    skipSettingValue = true;
                } else if (f.getName().equals("specificMobMultipliers")){
                    final Map<String, FineTuningAttributes> mergingPreset = (Map<String, FineTuningAttributes>) presetValue;
                    if (this.specificMobMultipliers == null)
                        this.specificMobMultipliers = new TreeMap<>();

                    for (final String entityType : mergingPreset.keySet())
                        this.specificMobMultipliers.put(entityType, mergingPreset.get(entityType).cloneItem());

                    skipSettingValue = true;
                }

                if (presetValue instanceof CachedModalList){
                    final CachedModalList<?> cachedModalList_preset = (CachedModalList<?>) presetValue;
                    final CachedModalList<?> thisCachedModalList = (CachedModalList<?>) this.getClass().getDeclaredField(f.getName()).get(this);

                    if (thisCachedModalList != null && cachedModalList_preset.doMerge)
                        thisCachedModalList.mergeCachedModal(cachedModalList_preset);
                    else
                        this.getClass().getDeclaredField(f.getName()).set(this, cachedModalList_preset.clone());

                    skipSettingValue = true;
                }
                if (presetValue instanceof LevellingStrategy) {
                    if (this.levellingStrategy != null && this.levellingStrategy.getClass().equals(presetValue.getClass())) {
                        this.levellingStrategy.mergeRule((LevellingStrategy) presetValue);
                    } else
                        this.levellingStrategy = (LevellingStrategy) presetValue;
                    skipSettingValue = true;
                }

                if (presetValue instanceof TieredColoringInfo) {
                    this.getClass().getDeclaredField(f.getName()).set(this, ((TieredColoringInfo) presetValue).cloneItem());
                    skipSettingValue = true;
                }

                if (presetValue == MobCustomNameStatus.NOT_SPECIFIED) continue;
                if (presetValue == MobTamedStatus.NOT_SPECIFIED) continue;

                // skip default values such as false, 0, 0.0
                if (presetValue instanceof Boolean && !((Boolean) presetValue)) continue;
                if (presetValue instanceof Integer && ((Integer) presetValue == 0)) continue;
                if (presetValue instanceof Double && ((Double) presetValue == 0.0)) continue;

                if (!skipSettingValue)
                    this.getClass().getDeclaredField(f.getName()).set(this, presetValue);
                this.ruleSourceNames.put(f.getName(), preset.ruleName);
            }
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }
}

