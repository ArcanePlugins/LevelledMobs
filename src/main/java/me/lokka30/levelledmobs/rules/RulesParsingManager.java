/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.rules;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.misc.CachedModalList;
import me.lokka30.levelledmobs.misc.CustomUniversalGroups;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.levelledmobs.misc.YmlParsingHelper;
import me.lokka30.levelledmobs.rules.strategies.RandomLevellingStrategy;
import me.lokka30.levelledmobs.rules.strategies.SpawnDistanceStrategy;
import me.lokka30.levelledmobs.rules.strategies.YDistanceStrategy;
import org.bukkit.Particle;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Contains the logic that parses rules.yml and reads them into the
 * corresponding java classes
 *
 * @author stumper66
 * @since 3.0.0
 */
@SuppressWarnings("unchecked")
public class RulesParsingManager {
    public RulesParsingManager(final LevelledMobs main){
        this.main = main;
        this.rulePresets = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.customRules = new LinkedList<>();
        this.ymlHelper = new YmlParsingHelper();
        this.emptyArrayPattern = Pattern.compile("\\[\\s+?]|\\[]");
    }

    final private LevelledMobs main;
    final private YmlParsingHelper ymlHelper;
    private RuleInfo parsingInfo;
    @NotNull
    public final Map<String, RuleInfo> rulePresets;
    @NotNull
    public List<RuleInfo> customRules;
    public RuleInfo defaultRule;
    private Map<String, Set<String>> customBiomeGroups;
    private final Pattern emptyArrayPattern;
    private final static String ml_AllowedItems = "allowed-list";
    private final static String ml_AllowedGroups = "allowed-groups";
    private final static String ml_ExcludedItems = "excluded-list";
    private final static String ml_ExcludedGroups = "excluded-groups";

    public void parseRulesMain(final YamlConfiguration config){
        if (config == null) return;

        this.rulePresets.clear();
        this.main.rulesManager.rulesInEffect.clear();
        this.main.customMobGroups.clear();

        parseCustomMobGroups(objTo_CS(config, "mob-groups"));
        parseCustomBiomeGroups(objTo_CS(config, "biome-groups"));

        final List<RuleInfo> presets = parsePresets(objTo_CS(config, "presets"));
        for (final RuleInfo ri : presets)
            this.rulePresets.put(ri.presetName, ri);

        this.defaultRule = parseDefaults(objTo_CS(config, "default-rule"));
        this.main.rulesManager.rulesInEffect.put(Integer.MIN_VALUE, new LinkedList<>());
        this.main.rulesManager.rulesInEffect.get(Integer.MIN_VALUE).add(defaultRule);
        this.main.rulesManager.anyRuleHasChance = this.defaultRule.conditions_Chance != null;

        this.main.rulesManager.buildBiomeGroupMappings(customBiomeGroups);
        this.customRules = parseCustomRules(config.get(ymlHelper.getKeyNameFromConfig(config, "custom-rules")));
        for (final RuleInfo ruleInfo : customRules) {
            if (!this.main.rulesManager.rulesInEffect.containsKey(ruleInfo.rulePriority))
                this.main.rulesManager.rulesInEffect.put(ruleInfo.rulePriority, new LinkedList<>());

            this.main.rulesManager.rulesInEffect.get(ruleInfo.rulePriority).add(ruleInfo);
            if (ruleInfo.conditions_Chance != null) this.main.rulesManager.anyRuleHasChance = true;
        }
    }

    @NotNull
    public List<RuleInfo> getAllRules(){
        final List<RuleInfo> results = new LinkedList<>();
        if (this.defaultRule != null) results.add(this.defaultRule);
        results.addAll(this.rulePresets.values());
        results.addAll(this.customRules);

        return results;
    }

    private void parseCustomMobGroups(final ConfigurationSection cs){
        if (cs == null) return;

        for (final String groupName : cs.getKeys(false)){
            final List<String> names = cs.getStringList(groupName);
            final Set<String> groupMembers = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            groupMembers.addAll(names);
            main.customMobGroups.put(groupName, groupMembers);
        }
    }

    private void parseCustomBiomeGroups(final ConfigurationSection cs){
        this.customBiomeGroups = new TreeMap<>();
        if (cs == null) return;

        for (final String groupName : cs.getKeys(false)){
            final List<String> names = cs.getStringList(groupName);
            final Set<String> groupMembers = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            groupMembers.addAll(names);
            this.customBiomeGroups.put(groupName, groupMembers);
        }
    }

    @NotNull
    private RuleInfo parseDefaults(final ConfigurationSection cs) {
        this.parsingInfo = new RuleInfo("defaults");
        parsingInfo.restrictions_MinLevel = 0;
        parsingInfo.restrictions_MaxLevel = 0;
        parsingInfo.conditions_MobCustomnameStatus = MobCustomNameStatus.EITHER;
        parsingInfo.conditions_MobTamedStatus = MobTamedStatus.EITHER;
        parsingInfo.babyMobsInheritAdultSetting = true;
        parsingInfo.mobLevelInheritance = true;
        parsingInfo.creeperMaxDamageRadius = 5;
        parsingInfo.nametagVisibleTime = 4000;
        parsingInfo.nametagVisibilityEnum = List.of(NametagVisibilityEnum.TARGETED, NametagVisibilityEnum.ATTACKED, NametagVisibilityEnum.TRACKING);

        if (cs == null){
            Utils.logger.info("default-rule section was null");
            return this.parsingInfo;
        }

        parseValues(cs);
        return this.parsingInfo;
    }

    @NotNull
    private List<RuleInfo> parsePresets(final ConfigurationSection cs){
        final List<RuleInfo> results = new LinkedList<>();
        if (cs == null) return results;

        int count = -1;
        for (final String key : cs.getKeys(false)){
            count++;
            final ConfigurationSection cs_Key = objTo_CS(cs, key);
            if (cs_Key == null){
                Utils.logger.warning("nothing was specified for preset: " + key);
                continue;
            }

            this.parsingInfo = new RuleInfo("preset " + count);
            this.parsingInfo.presetName = key;
            parseValues(cs_Key);
            results.add(this.parsingInfo);
        }

        return results;
    }

