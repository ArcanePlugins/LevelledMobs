package me.lokka30.levelledmobs.rules;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.misc.CachedModalList;
import me.lokka30.levelledmobs.misc.CustomUniversalGroups;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.levelledmobs.misc.YmlParsingHelper;
import me.lokka30.levelledmobs.rules.strategies.SpawnDistanceStrategy;
import me.lokka30.levelledmobs.rules.strategies.YDistanceStrategy;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Contains the logic that parses rules.yml and reads them into the
 * corresponding java classes
 *
 * @author stumper66
 */
public class RulesParsingManager {
    public RulesParsingManager(final LevelledMobs main){
        this.main = main;
        this.rulePresets = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.customRules = new LinkedList<>();
    }

    final private LevelledMobs main;
    private RuleInfo parsingInfo;
    @NotNull
    public final Map<String, RuleInfo> rulePresets;
    @NotNull
    public List<RuleInfo> customRules;
    public RuleInfo defaultRule;
    private final static String ml_AllowedItems = "allowed-list";
    private final static String ml_AllowedGroups = "allowed-groups";
    private final static String ml_ExcludedItems = "excluded-list";
    private final static String ml_ExcludedGroups = "excluded-groups";

    public void parseRulesMain(final YamlConfiguration config){
        if (config == null) return;

        this.rulePresets.clear();
        this.main.rulesManager.rulesInEffect.clear();
        this.main.customMobGroups.clear();


        parseCustomMobGroups(objTo_CS(config.get(getKeyNameFromConfig(config, "mob-groups"))));

        final List<RuleInfo> presets = parsePresets(config.get(getKeyNameFromConfig(config, "presets")));
        for (RuleInfo ri : presets)
            this.rulePresets.put(ri.presetName, ri);

        this.defaultRule = parseDefaults(config.get(getKeyNameFromConfig(config, "default-rule")));
        this.main.rulesManager.rulesInEffect.put(Integer.MIN_VALUE, new LinkedList<>());
        this.main.rulesManager.rulesInEffect.get(Integer.MIN_VALUE).add(defaultRule);
        this.customRules = parseCustomRules(config.get(getKeyNameFromConfig(config, "custom-rules")));
        for (final RuleInfo ruleInfo : customRules) {
            if (!this.main.rulesManager.rulesInEffect.containsKey(ruleInfo.rulePriority))
                this.main.rulesManager.rulesInEffect.put(ruleInfo.rulePriority, new LinkedList<>());

            this.main.rulesManager.rulesInEffect.get(ruleInfo.rulePriority).add(ruleInfo);
        }
    }

