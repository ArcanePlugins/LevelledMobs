package me.lokka30.levelledmobs.rules;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.CachedModalList;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.levelledmobs.rules.strategies.LevellingStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

public class RulesManager {
    public RulesManager(final LevelledMobs main) {
        //this.main = main;
        this.rulesInEffect = new LinkedList<>();
    }

    //private final LevelledMobs main;
    @NotNull
    public final List<RuleInfo> rulesInEffect;

    public boolean getRule_IsMobInAnyAllowedLists(final LivingEntityWrapper lmEntity){
        // if no conditions mentioned the entity type or group then return false
        return !lmEntity.getApplicableRules().isEmpty();
    }

    public boolean getRule_IsMobAllowedInEntityOverride(final LivingEntityWrapper lmEntity){
        // check if it should be denied thru the entity override list
        boolean babyMobsInheritAdultSetting = true; // default
        CachedModalList allowedEntitiesList = null;
        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.allowedEntities != null)
                allowedEntitiesList = ruleInfo.allowedEntities;
            if (ruleInfo.babyMobsInheritAdultSetting != null)
                babyMobsInheritAdultSetting = ruleInfo.babyMobsInheritAdultSetting;
        }

        return (
                allowedEntitiesList == null ||
                !babyMobsInheritAdultSetting && lmEntity.isBabyMob() && allowedEntitiesList.isLivingEntityInList(lmEntity, true)) ||
                allowedEntitiesList.isLivingEntityInList(lmEntity, babyMobsInheritAdultSetting);
    }

    public int getRule_CreeperMaxBlastRadius(final LivingEntityWrapper lmEntity){
        int maxBlast = 5;
        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.creeperMaxDamageRadius != null) maxBlast = ruleInfo.creeperMaxDamageRadius;
        }

        return maxBlast;
    }

    @Nullable
    public LevellingStrategy getRule_LevellingStrategy(final LivingEntityWrapper lmEntity){
        LevellingStrategy levellingStrategy = null;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (!ruleInfo.levellingStrategies.isEmpty())
                levellingStrategy = ruleInfo.levellingStrategies.get(0);
            // TODO: allow multiple levelling strategies
        }

        return levellingStrategy;
    }

    public boolean getRule_MobLevelInheritance(final LivingEntityWrapper lmEntity){
        boolean result = true;
        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.mobLevelInheritance != null) result = ruleInfo.mobLevelInheritance;
        }

        return result;
    }

    public MobCustomNameStatusEnum getRule_MobCustomNameStatus(final LivingEntityWrapper lmEntity){
        MobCustomNameStatusEnum result = MobCustomNameStatusEnum.NOT_SPECIFIED;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules())
            if (ruleInfo.conditions_MobCustomnameStatus != MobCustomNameStatusEnum.NOT_SPECIFIED) result = ruleInfo.conditions_MobCustomnameStatus;

        return result;
    }

    public MobTamedStatusEnum getRule_MobTamedStatus(final LivingEntityWrapper lmEntity){
        MobTamedStatusEnum result = MobTamedStatusEnum.NOT_SPECIFIED;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules())
            if (ruleInfo.conditions_MobTamedStatus != MobTamedStatusEnum.NOT_SPECIFIED) result = ruleInfo.conditions_MobTamedStatus;

        return result;
    }

    public int getRule_MobMinLevel(final LivingEntityWrapper lmEntity){
        int minLevel = -1;
        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.restrictions_MinLevel != null) minLevel = ruleInfo.restrictions_MinLevel;
        }

        return minLevel;
    }

    public int getRule_MobMaxLevel(final LivingEntityWrapper lmEntity){
        int maxLevel = -1;
        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (ruleInfo.restrictions_MaxLevel != null) maxLevel = ruleInfo.restrictions_MaxLevel;
        }

        return maxLevel;
    }

    public String getRule_Nametag(final LivingEntityWrapper lmEntity){
        String nametag = "";
        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (!Utils.isNullOrEmpty(ruleInfo.nametag)) nametag = ruleInfo.nametag;
        }

        return nametag;
    }

    public String getRule_Nametag_CreatureDeath(final LivingEntityWrapper lmEntity){
        String nametag = "";
        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()) {
            if (!Utils.isNullOrEmpty(ruleInfo.nametag_CreatureDeath)) nametag = ruleInfo.nametag_CreatureDeath;
        }

        return nametag;
    }

    public boolean getRule_CreatureNametagAlwaysVisible(final LivingEntityWrapper lmEntity){
        boolean creatureNametagAlwaysVisible = false;

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.CreatureNametagAlwaysVisible != null)
                creatureNametagAlwaysVisible = ruleInfo.CreatureNametagAlwaysVisible;
        }

        return creatureNametagAlwaysVisible;
    }

    public String getRule_EntityOverriddenName(final LivingEntityWrapper lmEntity){
        String overridenName = null;
        final String checkName = lmEntity.isBabyMob() ?
                "baby_" + lmEntity.getTypeName() : lmEntity.getTypeName();

        for (final RuleInfo ruleInfo : lmEntity.getApplicableRules()){
            if (ruleInfo.entityNameOverrides.containsKey(checkName))
                overridenName = ruleInfo.entityNameOverrides.get(checkName);
        }

        return overridenName;
    }

    public List<RuleInfo> getApplicableRules(final LivingEntityWrapper lmEntity){
        List<RuleInfo> rules = new LinkedList<>();

        for (final RuleInfo ruleInfo : rulesInEffect) {
            if (!ruleInfo.ruleIsEnabled) continue;
            final boolean ruleIsApplicable = isRuleApplicable(lmEntity, ruleInfo);
            //Utils.logger.info(livingEntity.getType().name() + ", mobTamedStatus, rule id: " + ruleInfo.getInternalId() + ", is applicable: " + ruleIsApplicable);

            if (ruleIsApplicable) rules.add(ruleInfo);
        }

        return rules;
    }

    private boolean isRuleApplicable(final LivingEntityWrapper lmEntity, final RuleInfo ri){

        if (ri.conditions_Entities != null && !ri.conditions_Entities.isLivingEntityInList(lmEntity)) {
            //Utils.logger.info(lmEntity.getTypeName() + ", rule id: " + ri.getInternalId() + ", isLivingEntityInList returned false");
            return false;
        }
        if (ri.conditions_MinLevel != null && (!lmEntity.isLevelled() || ri.conditions_MinLevel < lmEntity.getMobLevel())) {
            Utils.logger.info(lmEntity.getTypeName() + ", rule id: " + ri.getInternalId() + ", didn't meet minlevel criteria of " + ri.conditions_MinLevel);
            return false;
        }
        if (ri.conditions_MaxLevel != null && (!lmEntity.isLevelled() || ri.conditions_MaxLevel > lmEntity.getMobLevel())) {
            Utils.logger.info(lmEntity.getTypeName() + ", rule id: " + ri.getInternalId() + ", didn't meet maxlevel criteria of " +  + ri.conditions_MaxLevel);
            return false;
        }
        if (ri.worlds != null && !ri.worlds.isEnabledInList(lmEntity.getWorldName())) {
            Utils.logger.info(lmEntity.getTypeName() + ", rule id: " + ri.getInternalId() + ", denied from world list");
            return false;
        }

        // TODO: add more rule constraints like entity overrides, etc

        return true;
    }
}