    @NotNull
    private CachedModalList<LevelledMobSpawnReason> buildCachedModalListOfSpawnReason(final ConfigurationSection cs,
                                                                                      final CachedModalList<LevelledMobSpawnReason> defaultValue) {
        if (cs == null) return defaultValue;

        final CachedModalList<LevelledMobSpawnReason> cachedModalList = new CachedModalList<>();
        final Object simpleStringOrArray = cs.get(ymlHelper.getKeyNameFromConfig(cs, "allowed-spawn-reasons"));
        ConfigurationSection cs2 = null;
        List<String> useList = null;

        if (simpleStringOrArray instanceof ArrayList)
            useList = new LinkedList<>((ArrayList<String>) simpleStringOrArray);
        else if (simpleStringOrArray instanceof String)
            useList = List.of((String) simpleStringOrArray);

        if (useList == null) {
            final String useKeyName = ymlHelper.getKeyNameFromConfig(cs, "allowed-spawn-reasons");
            cs2 = objTo_CS(cs, useKeyName);
        }
        if (cs2 == null && useList == null) return defaultValue;

        cachedModalList.doMerge = ymlHelper.getBoolean(cs2, "merge");
        if (cs2 != null) {
            final String allowedList = ymlHelper.getKeyNameFromConfig(cs2, ml_AllowedItems);
            useList = YmlParsingHelper.getListFromConfigItem(cs2, allowedList);
        }

        for (final String item : useList){
            if (item.trim().isEmpty()) continue;
            if ("*".equals(item.trim())){
                cachedModalList.allowAll = true;
                continue;
            }
            try {
                final LevelledMobSpawnReason reason = LevelledMobSpawnReason.valueOf(item.trim().toUpperCase());
                cachedModalList.allowedList.add(reason);
            } catch (final IllegalArgumentException ignored) {
                Utils.logger.warning("Invalid spawn reason: " + item);
            }
        }
        if (cs2 == null) return cachedModalList;

        cachedModalList.allowedGroups = getSetOfGroups(cs, ml_AllowedGroups);
        final String excludedList = ymlHelper.getKeyNameFromConfig(cs2, ml_ExcludedItems);
        cachedModalList.excludedGroups = getSetOfGroups(cs, ml_ExcludedGroups);

        for (final String item : YmlParsingHelper.getListFromConfigItem(cs2, excludedList)){
            if (item.trim().isEmpty()) continue;
            if ("*".equals(item.trim())){
                cachedModalList.excludeAll = true;
                continue;
            }
            if (emptyArrayPattern.matcher(item).matches()) continue;
            try {
                final LevelledMobSpawnReason reason = LevelledMobSpawnReason.valueOf(item.trim().toUpperCase());
                cachedModalList.excludedList.add(reason);
            } catch (final IllegalArgumentException ignored) {
                Utils.logger.warning("Invalid spawn reason: " + item);
            }
        }

        if (cachedModalList.isEmpty() && !cachedModalList.allowAll && !cachedModalList.excludeAll)
            return defaultValue;

        return cachedModalList;
    }

    @NotNull
    private CachedModalList<Biome> buildCachedModalListOfBiome(final ConfigurationSection cs, final CachedModalList<Biome> defaultValue){
        if (cs == null) return defaultValue;

        final CachedModalList<Biome> cachedModalList = new CachedModalList<>();
        final Object simpleStringOrArray = cs.get(ymlHelper.getKeyNameFromConfig(cs, "biomes"));
        ConfigurationSection cs2 = null;
        List<String> useList = null;

        if (simpleStringOrArray instanceof ArrayList)
            useList = new LinkedList<>((ArrayList<String>) simpleStringOrArray);
        else if (simpleStringOrArray instanceof String)
            useList = List.of((String) simpleStringOrArray);

        if (useList == null) {
            final String useKeyName = ymlHelper.getKeyNameFromConfig(cs, "biomes");
            cs2 = objTo_CS(cs, useKeyName);
        }
        if (cs2 == null && useList == null) return defaultValue;

        cachedModalList.doMerge = ymlHelper.getBoolean(cs2, "merge");

        if (cs2 != null) {
            cachedModalList.allowedGroups = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            cachedModalList.excludedGroups = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

            for (final String group : YmlParsingHelper.getListFromConfigItem(cs2, ml_AllowedGroups)) {
                if (group.trim().isEmpty()) continue;
                if (!main.rulesManager.biomeGroupMappings.containsKey(group))
                    Utils.logger.info("invalid biome group: " + group);
                else
                    cachedModalList.allowedGroups.add(group);
            }

            for (final String group : YmlParsingHelper.getListFromConfigItem(cs2, ml_ExcludedGroups)) {
                if (group.trim().isEmpty()) continue;
                if (!main.rulesManager.biomeGroupMappings.containsKey(group))
                    Utils.logger.info("invalid biome group: " + group);
                else
                    cachedModalList.excludedGroups.add(group);
            }
        }

        if (useList == null) {
            final String allowedList = ymlHelper.getKeyNameFromConfig(cs2, ml_AllowedItems);
            useList = YmlParsingHelper.getListFromConfigItem(cs2, allowedList);
        }

        for (final String item : useList){
            if (item.trim().isEmpty()) continue;
            if ("*".equals(item.trim())){
                cachedModalList.allowAll = true;
                continue;
            }
            if (emptyArrayPattern.matcher(item).matches()) continue;
            try {
                final Biome biome = Biome.valueOf(item.trim().toUpperCase());
                cachedModalList.allowedList.add(biome);
            } catch (final IllegalArgumentException e) {
                Utils.logger.warning("Invalid allowed biome: " + item);
            }
        }
        if (cs2 == null) return cachedModalList;

        final String excludedList = ymlHelper.getKeyNameFromConfig(cs2, ml_ExcludedItems);

        for (final String item : YmlParsingHelper.getListFromConfigItem(cs2, excludedList)){
            if (item.trim().isEmpty()) continue;
            if ("*".equals(item.trim())){
                cachedModalList.excludeAll = true;
                continue;
            }
            try {
                final Biome biome = Biome.valueOf(item.trim().toUpperCase());
                cachedModalList.excludedList.add(biome);
            } catch (final IllegalArgumentException e) {
                Utils.logger.warning("Invalid excluded biome: " + item);
            }
        }

        return cachedModalList;
    }

