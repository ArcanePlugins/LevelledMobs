package me.lokka30.levelledmobs.rules;

import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.misc.CachedModalList;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.levelledmobs.rules.strategies.LevellingStrategy;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.*;

public class RuleInfo {
    public RuleInfo(final String id){
        this.ruleName = id;

        this.ruleIsEnabled = true;
        this.levellingStrategies = new LinkedList<>();
        this.entityNameOverrides = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.ruleSourceNames = new TreeMap<>();
        this.conditions_MobCustomnameStatus = MobCustomNameStatusEnum.NOT_SPECIFIED;
        this.conditions_MobTamedStatus = MobTamedStatusEnum.NOT_SPECIFIED;
    }

    private String ruleName;
    public boolean ruleIsEnabled;
    public Boolean CreatureNametagAlwaysVisible;
    public Boolean babyMobsInheritAdultSetting;
    public Boolean mobLevelInheritance;
    public Boolean useCustomItemDropsForMobs;
    public Integer maxRandomVariance;
    public Integer creeperMaxDamageRadius;
    public Integer conditions_MinLevel;
    public Integer conditions_MaxLevel;
    public Integer restrictions_MinLevel;
    public Integer restrictions_MaxLevel;
    public Integer lowerMobLevelBiasFactor;
    public Integer conditions_ApplyAboveY;
    public Integer conditions_ApplyBelowY;
    public double random_BiasFactor;
    public Double conditions_Chance;
    public String nametag;
    public String nametag_CreatureDeath;
    public String presetName;
    public MobCustomNameStatusEnum conditions_MobCustomnameStatus;
    public MobTamedStatusEnum conditions_MobTamedStatus;
    @NotNull
    public List<LevellingStrategy> levellingStrategies;
    @NotNull
    public Map<String, List<String>> entityNameOverrides;
    @NotNull
    public final Map<String, String> ruleSourceNames;
    public List<TieredColoringInfo> tieredColoringInfos;
    public List<ExternalCompatibilityManager.ExternalCompatibility> enabledExtCompats;
    public CachedModalList<String> allowedEntities;
    public CachedModalList<String> worlds;
    public CachedModalList<String> conditions_Entities;
    public CachedModalList<String> conditions_Biomes;
    public CachedModalList<String> conditions_ApplyPlugins;
    public CachedModalList<String> conditions_CustomNames;
    public CachedModalList<String> conditions_NoDropEntities;
    public CachedModalList<String> conditions_WGRegions;
    public CachedModalList<CreatureSpawnEvent.SpawnReason> conditions_SpawnReasons;
    public FineTuningAttributes defaultFineTuning;
    public Map<String, FineTuningAttributes> fineTuning;

    public String getRuleName(){
        return this.ruleName;
    }

    public void setRuleName(final String name){
        this.ruleName = name;
    }

    public void mergePresetRules(final RuleInfo preset){
        if (preset == null) return;

        try {
            for (final Field f : preset.getClass().getDeclaredFields()) {
                if (!Modifier.isPublic(f.getModifiers())) continue;
                if (f.getName().equals("ruleIsEnabled")) continue;
                if (f.getName().equals("presetName")) continue;
                if (f.getName().equals("ruleSourceNames")) continue;
                if (f.get(preset) == null) continue;

                // skip default values such as false, 0, 0.0
                if (f.get(preset) instanceof Boolean && !((Boolean) f.get(preset))) continue;
                if (f.get(preset) instanceof Integer && ((Integer) f.get(preset)) == 0) continue;
                if (f.get(preset) instanceof Double && ((Double) f.get(preset)) == 0.0) continue;

                this.getClass().getDeclaredField(f.getName()).set(this, f.get(preset));
                this.ruleSourceNames.put(f.getName(), preset.ruleName);
            }
        }
        catch (IllegalAccessException | NoSuchFieldException ignored){}
    }
}

