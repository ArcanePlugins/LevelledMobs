package me.lokka30.levelledmobs.rules;

import me.lokka30.levelledmobs.misc.ModalList;

import java.util.List;
import java.util.Map;

/**
 * TODO Describe...
 *
 * @author stumper66
 * @contributors lokka30
 */
public class RulesInfo {
    public boolean enabled;
    public List<String> entities;
    public ModalList allowedWorlds;
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
    public ModalList allowedEntities;
}
