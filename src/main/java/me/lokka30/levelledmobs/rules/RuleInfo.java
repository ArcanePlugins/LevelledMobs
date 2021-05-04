package me.lokka30.levelledmobs.rules;

import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.misc.CachedModalList;
import me.lokka30.levelledmobs.rules.strategies.LevellingStrategy;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class RuleInfo {
    public RuleInfo(final String id){
        this.internalId = id;

        this.ruleIsEnabled = true;
        this.presetType = PresetType.NONE;
        this.levellingStrategies = new LinkedList<>();
        this.entityNameOverrides = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.conditions_MobCustomnameStatus = MobCustomNameStatusEnum.NOT_SPECIFIED;
        this.conditions_MobTamedStatus = MobTamedStatusEnum.NOT_SPECIFIED;
    }

    private final String internalId;
    public boolean ruleIsEnabled;
    public Boolean CreatureNametagAlwaysVisible;
    public Boolean babyMobsInheritAdultSetting;
    public Boolean mobLevelInheritance;
    public Boolean useCustomItemDropsForMobs;
    public int maxRandomVariance;
    public Integer creeperMaxDamageRadius;
    public Integer conditions_MinLevel;
    public Integer conditions_MaxLevel;
    public Integer restrictions_MinLevel;
    public Integer restrictions_MaxLevel;
    public Integer lowerMobLevelBiasFactor;
    public double random_BiasFactor;
    public Double conditions_Chance;
    public String nametag;
    public String nametag_CreatureDeath;
    public String presetName;
    public String calculation_Formula;
    public MobCustomNameStatusEnum conditions_MobCustomnameStatus;
    public MobTamedStatusEnum conditions_MobTamedStatus;
    public PresetType presetType;
    @NotNull
    public List<LevellingStrategy> levellingStrategies;
    public Map<String, String> calculation_CustomVariables;
    @NotNull
    public Map<String, List<String>> entityNameOverrides;
    public List<TieredColoringInfo> tieredColoringInfos;
    public List<ExternalCompatibilityManager.ExternalCompatibility> enabledExtCompats;
    public CachedModalList<String> allowedEntities;
    public CachedModalList<String> worlds;
    public CachedModalList<String> conditions_Entities;
    public CachedModalList<String> conditions_Biomes;
    public CachedModalList<String> conditions_ApplyPlugins;
    public CachedModalList<String> conditions_CustomNames;
    public CachedModalList<String> conditions_NoDropEntities;
    public CachedModalList<CreatureSpawnEvent.SpawnReason> conditions_SpawnReasons;
    public FineTuningAttributes defaultFineTuning;
    public Map<String, FineTuningAttributes> fineTuning;

    public String getInternalId(){
        return this.internalId;
    }

    public void mergePresetRules(RuleInfo preset){
        if (preset == null) return;

        if (preset.presetType == PresetType.CONDITIONS){
            if (preset.conditions_Biomes != null)
                this.conditions_Biomes = (CachedModalList<String>) preset.conditions_Biomes.clone();
            if (preset.conditions_Entities != null)
                this.conditions_Entities = (CachedModalList<String>) preset.conditions_Entities.clone();
            this.conditions_Chance = preset.conditions_Chance;
        }
        else if (preset.presetType == PresetType.WORLDS){
            this.worlds = (CachedModalList<String>) preset.worlds.clone();
        }
        else if (preset.presetType == PresetType.STRATEGIES){
            this.levellingStrategies.addAll(preset.levellingStrategies);
        }
    }
}

