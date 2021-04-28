package me.lokka30.levelledmobs.rules;

import me.lokka30.levelledmobs.misc.CachedModalList;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.levelledmobs.rules.strategies.LevellingStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class RuleInfo {
    public RuleInfo(final String id){
        this.internalId = id;

        this.ruleIsEnabled = true;
        this.presetType = PresetType.NONE;
        this.levellingStrategies = new LinkedList<>();
        this.calculation_CustomVariables = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.entityNameOverrides = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.conditions_MobCustomnameStatus = MobCustomNameStatusEnum.NOT_SPECIFIED;
        this.conditions_MobTamedStatus = MobTamedStatusEnum.NOT_SPECIFIED;
    }

    private final String internalId;
    public boolean ruleIsEnabled;
    public Boolean CreatureNametagAlwaysVisible;
    public Boolean babyMobsInheritAdultSetting;
    public Boolean mobLevelInheritance;
    public int maxRandomVariance;
    public Integer creeperMaxDamageRadius;
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
    public Map<String, String> entityNameOverrides;
    public CachedModalList allowedEntities;
    public CachedModalList worlds;
    public CachedModalList conditions_Entities;
    public CachedModalList conditions_Biomes;

    public String getInternalId(){
        return this.internalId;
    }

    public void mergePresetRules(RuleInfo preset){
        if (preset == null) return;

        if (preset.presetType == PresetType.CONDITIONS){
            this.conditions_Biomes = preset.conditions_Biomes;
            this.conditions_Entities = preset.conditions_Entities.cloneItem();
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

