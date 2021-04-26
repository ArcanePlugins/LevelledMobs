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
    }

    private final LevelledMobs main;
    @NotNull
    public final List<RuleInfo> rulesInEffect;

    public MobCustomNameStatusEnum mobCustomNameStatus(final LivingEntity livingEntity){
        MobCustomNameStatusEnum result = MobCustomNameStatusEnum.NOT_SPECIFIED;
        final boolean isLevelled = main.levelInterface.isLevelled(livingEntity);
        int mobLevel = -1;
        if (isLevelled) mobLevel = main.levelInterface.getLevelOfMob(livingEntity);

        for (final RuleInfo ruleInfo : rulesInEffect) {
            if (!isRuleApplicable(livingEntity, ruleInfo, mobLevel)) continue;
            if (ruleInfo.conditions_MobCustomnameStatus != MobCustomNameStatusEnum.NOT_SPECIFIED) result = ruleInfo.conditions_MobCustomnameStatus;
        }

        return result;
    }

    private boolean isRuleApplicable(final LivingEntity le, final RuleInfo ri, final int mobLevel){
        final boolean isLevelled = mobLevel > -1;

        if (!isEntityAllowedWithinGroup(le, ri.conditions_Entities, isLevelled)) return false;
        if (ri.minLevel != null && (!isLevelled || ri.minLevel < mobLevel)) return false;
        if (ri.maxLevel != null && (!isLevelled || ri.maxLevel > mobLevel)) return false;
        if (!isEnabledInList(le.getWorld().getName(), ri.worlds_List, ri.worlds_Mode)) return false;

        // TODO: add more rule constraints like entity overrides, etc

        return true;
    }

    private boolean isEntityAllowedWithinGroup(final LivingEntity le, final Map<String, CustomUniversalGroups> list, final boolean isLevelled){
        if (list.isEmpty()) return true;

        for (final String entity : list.keySet()){
            final CustomUniversalGroups group = list.get(entity);
            if (group == CustomUniversalGroups.NOT_APPLICABLE && entity.equalsIgnoreCase(le.getType().name()))
                return true;
            else if (group != CustomUniversalGroups.NOT_APPLICABLE && main.companion.getApllicableGroupsForMob(le, isLevelled).contains(group))
                return true;
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