    @Nullable
    private CachedModalList<String> buildCachedModalListOfString(final ConfigurationSection cs, @NotNull final String name, final CachedModalList<String> defaultValue){
        if (cs == null) return defaultValue;

        final CachedModalList<String> cachedModalList = new CachedModalList<>(new TreeSet<>(String.CASE_INSENSITIVE_ORDER), new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
        final Object simpleStringOrArray = cs.get(ymlHelper.getKeyNameFromConfig(cs, name));
        ConfigurationSection cs2 = null;
        List<String> useList = null;

        if (simpleStringOrArray instanceof ArrayList)
            useList = new LinkedList<>((ArrayList<String>) simpleStringOrArray);
        else if (simpleStringOrArray instanceof String)
            useList = List.of((String) simpleStringOrArray);

        if (useList == null) {
            final String useKeyName = ymlHelper.getKeyNameFromConfig(cs, name);
            cs2 = objTo_CS(cs, useKeyName);
        }
        if (cs2 == null && useList == null) return defaultValue;

        cachedModalList.doMerge = ymlHelper.getBoolean(cs2, "merge");

        if (cs2 != null) {
            final String allowedList = ymlHelper.getKeyNameFromConfig(cs2, ml_AllowedItems);
            useList = YmlParsingHelper.getListFromConfigItem(cs2, allowedList);
        }

        for (final String item : useList) {
            if (item.trim().isEmpty()) continue;
            if ("*".equals(item.trim())){
                cachedModalList.allowAll = true;
                continue;
            }
            if (emptyArrayPattern.matcher(item).matches()) continue;
            cachedModalList.allowedList.add(item);
        }
        if (cs2 == null) return cachedModalList;

        final String allowedGroups = ymlHelper.getKeyNameFromConfig(cs2, ml_AllowedGroups);
        final String excludedList = ymlHelper.getKeyNameFromConfig(cs2, ml_ExcludedItems);
        final String excludedGroups = ymlHelper.getKeyNameFromConfig(cs2, ml_ExcludedGroups);
        cachedModalList.allowedGroups = getSetOfGroups(cs2, allowedGroups);

        for (final String item : YmlParsingHelper.getListFromConfigItem(cs2, excludedList)) {
            if (item.trim().isEmpty()) continue;
            if ("*".equals(item.trim())){
                cachedModalList.excludeAll = true;
                continue;
            }
            cachedModalList.excludedList.add(item);
        }
        cachedModalList.excludedGroups = getSetOfGroups(cs2, excludedGroups);

        if (cachedModalList.isEmpty() && !cachedModalList.allowAll && !cachedModalList.excludeAll)
            return defaultValue;

        return cachedModalList;
    }

    @NotNull
    private Set<String> getSetOfGroups(@NotNull final ConfigurationSection cs, final String key){
        String foundKeyName = null;
        for (final String enumeratedKey : cs.getKeys(false)){
            if (key.equalsIgnoreCase(enumeratedKey)){
                foundKeyName = enumeratedKey;
                break;
            }
        }

        final Set<String> results = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (foundKeyName == null) return results;

        final List<String> groups = cs.getStringList(foundKeyName);
        if (groups.isEmpty() && cs.getString(foundKeyName) != null)
            groups.add(cs.getString(foundKeyName));

        for (final String group : groups) {
            if (group.trim().isEmpty()) continue;
            boolean invalidGroup = false;
            if (group.toLowerCase().startsWith("all_")) {
                try {
                    final CustomUniversalGroups customGroup = CustomUniversalGroups.valueOf(group.toUpperCase());
                    results.add(customGroup.toString());
                    continue;
                } catch (final IllegalArgumentException e) {
                    invalidGroup = true;
                }
            }
            if (main.customMobGroups.containsKey(group))
                results.add(group);
            else
                invalidGroup = true;

            if (invalidGroup)
                Utils.logger.warning("Invalid group: " + group);
        }

        return results;
    }

    @NotNull
    private List<RuleInfo> parseCustomRules(final Object rulesSection) {
        final List<RuleInfo> results = new LinkedList<>();
        if (rulesSection == null) return results;

        for (final LinkedHashMap<String, Object> hashMap : (List<LinkedHashMap<String, Object>>) (rulesSection)) {
            final ConfigurationSection cs = objTo_CS_2(hashMap);
            if (cs == null) {
                Utils.logger.info("cs was null (parsing custom-rules)");
                continue;
            }

            this.parsingInfo = new RuleInfo("rule " + results.size());
            parseValues(cs);
            results.add(this.parsingInfo);
        }

        return results;
    }

    private void parseValues(final ConfigurationSection cs){
        mergePreset(cs);

        parsingInfo.ruleIsEnabled = ymlHelper.getBoolean(cs, "enabled", true);
        //final String ruleName = cs.getString(ymlHelper.getKeyNameFromConfig(cs, "name"));
        final String ruleName = ymlHelper.getString(cs, "name");
        if (ruleName != null) parsingInfo.setRuleName(ymlHelper.getString(cs, "name"));

        parseStrategies(objTo_CS(cs,"strategies"));
        parseConditions(objTo_CS(cs,"conditions"));
        parseApplySettings(objTo_CS(cs, "apply-settings"));

        parsingInfo.allowedEntities = buildCachedModalListOfString(cs, "allowed-entities", parsingInfo.allowedEntities);
        parsingInfo.rulePriority = ymlHelper.getInt(cs, "priority");
    }

    private void mergePreset(final ConfigurationSection cs){
        if (cs == null) return;

        final String usePresetName = ymlHelper.getKeyNameFromConfig(cs,"use-preset");
        final List<String> presets = cs.getStringList(usePresetName);
        if (presets.isEmpty() && cs.getString(usePresetName) != null)
            presets.addAll(Arrays.asList(Objects.requireNonNull(cs.getString(usePresetName)).split(",")));

        if (presets.isEmpty()) return;

        for (String checkName : presets) {
            checkName = checkName.trim();
            if (!rulePresets.containsKey(checkName)) {
                Utils.logger.info(parsingInfo.getRuleName() + ", specified preset name '" + checkName + "' but none was found");
                continue;
            }

            this.parsingInfo.mergePresetRules(rulePresets.get(checkName));
        }
    }

    private void parseExternalCompat(final ConfigurationSection cs){
        if (cs == null) return;

        final Map<ExternalCompatibilityManager.ExternalCompatibility, Boolean> results = new EnumMap<>(ExternalCompatibilityManager.ExternalCompatibility.class);

        for (final String key : cs.getKeys(false)){
            final boolean value = cs.getBoolean(key);

            final ExternalCompatibilityManager.ExternalCompatibility compat;
            try {
                compat = ExternalCompatibilityManager.ExternalCompatibility.valueOf(key.toUpperCase());
                results.put(compat, value);
            } catch (final IllegalArgumentException e) {
                Utils.logger.warning("Invalid level-plugins key: " + key);
            }
        }

        if (!results.isEmpty()) parsingInfo.enabledExtCompats = results;
    }

    private void parseTieredColoring(final ConfigurationSection cs){
        if (cs == null) return;

        for (final String name : cs.getKeys(false)){
            final String value = cs.getString(name);

            if (!Utils.isNullOrEmpty(name) && value != null){
                final TieredColoringInfo coloringInfo;

                if ("default".equalsIgnoreCase(name))
                    coloringInfo = TieredColoringInfo.createDefault(value);
                else
                    coloringInfo = TieredColoringInfo.createFromString(name, value);

                if (coloringInfo != null) {
                    if (parsingInfo.tieredColoringInfos == null) parsingInfo.tieredColoringInfos = new LinkedList<>();
                    parsingInfo.tieredColoringInfos.add(coloringInfo);
                }
            }
        }
    }

    private void parseEntityNameOverride(final ConfigurationSection cs){
        if (cs == null) return;

        final Map<String, List<LevelTierMatching>> levelTiers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        final Map<String, LevelTierMatching> entityNames = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (final String name : cs.getKeys(false)){
            if ("merge".equalsIgnoreCase(name) && cs.getBoolean(name)){
                parsingInfo.mergeEntityNameOverrides = cs.getBoolean(name);
                continue;
            }

            final List<String> names = cs.getStringList(name);
            if (!names.isEmpty()) {
                final LevelTierMatching mobNames = new LevelTierMatching();
                mobNames.mobName = name;
                mobNames.names = names;
                final List<String> names2 = new LinkedList<>();

                for (final String nameFromList : names) {
                    if (!nameFromList.isEmpty())
                        names2.add(nameFromList);
                }

                if (!names2.isEmpty())
                    entityNames.put(name, mobNames);
            } else if (cs.getString(name) != null) {
                if ("merge".equalsIgnoreCase(name)) {
                    parsingInfo.mergeEntityNameOverrides = cs.getBoolean(name);
                    continue;
                }
                if (cs.get(name) instanceof String) {
                    final LevelTierMatching mobNames = new LevelTierMatching();
                    final List<String> names2 = List.of(Objects.requireNonNull(cs.getString(name)));
                    mobNames.mobName = name;
                    mobNames.names = names2;
                    entityNames.put(name, mobNames);
                } else if (cs.get(name) instanceof MemorySection || cs.get(name) instanceof LinkedHashMap) {
                    final List<LevelTierMatching> tiers = parseNumberRange(objTo_CS(cs, name), name);
                    if (tiers != null && !tiers.isEmpty())
                        levelTiers.put(name, tiers);
                }
            }
        }

        if (!entityNames.isEmpty())
            parsingInfo.entityNameOverrides = entityNames;
        if (!levelTiers.isEmpty())
            parsingInfo.entityNameOverrides_Level = levelTiers;
    }

    @Nullable
    private List<LevelTierMatching> parseNumberRange(final ConfigurationSection cs, final String keyName){
        if (cs == null) return null;

        final List<LevelTierMatching> levelTiers = new LinkedList<>();

        for (final String name : cs.getKeys(false)){
            final List<String> names = cs.getStringList(name);
            final LevelTierMatching tier = new LevelTierMatching();

            if ("merge".equalsIgnoreCase(name))
                continue;

            tier.mobName = name;

            if (!names.isEmpty()) {
                // an array of names was provided
                tier.names = names;
            } else if (cs.getString(name) != null) {
                // a string was provided
                tier.names = new LinkedList<>();
                tier.names.add(cs.getString(name));
            }

            if (!tier.setRangeFromString(keyName))
                Utils.logger.warning("Invalid number range: " + keyName);
            else if (!tier.names.isEmpty())
                levelTiers.add(tier);
        }

        return levelTiers;
    }

    private void parseApplySettings(final ConfigurationSection cs){
        if (cs == null) return;

        parseFineTuning(objTo_CS(cs,"multipliers"));
        parseEntityNameOverride(objTo_CS(cs,"entity-name-override"));
        parseTieredColoring(objTo_CS(cs,"tiered-coloring"));
        parseHealthIndicator(objTo_CS(cs,"health-indicator"));

        parsingInfo.restrictions_MinLevel = ymlHelper.getInt2(cs, "minlevel", parsingInfo.restrictions_MinLevel);
        parsingInfo.restrictions_MaxLevel = ymlHelper.getInt2(cs, "maxlevel", parsingInfo.restrictions_MaxLevel);

        parsingInfo.conditions_NoDropEntities = buildCachedModalListOfString(cs, "no-drop-multipler-entities", parsingInfo.conditions_NoDropEntities);
        parsingInfo.babyMobsInheritAdultSetting = ymlHelper.getBoolean2(cs, "baby-mobs-inherit-adult-setting", parsingInfo.babyMobsInheritAdultSetting);
        parsingInfo.mobLevelInheritance = ymlHelper.getBoolean2(cs, "level-inheritance", parsingInfo.mobLevelInheritance);
        parsingInfo.creeperMaxDamageRadius = ymlHelper.getInt2(cs,"creeper-max-damage-radius", parsingInfo.creeperMaxDamageRadius);
        parsingInfo.customDrops_UseForMobs = ymlHelper.getBoolean2(cs,"use-custom-item-drops-for-mobs", parsingInfo.customDrops_UseForMobs);
        parsingInfo.customDrops_UseOverride = ymlHelper.getBoolean2(cs,"custom-drops-override", parsingInfo.customDrops_UseOverride);
        parsingInfo.customDrop_DropTableIds.addAll(YmlParsingHelper.getListFromConfigItem(cs,"use-droptable-id"));
        parsingInfo.nametag = ymlHelper.getString(cs,"nametag", parsingInfo.nametag);
        parsingInfo.nametag_CreatureDeath = ymlHelper.getString(cs,"creature-death-nametag", parsingInfo.nametag_CreatureDeath);
        parsingInfo.nametag_Placeholder_Levelled = ymlHelper.getString(cs, "nametag-placeholder-levelled", parsingInfo.nametag_Placeholder_Levelled);
        parsingInfo.nametag_Placeholder_Unlevelled = ymlHelper.getString(cs, "nametag-placeholder-unlevelled", parsingInfo.nametag_Placeholder_Unlevelled);
        parsingInfo.sunlightBurnAmount = ymlHelper.getDouble2(cs, "sunlight-intensity", parsingInfo.sunlightBurnAmount);
        parsingInfo.lowerMobLevelBiasFactor = ymlHelper.getInt2(cs, "lower-mob-level-bias-factor", parsingInfo.lowerMobLevelBiasFactor);
        parsingInfo.lockEntity = ymlHelper.getBoolean2(cs, "lock-entity", parsingInfo.lockEntity);
        parseNBT_Data(cs);
        parsingInfo.passengerMatchLevel = ymlHelper.getBoolean2(cs, "passenger-match-level", parsingInfo.passengerMatchLevel);
        parsingInfo.nametagVisibleTime = ymlHelper.getInt2(cs, "nametag-visible-time", parsingInfo.nametagVisibleTime);
        parsingInfo.maximumDeathInChunkThreshold = ymlHelper.getInt2(cs, "maximum-death-in-chunk-threshold", parsingInfo.maximumDeathInChunkThreshold);
        parsingInfo.chunkMaxCoolDownTime = ymlHelper.getInt2(cs, "chunk-max-cooldown-seconds", parsingInfo.chunkMaxCoolDownTime);
        parsingInfo.disableVanillaDropsOnChunkMax = ymlHelper.getBoolean2(cs, "disable-vanilla-drops-on-chunk-max", parsingInfo.disableVanillaDropsOnChunkMax);
        parsingInfo.spawnerParticlesCount = ymlHelper.getInt2(cs, "spawner-particles-count", parsingInfo.spawnerParticlesCount);
        parsingInfo.maxAdjacentChunks = ymlHelper.getInt2(cs, "max-adjacent-chunks", parsingInfo.maxAdjacentChunks);
        if (parsingInfo.maxAdjacentChunks != null && parsingInfo.maxAdjacentChunks > 10)
            parsingInfo.maxAdjacentChunks = 10;
        parseSpawnerPartile(ymlHelper.getString(cs, "spawner-particles"));

        final Set<String> nametagVisibility = ymlHelper.getStringSet(cs, "nametag-visibility-method");
        final List<NametagVisibilityEnum> nametagVisibilityEnums = new LinkedList<>();
        for (final String nametagVisEnum : nametagVisibility) {
            try {
                final NametagVisibilityEnum nametagVisibilityEnum = NametagVisibilityEnum.valueOf(nametagVisEnum.toUpperCase());
                nametagVisibilityEnums.add(nametagVisibilityEnum);
            }
            catch (final Exception ignored){
                Utils.logger.warning("Invalid value in nametag-visibility-method: " + nametagVisibility + ", in rule: " + parsingInfo.getRuleName());
            }
        }

        if (!nametagVisibilityEnums.isEmpty())
            parsingInfo.nametagVisibilityEnum = nametagVisibilityEnums;
        else if (cs.get(ymlHelper.getKeyNameFromConfig(cs, "creature-nametag-always-visible")) != null)  {
            final Boolean nametagVisibilityBackwardsComat = ymlHelper.getBoolean2(cs, "creature-nametag-always-visible", null);
            if (nametagVisibilityBackwardsComat != null){
                if (nametagVisibilityBackwardsComat)
                    parsingInfo.nametagVisibilityEnum = List.of(NametagVisibilityEnum.ALWAYS_ON);
                else
                    parsingInfo.nametagVisibilityEnum = List.of(NametagVisibilityEnum.MELEE);
            }
            else
                parsingInfo.nametagVisibilityEnum = List.of(NametagVisibilityEnum.MELEE);
        }
    }

    private void parseSpawnerPartile(final @Nullable String particle){
        if (particle == null) return;

        if ("none".equalsIgnoreCase(particle)) {
            parsingInfo.spawnerParticle = null;
            parsingInfo.useNoSpawnerParticles = true;
            return;
        }

        try{
            parsingInfo.spawnerParticle = Particle.valueOf(particle.toUpperCase());
        }
        catch (Exception ignored){
            Utils.logger.warning("Invalid value in spawner-particles: " + particle + ", in rule: " + parsingInfo.getRuleName());
        }
    }

    private void parseNBT_Data(final @Nullable ConfigurationSection cs){
        if (cs == null) return;

        final String keyName = ymlHelper.getKeyNameFromConfig(cs, "nbt-data");
        final Object temp = cs.get(keyName);

        if (temp == null) return;

        if (temp instanceof MemorySection || temp instanceof LinkedHashMap){
            final ConfigurationSection cs2 = ymlHelper.objTo_CS(cs, keyName);
            if (cs2 == null) return;

            final String nbt = ymlHelper.getString(cs2, "data", null);
            final Set<String> nbtList = ymlHelper.getStringSet(cs2, "data");
            if (nbt == null && nbtList.isEmpty()) return;
            final boolean doMerge = ymlHelper.getBoolean(cs2, "merge", false);

            if (!nbtList.isEmpty()) {
                parsingInfo.mobNBT_Data = new MergeableStringList();
                parsingInfo.mobNBT_Data.setItemFromList(nbtList);
                parsingInfo.mobNBT_Data.doMerge = doMerge;
            }
            else
                parsingInfo.mobNBT_Data = new MergeableStringList(nbt, doMerge);
        }
        else if (temp instanceof Collection){
            parsingInfo.mobNBT_Data = new MergeableStringList();
            parsingInfo.mobNBT_Data.setItemFromList((Collection<String>) temp);
        }
        else if (temp instanceof String)
            parsingInfo.mobNBT_Data = new MergeableStringList((String) temp);
    }

    private void parseConditions(final @Nullable ConfigurationSection cs){
        if (cs == null) return;

        parsingInfo.conditions_Worlds = buildCachedModalListOfString(cs, "worlds", parsingInfo.conditions_Worlds);
        parseExternalCompat(objTo_CS(cs, "level-plugins"));

        parsingInfo.conditions_MinLevel = ymlHelper.getInt2(cs,"minlevel", parsingInfo.conditions_MinLevel);
        parsingInfo.conditions_MaxLevel = ymlHelper.getInt2(cs,"maxlevel", parsingInfo.conditions_MaxLevel);

        parsingInfo.stopProcessingRules = ymlHelper.getBoolean2(cs,"stop-processing", parsingInfo.stopProcessingRules);
        parsingInfo.conditions_Chance = ymlHelper.getFloat2(cs,"chance", parsingInfo.conditions_Chance);

        final String mobCustomNameStatus = ymlHelper.getString(cs,"mob-customname-status");
        if (mobCustomNameStatus != null) {
            try {
                parsingInfo.conditions_MobCustomnameStatus = MobCustomNameStatus.valueOf(mobCustomNameStatus.toUpperCase());
            } catch (final Exception e) {
                Utils.logger.warning("Invalid value for " + mobCustomNameStatus);
            }
        }

        final String mobTamedStatus = ymlHelper.getString(cs, "mob-tamed-status");
        if (mobTamedStatus != null) {
            try {
                parsingInfo.conditions_MobTamedStatus = MobTamedStatus.valueOf(mobTamedStatus.toUpperCase());
            } catch (final Exception e) {
                Utils.logger.warning("Invalid value for " + mobTamedStatus);
            }
        }

        parsingInfo.conditions_ApplyAboveY = ymlHelper.getInt2(cs,"apply-above-y", parsingInfo.conditions_ApplyAboveY);
        parsingInfo.conditions_ApplyBelowY = ymlHelper.getInt2(cs,"apply-below-y", parsingInfo.conditions_ApplyBelowY);
        parsingInfo.conditions_MinDistanceFromSpawn = ymlHelper.getInt2(cs, "min-distance-from-spawn", parsingInfo.conditions_MinDistanceFromSpawn);
        parsingInfo.conditions_MaxDistanceFromSpawn = ymlHelper.getInt2(cs, "max-distance-from-spawn", parsingInfo.conditions_MaxDistanceFromSpawn);

        parsingInfo.conditions_WGRegions = buildCachedModalListOfString(cs, "allowed-worldguard-regions", parsingInfo.conditions_WGRegions);
        parsingInfo.conditions_WGRegionOwners = buildCachedModalListOfString(cs, "allowed-worldguard-region-owners", parsingInfo.conditions_WGRegionOwners);
        parsingInfo.conditions_SpawnReasons = buildCachedModalListOfSpawnReason(cs, parsingInfo.conditions_SpawnReasons);
        parsingInfo.conditions_CustomNames = buildCachedModalListOfString(cs,"custom-names", parsingInfo.conditions_CustomNames);
        parsingInfo.conditions_Entities = buildCachedModalListOfString(cs, "entities", parsingInfo.conditions_Entities);
        parsingInfo.conditions_Biomes = buildCachedModalListOfBiome(cs, parsingInfo.conditions_Biomes);
        parsingInfo.conditions_ApplyPlugins = buildCachedModalListOfString(cs, "apply-plugins", parsingInfo.conditions_ApplyPlugins);
        parsingInfo.conditions_MM_Names = buildCachedModalListOfString(cs,"mythicmobs-internal-names", parsingInfo.conditions_MM_Names);
        parsingInfo.conditions_SpawnerNames = buildCachedModalListOfString(cs,"spawner-names", parsingInfo.conditions_SpawnerNames);
        parsingInfo.conditions_SpawnegEggNames = buildCachedModalListOfString(cs,"spawner-egg-names", parsingInfo.conditions_SpawnegEggNames);
        parsingInfo.conditions_WorldTickTime = parseWorldTimeTicks(cs, parsingInfo.conditions_WorldTickTime);
        parsingInfo.conditions_Permission = buildCachedModalListOfString(cs, "permission", parsingInfo.conditions_Permission);
        parsingInfo.conditions_ScoreboardTags = buildCachedModalListOfString(cs, "scoreboard-tags", parsingInfo.conditions_ScoreboardTags);
        parsingInfo.conditions_SkyLightLevel = parseMinMaxValue(ymlHelper.getString(cs, "skylight-level"), "skylight-level");
    }

    private void parseStrategies(final ConfigurationSection cs){
        if (cs == null) return;

        parsingInfo.maxRandomVariance = ymlHelper.getInt2(cs, "max-random-variance", parsingInfo.maxRandomVariance);
        parsingInfo.useRandomLevelling = ymlHelper.getBoolean2(cs, "random", parsingInfo.useRandomLevelling);

        final ConfigurationSection cs_YDistance = objTo_CS(cs,"y-coordinate");
        if (cs_YDistance != null){
            final YDistanceStrategy yDistanceStrategy = parsingInfo.levellingStrategy instanceof YDistanceStrategy ?
                    (YDistanceStrategy) parsingInfo.levellingStrategy : new YDistanceStrategy();

            yDistanceStrategy.startingYLevel = ymlHelper.getInt2(cs_YDistance, "start", yDistanceStrategy.startingYLevel);
            yDistanceStrategy.endingYLevel = ymlHelper.getInt2(cs_YDistance, "end", yDistanceStrategy.endingYLevel);
            yDistanceStrategy.yPeriod = ymlHelper.getInt2(cs_YDistance, "period", yDistanceStrategy.yPeriod);

            if (this.parsingInfo.levellingStrategy != null && this.parsingInfo.levellingStrategy instanceof YDistanceStrategy)
                this.parsingInfo.levellingStrategy.mergeRule(yDistanceStrategy);
            else
                this.parsingInfo.levellingStrategy = yDistanceStrategy;
        }

        final ConfigurationSection cs_SpawnDistance = objTo_CS(cs,"distance-from-spawn");
        if (cs_SpawnDistance != null){
            final SpawnDistanceStrategy spawnDistanceStrategy = parsingInfo.levellingStrategy instanceof SpawnDistanceStrategy ?
                    (SpawnDistanceStrategy) parsingInfo.levellingStrategy : new SpawnDistanceStrategy();

            spawnDistanceStrategy.increaseLevelDistance = ymlHelper.getInt2(cs_SpawnDistance, "increase-level-distance", spawnDistanceStrategy.increaseLevelDistance);
            spawnDistanceStrategy.startDistance = ymlHelper.getInt2(cs_SpawnDistance, "start-distance", spawnDistanceStrategy.startDistance);

            if (cs_SpawnDistance.get(ymlHelper.getKeyNameFromConfig(cs_SpawnDistance,"spawn-location.x")) != null)
                spawnDistanceStrategy.spawnLocation_X = parseOptionalSpawnCoordinate(ymlHelper.getKeyNameFromConfig(cs,"spawn-location.x"), cs_SpawnDistance);
            if (cs_SpawnDistance.get(ymlHelper.getKeyNameFromConfig(cs_SpawnDistance,"spawn-location.z")) != null)
                spawnDistanceStrategy.spawnLocation_Z = parseOptionalSpawnCoordinate(ymlHelper.getKeyNameFromConfig(cs,"spawn-location.z"), cs_SpawnDistance);

            if (ymlHelper.getString(cs_SpawnDistance,"blended-levelling") != null)
                parseBlendedLevelling(objTo_CS(cs_SpawnDistance,"blended-levelling"), spawnDistanceStrategy);

            if (this.parsingInfo.levellingStrategy != null && this.parsingInfo.levellingStrategy instanceof SpawnDistanceStrategy)
                this.parsingInfo.levellingStrategy.mergeRule(spawnDistanceStrategy);
            else
                this.parsingInfo.levellingStrategy = spawnDistanceStrategy;
        }

        final ConfigurationSection cs_Random = objTo_CS(cs,"weighted-random");
        if (cs_Random != null){
            final Map<String, Integer> randomMap = new TreeMap<>();
            final RandomLevellingStrategy randomLevelling = new RandomLevellingStrategy();
            randomLevelling.doMerge = ymlHelper.getBoolean(cs_Random, "merge");

            for (final String range : cs_Random.getKeys(false)){
                if ("merge".equalsIgnoreCase(range)) continue;
                final int value = cs_Random.getInt(range);
                randomMap.put(range, value);
            }

            if (!randomMap.isEmpty())
                randomLevelling.weightedRandom = randomMap;

            if (this.parsingInfo.levellingStrategy != null && this.parsingInfo.levellingStrategy instanceof RandomLevellingStrategy)
                this.parsingInfo.levellingStrategy.mergeRule(randomLevelling);
            else
                this.parsingInfo.levellingStrategy = randomLevelling;
        }

        parsePlayerLevellingOptions(objTo_CS(cs,"player-levelling"));
    }

    @Nullable
    private CachedModalList<MinAndMax> parseWorldTimeTicks(final ConfigurationSection cs, final CachedModalList<MinAndMax> existingList){
        if (cs == null) return existingList;

        final String configName = "world-time-tick";
        final CachedModalList<String> temp = buildCachedModalListOfString(cs, configName, null);
        if (temp == null) return existingList;
        final CachedModalList<MinAndMax> result = new CachedModalList<>();
        result.allowAll = temp.allowAll;
        result.excludeAll = temp.excludeAll;
        result.excludedList.addAll(parseMinMaxValue(temp.excludedList, configName));
        result.allowedList.addAll(parseMinMaxValue(temp.allowedList, configName));

        return result;
    }

    @Nullable
    private MinAndMax parseMinMaxValue(@Nullable final String numberPair, @SuppressWarnings("SameParameterValue") final @NotNull String configName){
        if (numberPair == null) return null;

        final Set<MinAndMax> result = parseMinMaxValue(Set.of(numberPair), configName);

        if (result.isEmpty())
            return null;
        else
            return result.iterator().next();
    }

    @NotNull
    private Set<MinAndMax> parseMinMaxValue(@NotNull final Set<String> numberPairs, final @NotNull String configName){
        final Set<MinAndMax> result = new TreeSet<>();

        for (final String numberPair : numberPairs) {
            final String[] split = numberPair.split("-");
            final MinAndMax minAndMax = new MinAndMax();
            boolean hadInvalidValue = false;
            for (int i = 0; i <= 1; i++) {
                if (!Utils.isInteger(split[i])) {
                    Utils.logger.info(String.format("Invalid value for %s: '%s' in rule %s", configName, split[i], parsingInfo.getRuleName()));
                    hadInvalidValue = true;
                    break;
                }
                final int parsedNum = Integer.parseInt(split[i]);

                if (i == 0) {
                    minAndMax.min = parsedNum;
                    if (split.length == 1) minAndMax.max = parsedNum;
                }
                else
                    minAndMax.max = parsedNum;
            }

            if (hadInvalidValue) continue;
            result.add(minAndMax);
        }

        return result;
    }

    private void parseHealthIndicator(final ConfigurationSection cs){
        if (cs == null) return;

        final HealthIndicator indicator = new HealthIndicator();
        indicator.indicator = ymlHelper.getString(cs, "indicator", indicator.indicator);
        indicator.indicatorHalf = ymlHelper.getString(cs, "indicator-half", indicator.indicatorHalf);
        indicator.maxIndicators = ymlHelper.getInt2(cs, "max", indicator.maxIndicators);
        indicator.scale = ymlHelper.getDouble2(cs, "scale", indicator.scale);
        indicator.doMerge = ymlHelper.getBoolean2(cs, "merge", indicator.doMerge);

        final ConfigurationSection cs_Tiers = objTo_CS(cs,"colored-tiers");
        if (cs_Tiers != null){
            final Map<Integer, String> tiers = new TreeMap<>();

            for (final String name : cs_Tiers.getKeys(false)){
                final String name2 = name.toLowerCase().replace("tier-", "");

                if ("default".equalsIgnoreCase(name)){
                    if (Utils.isNullOrEmpty(cs_Tiers.getString(name)))
                        Utils.logger.warning("No value entered for colored tier: " + name);
                    else
                        tiers.put(0, cs_Tiers.getString(name));

                    continue;
                }

                if (!Utils.isInteger(name2)){
                    Utils.logger.warning("Not a valid colored tier, missing number: " + name);
                    continue;
                }

                final String tierValue = cs_Tiers.getString(name);
                if (Utils.isNullOrEmpty(tierValue)){
                    Utils.logger.warning("No value entered for colored tier: " + name);
                    continue;
                }

                final int tierNumber = Integer.parseInt(name2);
                if (tiers.containsKey(tierNumber))
                    Utils.logger.warning("Duplicate tier: " + name);
                else
                    tiers.put(tierNumber, tierValue);
            }
            if (!tiers.isEmpty()) indicator.tiers = tiers;
        }

        if (parsingInfo.healthIndicator != null && parsingInfo.healthIndicator.doMerge != null && parsingInfo.healthIndicator.doMerge)
            parsingInfo.healthIndicator.mergeIndicator(indicator);
        else
            parsingInfo.healthIndicator = indicator;
    }

    private void parsePlayerLevellingOptions(final ConfigurationSection cs){
        if (cs == null) return;

        final PlayerLevellingOptions options = new PlayerLevellingOptions();
        options.matchPlayerLevel = ymlHelper.getBoolean2(cs, "match-level", options.matchPlayerLevel);
        options.usePlayerMaxLevel = ymlHelper.getBoolean2(cs, "use-player-max-level", options.usePlayerMaxLevel);
        options.playerLevelScale = ymlHelper.getDouble2(cs, "player-level-scale", options.playerLevelScale);
        options.levelCap = ymlHelper.getInt2(cs, "level-cap", options.levelCap);
        options.enabled = ymlHelper.getBoolean2(cs, "enabled", options.enabled);
        options.variable = ymlHelper.getString(cs, "variable", options.variable);
        options.decreaseLevel = ymlHelper.getBoolean(cs, "decrease-level", true);
        parsingInfo.playerLevellingOptions = options;

        final ConfigurationSection csTiers = objTo_CS(cs,"tiers");
        if (csTiers == null) return;

        final List<LevelTierMatching> levelTiers = new LinkedList<>();

        for (final String name : csTiers.getKeys(false)){
            final LevelTierMatching info = new LevelTierMatching();

            final String value = csTiers.getString(name);
            if (value == null) {
                Utils.logger.warning("No value was specified for: " + name);
                continue;
            }

            if (!name.contains("-") && !Utils.isInteger(name)){
                // found a source tier name rather than number
                info.sourceTierName = name;
            }
            else if (!info.setRangeFromString(name)){
                Utils.logger.warning("Invalid number range: " + name);
                continue;
            }

            final int[] levelRange = LevelTierMatching.getRangeFromString(value);
            if (levelRange.length < 2) {
                Utils.logger.warning("Invalid number range (len): " + value);
                continue;
            }
            if (levelRange[0] == -1 && levelRange[1] == -1){
                Utils.logger.warning("Invalid number range: " + value);
                continue;
            }

            info.valueRanges = levelRange;
            levelTiers.add(info);
        }

        if (!levelTiers.isEmpty()) options.levelTiers.addAll(levelTiers);

    }

    private void parseBlendedLevelling(final ConfigurationSection cs, final @NotNull SpawnDistanceStrategy spawnDistanceStrategy){
        if (cs == null) return;

        spawnDistanceStrategy.blendedLevellingEnabled = ymlHelper.getBoolean2(cs, "enabled", spawnDistanceStrategy.blendedLevellingEnabled);
        spawnDistanceStrategy.transition_Y_Height = ymlHelper.getInt2(cs, "transition-y-height", spawnDistanceStrategy.transition_Y_Height);
        spawnDistanceStrategy.lvlMultiplier = ymlHelper.getDouble2(cs, "lvl-multiplier", spawnDistanceStrategy.lvlMultiplier);
        spawnDistanceStrategy.multiplierPeriod = ymlHelper.getInt2(cs, "multiplier-period", spawnDistanceStrategy.multiplierPeriod);
        spawnDistanceStrategy.scaleDownward = ymlHelper.getBoolean2(cs, "scale-downward", spawnDistanceStrategy.scaleDownward);
    }

    @Nullable
    private Integer parseOptionalSpawnCoordinate(final String path, @NotNull final ConfigurationSection cs){
        if (cs.getString(path) == null) return null;
        if ("default".equalsIgnoreCase(cs.getString(path))) return null;

        return (cs.getInt(path));
    }

    private void parseFineTuning(final ConfigurationSection cs){
        if (cs == null) return;

        parsingInfo.allMobMultipliers = parseFineTuningValues(cs);

        final ConfigurationSection cs_Custom = objTo_CS(cs,"custom-mob-level");
        if (cs_Custom == null) return;

        final Map<String, FineTuningAttributes> fineTuning = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (final String mobName : cs_Custom.getKeys(false)){
            String checkName = mobName;
            if (checkName.toLowerCase().startsWith("baby_"))
                checkName = checkName.substring(5);

            try {
                EntityType.valueOf(checkName.toUpperCase());
            } catch (final IllegalArgumentException e) {
                Utils.logger.warning("Invalid entity type: " + mobName + " for fine-tuning in rule: " + parsingInfo.getRuleName());
                continue;
            }

            final FineTuningAttributes attribs = parseFineTuningValues(objTo_CS(cs_Custom, mobName));
            if (attribs == null) continue;

            fineTuning.put(mobName, attribs);
        }

        if (!fineTuning.isEmpty()) {
            if (parsingInfo.specificMobMultipliers != null)
                parsingInfo.specificMobMultipliers.putAll(fineTuning);
            else
                parsingInfo.specificMobMultipliers = fineTuning;
        }
    }

    @Nullable
    private FineTuningAttributes parseFineTuningValues(final ConfigurationSection cs){
        if (cs == null) return null;

        final FineTuningAttributes attribs = new FineTuningAttributes();

        attribs.maxHealth = ymlHelper.getDouble2(cs, "max-health", attribs.maxHealth);
        attribs.movementSpeed = ymlHelper.getDouble2(cs, "movement-speed", attribs.movementSpeed);
        attribs.attackDamage = ymlHelper.getDouble2(cs, "attack-damage", attribs.attackDamage);
        attribs.rangedAttackDamage = ymlHelper.getDouble2(cs, "ranged-attack-damage", attribs.rangedAttackDamage);
        attribs.itemDrop = ymlHelper.getInt2(cs, "item-drop", attribs.itemDrop);
        attribs.xpDrop = ymlHelper.getInt2(cs, "xp-drop", attribs.xpDrop);
        attribs.creeperExplosionRadius = ymlHelper.getDouble2(cs, "creeper-blast-damage", attribs.creeperExplosionRadius);
        attribs.armorBonus = ymlHelper.getDouble2(cs, "armor-bonus", attribs.armorBonus);
        attribs.armorToughness = ymlHelper.getDouble2(cs, "armor-toughness", attribs.armorToughness);
        attribs.attackKnockback = ymlHelper.getDouble2(cs, "attack-knockback", attribs.attackKnockback);
        attribs.flyingSpeed = ymlHelper.getDouble2(cs, "flying-speed", attribs.flyingSpeed);
        attribs.knockbackResistance = ymlHelper.getDouble2(cs, "knockback-resistance", attribs.knockbackResistance);
        attribs.horseJumpStrength = ymlHelper.getDouble2(cs, "horse-jump-strength", attribs.horseJumpStrength);
        attribs.zombieReinforcements = ymlHelper.getDouble2(cs, "zombie-spawn-reinforcements", attribs.zombieReinforcements);
        attribs.followRange = ymlHelper.getDouble2(cs, "follow-range", attribs.followRange);
        attribs.doNotMerge = ymlHelper.getBoolean(cs, "do-not-merge", false);

        return attribs;
    }

    @Nullable
    private ConfigurationSection objTo_CS(final ConfigurationSection cs, final String path){
        if (cs == null) return null;
        final String useKey = ymlHelper.getKeyNameFromConfig(cs, path);
        final Object object = cs.get(useKey);

        if (object == null) return null;

        if (object instanceof ConfigurationSection) {
            return (ConfigurationSection) object;
        } else if (object instanceof Map) {
            final MemoryConfiguration result = new MemoryConfiguration();

            // this is to convert any non-string objects
            final Map<Object, Object> temp = (Map<Object, Object>) object;
            final Map<String, Object> temp2 = new HashMap<>(temp.size());
            for (final Object obj : temp.keySet())
                temp2.put(obj.toString(), temp.get(obj));

            result.addDefaults(temp2);
            return result.getDefaultSection();
        } else {
            final String currentPath = Utils.isNullOrEmpty(cs.getCurrentPath()) ?
                    path : cs.getCurrentPath() + "." + path;
            Utils.logger.warning(currentPath + ": couldn't parse Config of type: " + object.getClass().getSimpleName() + ", value: " + object);
            return null;
        }
    }

    @Nullable
    private ConfigurationSection objTo_CS_2(final Object object){
        if (object == null) return null;

        if (object instanceof ConfigurationSection) {
            return (ConfigurationSection) object;
        } else if (object instanceof Map) {
            final MemoryConfiguration result = new MemoryConfiguration();
            result.addDefaults((Map<String, Object>) object);
            return result.getDefaultSection();
        } else {
            Utils.logger.warning("couldn't parse Config of type: " + object.getClass().getSimpleName() + ", value: " + object);
            return null;
        }
    }
}
