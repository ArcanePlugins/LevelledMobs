package me.lokka30.levelledmobs.rules;

import me.lokka30.levelledmobs.misc.CustomUniversalGroups;
import me.lokka30.levelledmobs.rules.strategies.LevellingStrategy;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class RuleInfo {
    public RuleInfo(){
        this.internalId = UUID.randomUUID();
        this.ruleIsEnabled = true;
        this.presetType = PresetType.NONE;
        this.worlds_List = new LinkedList<>();
        this.conditions_Entities = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.conditions_ExcludeEntities = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.levellingStrategies = new LinkedList<>();
        this.calculation_CustomVariables = new TreeMap<>();
        this.conditions_Biomes = new LinkedList<>();
        this.conditions_MobCustomnameStatus = MobCustomNameStatusEnum.NOT_SPECIFIED;
        this.conditions_MobTamedStatus = MobTamedStatusEnum.NOT_SPECIFIED;
    }

    private final UUID internalId;
    public boolean ruleIsEnabled;
    public MobCustomNameStatusEnum conditions_MobCustomnameStatus;
    public MobTamedStatusEnum conditions_MobTamedStatus;
    public Integer conditions_MinLevel;
    public Integer conditions_MaxLevel;
    public Integer restrictions_MinLevel;
    public Integer restrictions_MaxLevel;
    public int maxRandomVariance;
    public double random_BiasFactor;
    public Double conditions_Chance;
    public String nametag;
    public String nametag_CreateDeath;
    public String presetName;
    public String worlds_Mode;
    public String calculation_Formula;
    public PresetType presetType;
    @NotNull
    public List<LevellingStrategy> levellingStrategies;
    @NotNull
    public Map<String, String> calculation_CustomVariables;
    @NotNull
    public Map<String, CustomUniversalGroups> conditions_Entities;
    @NotNull
    public Map<String, CustomUniversalGroups> conditions_ExcludeEntities;
    @NotNull
    public List<String> worlds_List;
    @NotNull
    public List<String> conditions_Biomes;

    public UUID getInternalId(){
        return this.internalId;
    }

    public void mergePresetRules(RuleInfo preset){
        if (preset == null) return;

        if (preset.presetType == PresetType.CONDITIONS){
            this.conditions_Biomes = preset.conditions_Biomes;
            this.conditions_Entities = preset.conditions_Entities;
            this.conditions_Chance = preset.conditions_Chance;
        }
        else if (preset.presetType == PresetType.WORLDS){
            this.worlds_List = preset.worlds_List;
            this.worlds_Mode = preset.worlds_Mode;
        }
        else if (preset.presetType == PresetType.STRATEGIES){
            this.levellingStrategies.addAll(preset.levellingStrategies);
        }
    }
}

