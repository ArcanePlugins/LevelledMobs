/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.rules;

import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.misc.CachedModalList;
import me.lokka30.levelledmobs.rules.strategies.LevellingStrategy;
import me.lokka30.microlib.messaging.MessageUtils;
import org.bukkit.Particle;
import org.bukkit.block.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import scala.Int;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Holds rules parsed from rules.yml to make up a list of rules
 *
 * @author stumper66
 * @since 3.0.0
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
    @DoNotMerge
    boolean ruleIsEnabled;
    boolean useNoSpawnerParticles;
    Boolean babyMobsInheritAdultSetting;
    Boolean mobLevelInheritance;
    public Boolean customDrops_UseForMobs;
    Boolean customDrops_UseOverride;
    Boolean stopProcessingRules;
    Boolean useRandomLevelling;
    Boolean mergeEntityNameOverrides;
    Boolean passengerMatchLevel;
    @DoNotMerge
    int rulePriority;
    Integer spawnerParticlesCount;
    Integer maxRandomVariance;
    Integer creeperMaxDamageRadius;
    Integer conditions_MinLevel;
    Integer conditions_MaxLevel;
    Integer restrictions_MinLevel;
    Integer restrictions_MaxLevel;
    Integer lowerMobLevelBiasFactor;
    Integer conditions_ApplyAboveY;
    Integer conditions_ApplyBelowY;
    Integer conditions_MinDistanceFromSpawn;
    Integer conditions_MaxDistanceFromSpawn;
    Integer nametagVisibleTime;
    Float conditions_Chance;
    Double sunlightBurnAmount;
    public String nametag;
    String nametag_CreatureDeath;
    String nametag_Placeholder_Levelled;
    String nametag_Placeholder_Unlevelled;
    @DoNotMerge
    String presetName;
    String customDrop_DropTableId;
    HealthIndicator healthIndicator;
    MobCustomNameStatus conditions_MobCustomnameStatus;
    MobTamedStatus conditions_MobTamedStatus;
    LevellingStrategy levellingStrategy;
    PlayerLevellingOptions playerLevellingOptions;
    Map<String, List<LevelTierMatching>> entityNameOverrides_Level;
    Map<String, LevelTierMatching> entityNameOverrides;
    public List<NametagVisibilityEnum> nametagVisibilityEnum;
    @NotNull @DoNotMerge
    public final Map<String, String> ruleSourceNames;
    public Particle spawnerParticle;
    List<TieredColoringInfo> tieredColoringInfos;
    Map<ExternalCompatibilityManager.ExternalCompatibility, Boolean> enabledExtCompats;
    MergeableStringList mobNBT_Data;
    CachedModalList<String> allowedEntities;
    MinAndMax conditions_SkyLightLevel;
    CachedModalList<String> conditions_Worlds;
    CachedModalList<String> conditions_Entities;
    CachedModalList<Biome> conditions_Biomes;
    CachedModalList<String> conditions_ApplyPlugins;
    CachedModalList<String> conditions_CustomNames;
    CachedModalList<String> conditions_NoDropEntities;
    CachedModalList<String> conditions_WGRegions;
    CachedModalList<String> conditions_MM_Names;
    CachedModalList<String> conditions_SpawnerNames;
    CachedModalList<String> conditions_SpawnegEggNames;
    CachedModalList<String> conditions_ScoreboardTags;
    CachedModalList<MinAndMax> conditions_WorldTickTime;
    CachedModalList<LevelledMobSpawnReason> conditions_SpawnReasons;
    CachedModalList<String> conditions_Permission;
    @Nullable FineTuningAttributes allMobMultipliers;
    Map<String, FineTuningAttributes> specificMobMultipliers;

    public String getRuleName(){
        return this.ruleName;
    }

    void setRuleName(final String name){
        this.ruleName = name;
    }

    // this is only used for presets
    @SuppressWarnings("unchecked")
    void mergePresetRules(final RuleInfo preset){
        if (preset == null) return;

        try {
            for (final Field f : preset.getClass().getDeclaredFields()) {
                if (Modifier.isPrivate(f.getModifiers())) continue;
                if (f.isAnnotationPresent(DoNotMerge.class)) continue;
                if (f.get(preset) == null) continue;

                boolean skipSettingValue = false;
                final Object presetValue = f.get(preset);

                if (f.getName().equals("entityNameOverrides") && this.entityNameOverrides != null && presetValue instanceof Map) {
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

                    for (final Map.Entry<String, FineTuningAttributes> entityType : mergingPreset.entrySet())
                        this.specificMobMultipliers.put(entityType.getKey(), entityType.getValue().cloneItem());

                    skipSettingValue = true;
                }
                else if (presetValue instanceof MergeableStringList && this.getClass().getDeclaredField(f.getName()).get(this) != null){
                    final MergeableStringList mergingPreset = (MergeableStringList) presetValue;
                    if (mergingPreset.doMerge && !mergingPreset.isEmpty()) {
                        final MergeableStringList current = (MergeableStringList) this.getClass().getDeclaredField(f.getName()).get(this);
                        current.items.addAll(mergingPreset.items);
                        skipSettingValue = true;
                    }
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
        } catch (final IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    @NotNull
    public String formatRulesVisually(){
        return formatRulesVisually(null);
    }
    @NotNull
    public String formatRulesVisually(final List<String> excludedKeys){
        final SortedMap<String, String> values = new TreeMap<>();
        final StringBuilder sb = new StringBuilder();

        if (excludedKeys == null || !excludedKeys.contains("id")) {
            sb.append("id: ");
            sb.append(getRuleName());
            sb.append("\n");
        }

        try {
            for(final Field f : this.getClass().getDeclaredFields()) {
                if (Modifier.isPrivate(f.getModifiers())) continue;
                if (f.get(this) == null) continue;
                if (f.getName().equals("ruleSourceNames")) continue;
                if (excludedKeys != null && excludedKeys.contains(f.getName())) continue;
                final Object value = f.get(this);
                if (value.toString().equalsIgnoreCase("NOT_SPECIFIED")) continue;
                if (value.toString().equalsIgnoreCase("{}")) continue;
                if (value.toString().equalsIgnoreCase("[]")) continue;
                if (value.toString().equalsIgnoreCase("0") &&
                        f.getName().equals("rulePriority")) continue;
                if (value.toString().equalsIgnoreCase("0.0")) continue;
                if (value.toString().equalsIgnoreCase("false") &&
                        !f.getName().equals("ruleIsEnabled")) continue;
                if (value.toString().equalsIgnoreCase("NONE")) continue;
                if (value instanceof CachedModalList<?>) {
                    final CachedModalList<?> cml = (CachedModalList<?>) value;
                    if (cml.isEmpty() && !cml.allowAll && !cml.excludeAll) continue;
                }
                final String showValue = "&b" + f.getName() + "&r, value: &b" + value + "&r";
                values.put(f.getName(), showValue);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }

        for (final String s : values.values()){
            sb.append(MessageUtils.colorizeAll(s));
            sb.append("\n");
        }

        sb.setLength(sb.length() - 1); // remove trailing newline
        return sb.toString();
    }
}

