package me.lokka30.levelledmobs.rules;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.CustomUniversalGroups;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class RulesManager {
    public RulesManager(final LevelledMobs main) {
        this.main = main;
        this.rulesInEffect = new LinkedList<>();
        this.groupCache = new LinkedList<>();
    }

    private final LevelledMobs main;
    @NotNull
    public final List<RuleInfo> rulesInEffect;
    @NotNull
    private List<CustomUniversalGroups> groupCache;
    private boolean groupCacheNeedsBuilding;

    private int getMobLevelAndClearCache(final LivingEntity livingEntity){
        final boolean isLevelled = main.levelInterface.isLevelled(livingEntity);
        int mobLevel = -1;
        if (isLevelled) mobLevel = main.levelInterface.getLevelOfMob(livingEntity);
        this.groupCache.clear();
        this.groupCacheNeedsBuilding = true;

        return mobLevel;
    }

    public MobCustomNameStatusEnum getRule_MobCustomNameStatus(final LivingEntity livingEntity, final boolean skipGroupCheck){
        MobCustomNameStatusEnum result = MobCustomNameStatusEnum.NOT_SPECIFIED;

        for (final RuleInfo ruleInfo : getApplicableRules(livingEntity, skipGroupCheck))
            if (ruleInfo.conditions_MobCustomnameStatus != MobCustomNameStatusEnum.NOT_SPECIFIED) result = ruleInfo.conditions_MobCustomnameStatus;

        return result;
    }

    public MobTamedStatusEnum getRule_MobTamedStatus(final LivingEntity livingEntity, final boolean skipGroupCheck){
        MobTamedStatusEnum result = MobTamedStatusEnum.NOT_SPECIFIED;

        for (final RuleInfo ruleInfo : getApplicableRules(livingEntity, skipGroupCheck))
            if (ruleInfo.conditions_MobTamedStatus != MobTamedStatusEnum.NOT_SPECIFIED) result = ruleInfo.conditions_MobTamedStatus;

        return result;
    }

    public int getRule_MobMinLevel(final LivingEntity livingEntity){
        int minLevel = -1;
        for (final RuleInfo ruleInfo : getApplicableRules(livingEntity, false)) {
            if (ruleInfo.restrictions_MinLevel != null) minLevel = ruleInfo.restrictions_MinLevel;
        }

        return minLevel;
    }

    public int getRule_MobMaxLevel(final LivingEntity livingEntity){
        int maxLevel = -1;
        for (final RuleInfo ruleInfo : getApplicableRules(livingEntity, false)) {
            if (ruleInfo.restrictions_MaxLevel != null) maxLevel = ruleInfo.restrictions_MaxLevel;
        }

        return maxLevel;
    }

    public String getRule_Nametag(final LivingEntity livingEntity){
        String nametag = "";
        for (final RuleInfo ruleInfo : getApplicableRules(livingEntity, false)) {
            if (!Utils.isNullOrEmpty(ruleInfo.nametag)) nametag = ruleInfo.nametag;
        }

        return nametag;
    }

    public String getRule_Nametag_CreatureDeath(final LivingEntity livingEntity){
        String nametag = "";
        for (final RuleInfo ruleInfo : getApplicableRules(livingEntity, false)) {
            if (!Utils.isNullOrEmpty(ruleInfo.nametag_CreateDeath)) nametag = ruleInfo.nametag_CreateDeath;
        }

        return nametag;
    }

    private List<RuleInfo> getApplicableRules(final LivingEntity livingEntity, final boolean skipGroupCheck){
        final int mobLevel = getMobLevelAndClearCache(livingEntity);
        List<RuleInfo> rules = new LinkedList<>();

        for (final RuleInfo ruleInfo : rulesInEffect) {
            if (!ruleInfo.ruleIsEnabled) continue;
            final boolean ruleIsApplicable = isRuleApplicable(livingEntity, ruleInfo, mobLevel, skipGroupCheck);
            //Utils.logger.info(livingEntity.getType().name() + ", mobTamedStatus, rule id: " + ruleInfo.getInternalId() + ", is applicable: " + ruleIsApplicable);

            if (ruleIsApplicable) rules.add(ruleInfo);
        }

        return rules;
    }

    private boolean isRuleApplicable(final LivingEntity le, final RuleInfo ri, final int mobLevel, final boolean skipGroupCheck){
        final boolean isLevelled = mobLevel > -1;

        if (!skipGroupCheck && !isEntityAllowedWithinGroup(le, ri.conditions_Entities, isLevelled)) {
            Utils.logger.info(le.getType().name() + ", rule id: " + ri.getInternalId() + ", isEntityAllowedWithinGroup returned false");
            return false;
        }
//        if (!skipGroupCheck && isEntityAllowedWithinGroup(le, ri.conditions_ExcludeEntities, isLevelled)) {
//            Utils.logger.info(le.getType().name() + ", rule id: " + ri.getInternalId() + ", isEntityAllowedWithinGroup (exclude) returned false");
//            return false;
//        }
        if (ri.conditions_MinLevel != null && (!isLevelled || ri.conditions_MinLevel < mobLevel)) {
            Utils.logger.info(le.getType().name() + ", rule id: " + ri.getInternalId() + ", didn't meet minlevel criteria of " + ri.conditions_MinLevel);
            return false;
        }
        if (ri.conditions_MaxLevel != null && (!isLevelled || ri.conditions_MaxLevel > mobLevel)) {
            Utils.logger.info(le.getType().name() + ", rule id: " + ri.getInternalId() + ", didn't meet maxlevel criteria of " +  + ri.conditions_MaxLevel);
            return false;
        }
        if (!ri.worlds_List.isEmpty() && !isEnabledInList(le.getWorld().getName(), ri.worlds_List, ri.worlds_Mode)) {
            Utils.logger.info(le.getType().name() + ", rule id: " + ri.getInternalId() + ", denied from world list");
            return false;
        }

        // TODO: add more rule constraints like entity overrides, etc

        return true;
    }

    private boolean isEntityAllowedWithinGroup(final LivingEntity le, @NotNull final Map<String, CustomUniversalGroups> list, final boolean isLevelled){
        if (list.isEmpty()) return true;

        for (final String entity : list.keySet()){
            final CustomUniversalGroups group = list.get(entity);
            if (group == CustomUniversalGroups.NOT_APPLICABLE && entity.equalsIgnoreCase(le.getType().name()))
                return true;
            else if (group != CustomUniversalGroups.NOT_APPLICABLE){
                if (this.groupCacheNeedsBuilding){
                    this.groupCache = main.companion.getApllicableGroupsForMob(le, isLevelled, false);
                    this.groupCacheNeedsBuilding = false;
                }

                 if (this.groupCache.contains(group)) return true;
            }
        }

        return false;
    }

    private static boolean isEnabledInList(final String item, final List<String> list, String listMode) {
        if (listMode == null) listMode = "";

        switch (listMode.toUpperCase()) {
            case "ALL":
                return true;
            case "WHITELIST":
            case "":
                if ("".equals(listMode)) Utils.logger.warning("No list mode was specified");
                return list.contains(item);
            case "BLACKLIST":
                return !list.contains(item);
            default:
                Utils.logger.warning("Invalid list mode: " + listMode);
                return false;
        }
    }
}
