package me.lokka30.levelledmobs.rules;

import java.util.List;
import java.util.Map;

public class RulesInfo {
    public boolean enabled;
    public List<String> entities;
    public ListModeEnum allowedWorlds_Mode;
    public List<String> allowedWorlds_List;
    public int levelMin;
    public int levelMax;
    public int maxRandomVariance;
    public int levellingStrategy;
    public boolean levelPassiveMobs;
    public boolean levelInheritance;
    public boolean noLevel_Nametagged;
    public boolean noLevel_Tamed;
    public int creeperMaxDamageRadius;
    public Map<String, Integer> entitytypeLevelOverride_Min;
    public Map<String, Integer> entitytypeLevelOverride_Max;
    public List<String> overridenEntities;
    public List<String> noDropMultiplerEntities;
    public AttributesMultipliers multipliers;
    public ListModeEnum allowedEntities_Mode;
    public List<String> allowedEntities_List;
}
