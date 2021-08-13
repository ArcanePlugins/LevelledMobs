/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.rules;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.rules.strategies.RandomLevellingStrategy;
import me.lokka30.levelledmobs.rules.strategies.SpawnDistanceStrategy;
import me.lokka30.levelledmobs.rules.strategies.YDistanceStrategy;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Holds function to generate metrics to be sent to bstats
 *
 * @author stumper66
 * @since 3.1.0
 */
public class MetricsInfo {
    public MetricsInfo(final LevelledMobs main){
        this.main = main;
    }

    private final LevelledMobs main;

    private String convertBooleanToString(final boolean result){
        return result ?
                "Yes" : "No";
    }

    private boolean isCustomDropsEnabed(){
        for (final List<RuleInfo> rules : main.rulesManager.rulesInEffect.values()){
            for (final RuleInfo ruleInfo : rules) {
                if (ruleInfo.customDrops_UseForMobs != null && ruleInfo.customDrops_UseForMobs)
                    return true;
            }
        }

        return false;
    }

    public String getUsesCustomDrops(){
        return convertBooleanToString(isCustomDropsEnabed());
    }

    public String getUsesHealthIndicator(){
        final boolean usesHealthIndicator =
                main.rulesParsingManager.defaultRule.healthIndicator != null &&
                        main.rulesParsingManager.defaultRule.nametag != null &&
                        main.rulesParsingManager.defaultRule.nametag.toLowerCase().contains("%health-indicator%");

        return convertBooleanToString(usesHealthIndicator);
    }

    public String getUsesPlayerLevelling(){
        final boolean result =
                (main.rulesParsingManager.defaultRule.playerLevellingOptions != null &&
                        main.rulesParsingManager.defaultRule.playerLevellingOptions.enabled != null &&
                        main.rulesParsingManager.defaultRule.playerLevellingOptions.enabled);

        return convertBooleanToString(result);
    }

    public String getMaxLevelRange(){
        // 1-10, 11-24, 25-50, 51-100, 101-499, 500+
        final int maxLevel = main.rulesParsingManager.defaultRule.restrictions_MaxLevel == null ?
                1 : main.rulesParsingManager.defaultRule.restrictions_MaxLevel;

        if (maxLevel >= 500)
            return "500+";
        else if (maxLevel > 100)
            return "101-499";
        else if (maxLevel > 50)
            return "51-100";
        else if (maxLevel > 24)
            return "25-50";
        else if (maxLevel > 10)
            return "11-24";
        else
            return "1-10";
    }

    public String getCustomRulesUsed(){
        // 0, 1-2, 3-4, 5+
        int rulesEnabledCount = 0;
        for (final RuleInfo ruleInfo : main.rulesParsingManager.customRules) {
            if (ruleInfo.ruleIsEnabled) rulesEnabledCount++;
        }

        if (rulesEnabledCount > 4)
            return  "5+";
        else if (rulesEnabledCount > 2)
            return  "3-4";
        else if (rulesEnabledCount > 0)
            return  "1-2";
        else
            return "0";
    }

    public String getLevellingStrategy(){
        // Random, Weighted Random, Spawn Distance, Blended, Y-Levelling
        final RuleInfo defaultRule = main.rulesParsingManager.defaultRule;

        if (defaultRule.levellingStrategy != null){
            if (defaultRule.levellingStrategy instanceof SpawnDistanceStrategy) {
                final SpawnDistanceStrategy sds = (SpawnDistanceStrategy) defaultRule.levellingStrategy;
                if (sds.blendedLevellingEnabled == null || !sds.blendedLevellingEnabled)
                    return "Spawn Distance";
                else
                    return "Blended";
            } else if (defaultRule.levellingStrategy instanceof YDistanceStrategy)
                return "Y-Levelling";
            else if (defaultRule.levellingStrategy instanceof RandomLevellingStrategy) {
                final RandomLevellingStrategy random = (RandomLevellingStrategy) defaultRule.levellingStrategy;
                if (random.weightedRandom != null && !random.weightedRandom.isEmpty())
                    return "Weighted Random";
            }
        }

        return "Random";
    }

    public String usesAutoUpdateChecker(){
        return convertBooleanToString(main.helperSettings.getBoolean(main.settingsCfg, "use-update-checker", true));
    }

    public String levelMobsUponSpawn(){
        return convertBooleanToString(main.helperSettings.getBoolean(main.settingsCfg, "level-mobs-upon-spawn", true));
    }

    public String checkMobsOnChunkLoad(){
        return convertBooleanToString(main.helperSettings.getBoolean(main.settingsCfg, "ensure-mobs-are-levelled-on-chunk-load", true));
    }

    public String customEntityNamesCount(){
        // 0, 1-3, 4-8, 9-12, 13+
        int count = 0;
        if (main.rulesParsingManager.defaultRule.entityNameOverrides != null)
            count += main.rulesParsingManager.defaultRule.entityNameOverrides.size();
        if (main.rulesParsingManager.defaultRule.entityNameOverrides_Level != null)
            count += main.rulesParsingManager.defaultRule.entityNameOverrides_Level.size();

        if (count > 12)
            return "13+";
        else if (count > 8)
            return "9-12";
        else if (count > 3)
            return "4-8";
        else if (count > 0)
            return "1-3";
        else
            return "0";
    }

    public String usesNbtData(){
        if (!ExternalCompatibilityManager.hasNBTAPI_Installed())
            return "No";

        for (final RuleInfo ruleInfo : main.rulesParsingManager.getAllRules()){
            if (!ruleInfo.ruleIsEnabled) continue;

            if (ruleInfo.mobNBT_Data != null && !ruleInfo.mobNBT_Data.isEmpty())
                return "Yes";
        }

        if (isCustomDropsEnabed() && main.customDropsHandler.customDropsParser.dropsUtilizeNBTAPI)
            return "Yes";

        return "No";
    }

    public Map<String, Integer> enabledCompats(){
        final Map<String, Integer> results = new TreeMap<>();

        for (final ExternalCompatibilityManager.ExternalCompatibility compat : ExternalCompatibilityManager.ExternalCompatibility.values()) {
            if (compat.equals(ExternalCompatibilityManager.ExternalCompatibility.NOT_APPLICABLE) ||
                    compat.equals(ExternalCompatibilityManager.ExternalCompatibility.PLACEHOLDER_API))
                continue;

            results.put(compat.toString(), 0);
        }

        for (final RuleInfo ruleInfo : main.rulesParsingManager.getAllRules()){
            if (!ruleInfo.ruleIsEnabled) continue;

            if (ruleInfo.enabledExtCompats != null){
                for (final ExternalCompatibilityManager.ExternalCompatibility compat : ruleInfo.enabledExtCompats.keySet()){
                    if (compat.equals(ExternalCompatibilityManager.ExternalCompatibility.NOT_APPLICABLE)) continue;
                    final Boolean enabled = ruleInfo.enabledExtCompats.get(compat);
                    if (enabled != null && enabled)
                        results.put(compat.toString(), 1);
                }
            }
        }

        return results;
    }
}