    @NotNull
    public List<RuleInfo> getAllRules(){
        List<RuleInfo> results = new LinkedList<>();
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

    @NotNull
    private RuleInfo parseDefaults(final Object objDefaults) {
        this.parsingInfo = new RuleInfo("defaults");
        parsingInfo.restrictions_MinLevel = 1;
        parsingInfo.restrictions_MaxLevel = 10;
        parsingInfo.conditions_MobCustomnameStatus = MobCustomNameStatusEnum.EITHER;
        parsingInfo.conditions_MobTamedStatus = MobTamedStatusEnum.EITHER;
        parsingInfo.babyMobsInheritAdultSetting = true;
        parsingInfo.mobLevelInheritance = true;
        parsingInfo.creeperMaxDamageRadius = 5;

        final ConfigurationSection cs = objTo_CS(objDefaults);
        if (cs == null){
            Utils.logger.info("default-rule section was null");
            return this.parsingInfo;
        }

        parseValues(cs);
        return this.parsingInfo;
    }

    @NotNull
    private List<RuleInfo> parsePresets(final Object objPresets){
        final ConfigurationSection cs = objTo_CS(objPresets);
        final List<RuleInfo> results = new LinkedList<>();
        if (cs == null) return results;

        int count = -1;
        for (final String key : cs.getKeys(false)){
            count++;
            final ConfigurationSection cs_Key = objTo_CS(cs.get(key));
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
    private CachedModalList<CreatureSpawnEvent.SpawnReason> buildCachedModalListOfSpawnReason(final ConfigurationSection cs,
                                                                                              final CachedModalList<CreatureSpawnEvent.SpawnReason> defaultValue){
        if (cs == null) return defaultValue;


        final String useKeyName = getKeyNameFromConfig(cs, "allowed-spawn-reasons");
        final ConfigurationSection cs2 = objTo_CS(cs.get(useKeyName));
        if (cs2 == null) return defaultValue;

        final CachedModalList<CreatureSpawnEvent.SpawnReason> cachedModalList = new CachedModalList<>();
        cachedModalList.doMerge = YmlParsingHelper.getBoolean(cs2, "merge");

        final String allowedList = getKeyNameFromConfig(cs2, ml_AllowedItems);
        cachedModalList.allowedGroups = getSetOfGroups(cs, ml_AllowedGroups);
        final String excludedList = getKeyNameFromConfig(cs2, ml_ExcludedItems);
        cachedModalList.excludedGroups = getSetOfGroups(cs, ml_ExcludedGroups);

        for (final String item : getListFromConfigItem(cs2, allowedList)){
            if ("".equals(item.trim())) continue;
            if ("*".equals(item.trim())){
                cachedModalList.allowAll = true;
                continue;
            }
            try {
                final CreatureSpawnEvent.SpawnReason reason = CreatureSpawnEvent.SpawnReason.valueOf(item.trim().toUpperCase());
                cachedModalList.allowedList.add(reason);
            } catch (IllegalArgumentException e) {
                Utils.logger.warning("Invalid spawn reason: " + item);
            }
        }
        for (final String item : getListFromConfigItem(cs2, excludedList)){
            if ("".equals(item.trim())) continue;
            if ("*".equals(item.trim())){
                cachedModalList.excludeAll = true;
                continue;
            }
            try {
                final CreatureSpawnEvent.SpawnReason reason = CreatureSpawnEvent.SpawnReason.valueOf(item.trim().toUpperCase());
                cachedModalList.excludedList.add(reason);
            } catch (IllegalArgumentException e) {
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

        final String useKeyName = getKeyNameFromConfig(cs, "biomes");
        final ConfigurationSection cs2 = objTo_CS(cs.get(useKeyName));
        if (cs2 == null) return defaultValue;

        final CachedModalList<Biome> cachedModalList = new CachedModalList<>();
        cachedModalList.doMerge = YmlParsingHelper.getBoolean(cs2, "merge");

        final String allowedList = getKeyNameFromConfig(cs2, ml_AllowedItems);
        cachedModalList.allowedGroups = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        final String excludedList = getKeyNameFromConfig(cs2, ml_ExcludedItems);
        cachedModalList.excludedGroups = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (final String group : getListFromConfigItem(cs2, ml_AllowedGroups)){
            if ("".equals(group.trim())) continue;
            if (!main.rulesManager.biomeGroupMappings.containsKey(group))
                Utils.logger.info("invalid biome group: " + group);
            else
                cachedModalList.allowedGroups.add(group);
        }

        for (final String group : getListFromConfigItem(cs2, ml_ExcludedGroups)){
            if ("".equals(group.trim())) continue;
            if (!main.rulesManager.biomeGroupMappings.containsKey(group))
                Utils.logger.info("invalid biome group: " + group);
            else
                cachedModalList.excludedGroups.add(group);
        }

        for (final String item : getListFromConfigItem(cs2, allowedList)){
            if ("".equals(item.trim())) continue;
            if ("*".equals(item.trim())){
                cachedModalList.allowAll = true;
                continue;
            }
            try {
                final Biome biome = Biome.valueOf(item.trim().toUpperCase());
                cachedModalList.allowedList.add(biome);
            } catch (IllegalArgumentException e) {
                Utils.logger.warning("Invalid allowed biome: " + item);
            }
        }
        for (final String item : getListFromConfigItem(cs2, excludedList)){
            if ("".equals(item.trim())) continue;
            if ("*".equals(item.trim())){
                cachedModalList.excludeAll = true;
                continue;
            }
            try {
                final Biome biome = Biome.valueOf(item.trim().toUpperCase());
                cachedModalList.excludedList.add(biome);
            } catch (IllegalArgumentException e) {
                Utils.logger.warning("Invalid excluded biome: " + item);
            }
        }

        return cachedModalList;
    }

    @Nullable
    private CachedModalList<String> buildCachedModalListOfString(final ConfigurationSection cs, @NotNull final String name, final CachedModalList<String> defaultValue){
        if (cs == null) return defaultValue;

        final String useKeyName = getKeyNameFromConfig(cs, name);
        final ConfigurationSection cs2 = objTo_CS(cs.get(useKeyName));
        if (cs2 == null) return defaultValue;

        final CachedModalList<String> cachedModalList = new CachedModalList<>(new TreeSet<>(String.CASE_INSENSITIVE_ORDER), new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
        cachedModalList.doMerge = YmlParsingHelper.getBoolean(cs2, "merge");

        final String allowedList = getKeyNameFromConfig(cs2, ml_AllowedItems);
        final String allowedGroups = getKeyNameFromConfig(cs2, ml_AllowedGroups);
        final String excludedList = getKeyNameFromConfig(cs2, ml_ExcludedItems);
        final String excludedGroups = getKeyNameFromConfig(cs2, ml_ExcludedGroups);

        for (final String item : getListFromConfigItem(cs2, allowedList)) {
            if ("".equals(item.trim())) continue;
            if ("*".equals(item.trim())){
                cachedModalList.allowAll = true;
                continue;
            }
            cachedModalList.allowedList.add(item);
        }
        cachedModalList.allowedGroups = getSetOfGroups(cs2, allowedGroups);
        for (final String item : getListFromConfigItem(cs2, excludedList)) {
            if ("".equals(item.trim())) continue;
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
    private String getKeyNameFromConfig(final ConfigurationSection cs, final String key){
        for (final String enumeratedKey : cs.getKeys(false)){
            if (key.equalsIgnoreCase(enumeratedKey))
                return enumeratedKey;
        }

        return key;
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
            if ("".equals(group.trim())) continue;
            boolean invalidGroup = false;
            if (group.toLowerCase().startsWith("all_")) {
                try {
                    final CustomUniversalGroups customGroup = CustomUniversalGroups.valueOf(group.toUpperCase());
                    results.add(group);
                    continue;
                } catch (IllegalArgumentException e) {
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
    private List<String> getListFromConfigItem(@NotNull final ConfigurationSection cs, final String key){
        String foundKeyName = null;
        for (final String enumeratedKey : cs.getKeys(false)){
            if (key.equalsIgnoreCase(enumeratedKey)){
                foundKeyName = enumeratedKey;
                break;
            }
        }

        if (foundKeyName == null) return new LinkedList<>();

        final List<String> result = cs.getStringList(foundKeyName);
        if (result.isEmpty() && cs.getString(foundKeyName) != null && !"".equals(cs.getString(foundKeyName)))
            result.add(cs.getString(foundKeyName));

        return result;
    }

    @NotNull
    private List<RuleInfo> parseCustomRules(final Object rulesSection) {
        final List<RuleInfo> results = new LinkedList<>();
        if (rulesSection == null) return results;

        for (final LinkedHashMap<String, Object> hashMap : (List<LinkedHashMap<String, Object>>)(rulesSection)){
            final ConfigurationSection cs = objTo_CS(hashMap);
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

        parsingInfo.ruleIsEnabled = YmlParsingHelper.getBoolean(cs, "enabled", true);
        final String ruleName = cs.getString(getKeyNameFromConfig(cs, "name"));
        if (ruleName != null) parsingInfo.setRuleName(cs.getString("name"));

        parseStrategies(objTo_CS(cs.get(getKeyNameFromConfig(cs,"strategies"))));
        parseConditions(objTo_CS(cs.get(getKeyNameFromConfig(cs,"conditions"))));
        parseApplySettings(objTo_CS(cs.get(getKeyNameFromConfig(cs,"apply-settings"))));

        parsingInfo.allowedEntities = buildCachedModalListOfString(cs, "allowed-entities", parsingInfo.allowedEntities);
        parsingInfo.rulePriority = YmlParsingHelper.getInt(cs, "priority");
    }

    private void mergePreset(final ConfigurationSection cs){
        if (cs == null) return;

        final String usePresetName = getKeyNameFromConfig(cs,"use-preset");
        final List<String> presets = cs.getStringList(usePresetName);
        if (presets.isEmpty() && cs.getString(usePresetName) != null)
            presets.addAll(Arrays.asList(Objects.requireNonNull(cs.getString(usePresetName)).split(",")));

        if (presets.isEmpty()) return;

        for (String checkName : presets) {
            checkName = checkName.trim();
            if (!rulePresets.containsKey(checkName)) {
                Utils.logger.info(parsingInfo.getRuleName() + ", specified preset name '" + checkName + "' was none was found");
                continue;
            }

            this.parsingInfo.mergePresetRules(rulePresets.get(checkName));
        }
    }

    private void parseExternalCompat(final ConfigurationSection cs){
        if (cs == null) return;

        final Map<ExternalCompatibilityManager.ExternalCompatibility, Boolean> results = new TreeMap<>();

        for (final String key : cs.getKeys(false)){
            boolean value = cs.getBoolean(key);

            ExternalCompatibilityManager.ExternalCompatibility compat;
            try {
                compat = ExternalCompatibilityManager.ExternalCompatibility.valueOf(key.toUpperCase());
                results.put(compat, value);
            } catch (IllegalArgumentException e) {
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
                TieredColoringInfo coloringInfo;

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

        for (final String name : cs.getKeys(false)){
            final List<String> names = cs.getStringList(name);
            if (!names.isEmpty())
                parsingInfo.entityNameOverrides.put(name, names);
            else if (cs.getString(name) != null) {
                names.add(cs.getString(name));
                parsingInfo.entityNameOverrides.put(name, names);
            }
        }
    }

    private void parseApplySettings(final ConfigurationSection cs){
        if (cs == null) return;

        parseFineTuning(objTo_CS(cs.get(getKeyNameFromConfig(cs,"multipliers"))));
        parseEntityNameOverride(objTo_CS(cs.get(getKeyNameFromConfig(cs,"entity-name-override"))));
        parseTieredColoring(objTo_CS(cs.get(getKeyNameFromConfig(cs,"tiered-coloring"))));

        parsingInfo.restrictions_MinLevel = YmlParsingHelper.getInt2(cs, "minlevel", parsingInfo.restrictions_MinLevel);
        parsingInfo.restrictions_MaxLevel = YmlParsingHelper.getInt2(cs, "maxlevel", parsingInfo.restrictions_MaxLevel);

        parsingInfo.conditions_NoDropEntities = buildCachedModalListOfString(cs, "no-drop-multipler-entities", parsingInfo.conditions_NoDropEntities);
        parsingInfo.babyMobsInheritAdultSetting = YmlParsingHelper.getBoolean2(cs, "baby-mobs-inherit-adult-setting", parsingInfo.babyMobsInheritAdultSetting);
        parsingInfo.mobLevelInheritance = YmlParsingHelper.getBoolean2(cs, "level-inheritance", parsingInfo.mobLevelInheritance);
        parsingInfo.creeperMaxDamageRadius = YmlParsingHelper.getInt2(cs,"creeper-max-damage-radius", parsingInfo.creeperMaxDamageRadius);
        parsingInfo.customDrops_UseForMobs = YmlParsingHelper.getBoolean2(cs,"use-custom-item-drops-for-mobs", parsingInfo.customDrops_UseForMobs);
        parsingInfo.customDrops_UseOverride = YmlParsingHelper.getBoolean2(cs,"custom-drops-override", parsingInfo.customDrops_UseOverride);
        parsingInfo.customDrop_DropTableId = YmlParsingHelper.getString(cs,"use-droptable-id", parsingInfo.customDrop_DropTableId);
        parsingInfo.nametag = YmlParsingHelper.getString(cs,"nametag", parsingInfo.nametag);
        parsingInfo.nametag_CreatureDeath = YmlParsingHelper.getString(cs,"creature-death-nametag", parsingInfo.nametag_CreatureDeath);
        parsingInfo.CreatureNametagAlwaysVisible = YmlParsingHelper.getBoolean2(cs,"creature-nametag-always-visible", parsingInfo.CreatureNametagAlwaysVisible);
        parsingInfo.sunlightBurnAmount = YmlParsingHelper.getDouble2(cs, "sunlight-intensity", parsingInfo.sunlightBurnAmount);
    }

    private void parseConditions(final ConfigurationSection cs){
        if (cs == null) return;

        parsingInfo.conditions_Worlds = buildCachedModalListOfString(cs, "worlds", parsingInfo.conditions_Worlds);
        parseExternalCompat(objTo_CS(cs.get(getKeyNameFromConfig(cs, "level-plugins"))));

        parsingInfo.conditions_MinLevel = YmlParsingHelper.getInt2(cs,"minlevel", parsingInfo.conditions_MinLevel);
        parsingInfo.conditions_MaxLevel = YmlParsingHelper.getInt2(cs,"maxlevel", parsingInfo.conditions_MaxLevel);

        parsingInfo.stopProcessingRules = YmlParsingHelper.getBoolean2(cs,"stop-processing", parsingInfo.stopProcessingRules);
        parsingInfo.conditions_Chance = YmlParsingHelper.getDouble2(cs,"chance", parsingInfo.conditions_Chance);
        final String mobCustomNameStatus = cs.getString(getKeyNameFromConfig(cs,"mob-customname-status"));
        if (mobCustomNameStatus != null) {
            try {
                parsingInfo.conditions_MobCustomnameStatus = MobCustomNameStatusEnum.valueOf(mobCustomNameStatus.toUpperCase());
            } catch (Exception e) {
                Utils.logger.warning("Invalid value for " + mobCustomNameStatus);
            }
        }

        final String mobTamedStatus = cs.getString(getKeyNameFromConfig(cs, "mob-tamed-status"));
        if (mobTamedStatus != null) {
            try {
                parsingInfo.conditions_MobTamedStatus = MobTamedStatusEnum.valueOf(mobTamedStatus.toUpperCase());
            } catch (Exception e) {
                Utils.logger.warning("Invalid value for " + mobTamedStatus);
            }
        }

        parsingInfo.conditions_ApplyAboveY = YmlParsingHelper.getInt2(cs,"apply-above-y", parsingInfo.conditions_ApplyAboveY);
        parsingInfo.conditions_ApplyBelowY = YmlParsingHelper.getInt2(cs,"apply-below-y", parsingInfo.conditions_ApplyBelowY);

        parsingInfo.conditions_WGRegions = buildCachedModalListOfString(cs, "allowed-worldguard-regions", parsingInfo.conditions_WGRegions);
        parsingInfo.conditions_SpawnReasons = buildCachedModalListOfSpawnReason(cs, parsingInfo.conditions_SpawnReasons);
        parsingInfo.conditions_CustomNames = buildCachedModalListOfString(cs,"custom-names", parsingInfo.conditions_CustomNames);
        parsingInfo.conditions_Entities = buildCachedModalListOfString(cs, "entities", parsingInfo.conditions_Entities);
        parsingInfo.conditions_Biomes = buildCachedModalListOfBiome(cs, parsingInfo.conditions_Biomes);
        parsingInfo.conditions_ApplyPlugins = buildCachedModalListOfString(cs, "apply-plugins", parsingInfo.conditions_ApplyPlugins);
        parsingInfo.conditions_MM_Names = buildCachedModalListOfString(cs,"mythicmobs-internal-names", parsingInfo.conditions_MM_Names);
    }

    private void parseStrategies(final ConfigurationSection cs){
        if (cs == null) return;

        if (cs.getString("max-random-variance") != null)
            parsingInfo.maxRandomVariance = cs.getInt("max-random-variance", 0);
        if (cs.getString("random") != null)
            parsingInfo.useRandomLevelling = cs.getBoolean("random");

        ConfigurationSection cs_YDistance = objTo_CS(cs.get("y-coordinate"));
        if (cs_YDistance != null){
            final YDistanceStrategy yDistanceStrategy = parsingInfo.levellingStrategy instanceof YDistanceStrategy ?
                    (YDistanceStrategy) parsingInfo.levellingStrategy : new YDistanceStrategy();
            if (cs_YDistance.getString("start") != null)
                yDistanceStrategy.startingYLevel = cs_YDistance.getInt("start");
            if (cs_YDistance.getString("end") != null)
                yDistanceStrategy.endingYLevel = cs_YDistance.getInt("end");
            if (cs_YDistance.getString("period") != null)
                yDistanceStrategy.yPeriod = cs_YDistance.getInt("period");

            this.parsingInfo.levellingStrategy = yDistanceStrategy;
        }

        ConfigurationSection cs_SpawnDistance = objTo_CS(cs.get("distance-from-spawn"));
        if (cs_SpawnDistance != null){
            final SpawnDistanceStrategy spawnDistanceStrategy = parsingInfo.levellingStrategy instanceof SpawnDistanceStrategy ?
                    (SpawnDistanceStrategy) parsingInfo.levellingStrategy : new SpawnDistanceStrategy();
            if (cs_SpawnDistance.getString("increase-level-distance") != null)
                spawnDistanceStrategy.increaseLevelDistance = cs_SpawnDistance.getInt("increase-level-distance");
            if (cs_SpawnDistance.getString("start-distance") != null)
                spawnDistanceStrategy.startDistance = cs_SpawnDistance.getInt("start-distance");
            if (cs_SpawnDistance.getString("spawn-location.x") != null)
                spawnDistanceStrategy.spawnLocation_X = parseOptionalSpawnCoordinate("spawn-location.x", cs_SpawnDistance);
            if (cs_SpawnDistance.getString("spawn-location.z") != null)
                spawnDistanceStrategy.spawnLocation_Z = parseOptionalSpawnCoordinate("spawn-location.z", cs_SpawnDistance);
            if (cs_SpawnDistance.getString("blended-levelling") != null)
                parseBlendedLevelling(objTo_CS(cs_SpawnDistance.get("blended-levelling")), spawnDistanceStrategy);

            this.parsingInfo.levellingStrategy = spawnDistanceStrategy;
        }
    }

    private void parseBlendedLevelling(final ConfigurationSection cs, final @NotNull SpawnDistanceStrategy spawnDistanceStrategy){
        if (cs == null) return;

        if (cs.getString("enabled") != null)
            spawnDistanceStrategy.blendedLevellingEnabled = cs.getBoolean("enabled");
        if (cs.getString("transition-y-height") != null)
            spawnDistanceStrategy.transition_Y_Height = cs.getInt("transition-y-height");
        if (cs.getString("lvl-multiplier") != null)
            spawnDistanceStrategy.lvlMultiplier = cs.getDouble("lvl-multiplier");
        if (cs.getString("multiplier-period") != null)
            spawnDistanceStrategy.multiplierPeriod = cs.getInt("multiplier-period");
        if (cs.getString("scale-downward") != null)
            spawnDistanceStrategy.scaleDownward = cs.getBoolean("scale-downward");
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

        final ConfigurationSection cs_Custom = objTo_CS(cs.get("custom-mob-level"));
        if (cs_Custom == null) return;

        final Map<String, FineTuningAttributes> fineTuning = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (final String mobName : cs_Custom.getKeys(false)){
            String checkName = mobName;
            if (checkName.toLowerCase().startsWith("baby_"))
                checkName = checkName.substring(5);

            EntityType entityType;
            try {
                entityType = EntityType.valueOf(checkName.toUpperCase());
            } catch (IllegalArgumentException e) {
                Utils.logger.warning("Invalid entity type: " + mobName + " for fine-tuning in rule: " + parsingInfo.getRuleName());
                continue;
            }

            final FineTuningAttributes attribs = parseFineTuningValues(objTo_CS(cs_Custom.get(mobName)));
            if (attribs == null) continue;

            attribs.applicableEntity = entityType;
            fineTuning.put(mobName, attribs);
        }

        if (!fineTuning.isEmpty()) parsingInfo.specificMobMultipliers = fineTuning;
    }

    @Nullable
    private FineTuningAttributes parseFineTuningValues(final ConfigurationSection cs){
        if (cs == null) return null;

        FineTuningAttributes attribs = new FineTuningAttributes();
        if (cs.getString("max-health") != null)
            attribs.maxHealth = cs.getDouble("max-health");
        if (cs.getString("movement-speed") != null)
            attribs.movementSpeed = cs.getDouble("movement-speed");
        if (cs.getString("attack-damage") != null)
            attribs.attackDamage = cs.getDouble("attack-damage");
        if (cs.getString("ranged-attack-damage") != null)
            attribs.rangedAttackDamage = cs.getDouble("ranged-attack-damage");
        if (cs.getString("item-drop") != null)
            attribs.itemDrop = cs.getInt("item-drop");
        if (cs.getString("xp-drop") != null)
            attribs.xpDrop = cs.getInt("xp-drop");

        return attribs;
    }

    private void parseStategiesRandom(final ConfigurationSection cs){
        if (cs == null) {
            Utils.logger.info("random was null, " + parsingInfo.getRuleName());
            return;
        }

        // if they simply specified 'random:' then we enabled random levelling
        parsingInfo.useRandomLevelling = true;
    }

    @NotNull
    private static List<String> getListOrItemFromConfig(final String name, @NotNull final ConfigurationSection cs){
        List<String> result = cs.getStringList(name);
        if (result.isEmpty() && !Utils.isNullOrEmpty(cs.getString(name)))
            result.add(cs.getString(name));

        return result;
    }

    @Nullable
    private ConfigurationSection objTo_CS(final Object object){
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
