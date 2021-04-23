package me.lokka30.levelledmobs.rules;

import me.lokka30.levelledmobs.rules.strategies.LevellingStrategy;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class RuleParsingInfo{
    public RuleParsingInfo(){
        this.calculation_CustomVariables = new TreeMap<>();
        this.worlds_List = new LinkedList<>();
        this.calculation_Entities = new LinkedList<>();
        this.minLevel = 1;
        this.maxLevel = 10;
        this.ruleIsEnabled = true;
        this.presetType = PresetType.NONE;
        this.levellingStrategies = new LinkedList<>();
    }

    public int minLevel;
    public int maxLevel;
    public int maxRandomVariance;
    public double biasFactor;
    public boolean ruleIsEnabled;
    public boolean isPreset;
    public String presetName;
    public String worlds_UsePreset;
    public String worlds_Mode;
    public String strategies_Preset;
    public String calculation_Formula;
    public String conditions_Preset;
    public PresetType presetType;
    @NotNull
    public List<LevellingStrategy> levellingStrategies;
    @NotNull
    public Map<String, String> calculation_CustomVariables;
    @NotNull
    public List<String> calculation_Entities;
    @NotNull
    public List<String> worlds_List;
}

