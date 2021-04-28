package me.lokka30.levelledmobs.rules;

import me.lokka30.levelledmobs.misc.CachedModalList;
import me.lokka30.levelledmobs.misc.CustomUniversalGroups;
import me.lokka30.levelledmobs.rules.strategies.LevellingStrategy;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class RuleInfo {
    public RuleInfo(){
        this.internalId = UUID.randomUUID().toString().substring(24);
        this.ruleIsEnabled = true;
        this.presetType = PresetType.NONE;
        this.worlds = new CachedModalList();
        this.conditions_Biomes = new CachedModalList();
        this.conditions_Entities = new CachedModalList();
        this.levellingStrategies = new LinkedList<>();
        this.calculation_CustomVariables = new TreeMap<>();
        this.conditions_MobCustomnameStatus = MobCustomNameStatusEnum.NOT_SPECIFIED;
        this.conditions_MobTamedStatus = MobTamedStatusEnum.NOT_SPECIFIED;
    }

    private final String internalId;
    public boolean ruleIsEnabled;
    public Boolean CreatureNametagAlwaysVisible;
    public int maxRandomVariance;
    public Integer conditions_MinLevel;
    public Integer conditions_MaxLevel;
    public Integer restrictions_MinLevel;
    public Integer restrictions_MaxLevel;
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
    @NotNull
    public Map<String, String> calculation_CustomVariables;
    @NotNull
    public CachedModalList worlds;
    @NotNull
    public CachedModalList conditions_Entities;
    @NotNull
    public CachedModalList conditions_Biomes;

    public String getInternalId(){
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
            this.worlds = preset.worlds;
        }
        else if (preset.presetType == PresetType.STRATEGIES){
            this.levellingStrategies.addAll(preset.levellingStrategies);
        }
    }
}

