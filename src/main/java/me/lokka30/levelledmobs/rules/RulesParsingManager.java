package me.lokka30.levelledmobs.rules;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.misc.CachedModalList;
import me.lokka30.levelledmobs.misc.CustomUniversalGroups;
import me.lokka30.levelledmobs.misc.Utils;
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
 * TODO Describe...
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

        parseCustomMobGroups(objectToConfigurationSection(config.get("mob-groups")));

        final List<RuleInfo> presets = parsePresets(config.get("presets"));
        for (RuleInfo ri : presets)
            this.rulePresets.put(ri.presetName, ri);

        this.defaultRule = parseDefaults(config.get("default-rule"));
        this.main.rulesManager.rulesInEffect.put(Integer.MIN_VALUE, new LinkedList<>());
        this.main.rulesManager.rulesInEffect.get(Integer.MIN_VALUE).add(defaultRule);
        this.customRules = parseCustomRules(config.get("custom-rules"));
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

        final ConfigurationSection cs = objectToConfigurationSection(objDefaults);
        if (cs == null){
            Utils.logger.info("default-rule section was null");
            return this.parsingInfo;
        }

        parseValues(cs);
        return this.parsingInfo;
    }

    @NotNull
    private List<RuleInfo> parsePresets(final Object objPresets){
        final ConfigurationSection cs = objectToConfigurationSection(objPresets);
        final List<RuleInfo> results = new LinkedList<>();
        if (cs == null) return results;

        int count = -1;
        for (final String key : cs.getKeys(false)){
            count++;
            final ConfigurationSection cs_Key = objectToConfigurationSection(cs.get(key));
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
    private CachedModalList<CreatureSpawnEvent.SpawnReason> buildCachedModalListOfSpawnReason(final ConfigurationSection cs){
        CachedModalList<CreatureSpawnEvent.SpawnReason> cachedModalList = new CachedModalList<>();
        if (cs == null || isCacheModalDeclarationEmpty(cs))
            return cachedModalList;

        cachedModalList.doMerge = cs.getBoolean("merge");

        final List<String> allowedItems = getListFromConfigItem(cs, ml_AllowedItems);
        cachedModalList.allowedGroups = getSetOfGroups(cs, ml_AllowedGroups);
        final List<String> excludedItems = getListFromConfigItem(cs , ml_ExcludedItems);
        cachedModalList.excludedGroups = getSetOfGroups(cs, ml_ExcludedGroups);

        for (final String item : allowedItems){
            try {
                final CreatureSpawnEvent.SpawnReason reason = CreatureSpawnEvent.SpawnReason.valueOf(item.toUpperCase());
                cachedModalList.allowedList.add(reason);
            } catch (IllegalArgumentException e) {
                Utils.logger.warning("Invalid spawn reason: " + item);
            }
        }
        for (final String item : excludedItems){
            try {
                final CreatureSpawnEvent.SpawnReason reason = CreatureSpawnEvent.SpawnReason.valueOf(item.toUpperCase());
                cachedModalList.excludedList.add(reason);
            } catch (IllegalArgumentException e) {
                Utils.logger.warning("Invalid spawn reason: " + item);
            }
        }

        return cachedModalList;
    }

    @NotNull
    private CachedModalList<Biome> buildCachedModalListOfBiome(final ConfigurationSection cs){
        CachedModalList<Biome> cachedModalList = new CachedModalList<>();
        if (cs == null || isCacheModalDeclarationEmpty(cs))
            return cachedModalList;

        cachedModalList.doMerge = cs.getBoolean("merge");

        final List<String> allowedItems = getListFromConfigItem(cs, ml_AllowedItems);
        cachedModalList.allowedGroups = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        final List<String> excludedItems = getListFromConfigItem(cs, ml_ExcludedItems);
        cachedModalList.excludedGroups = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (final String group : cs.getStringList(ml_AllowedGroups)){
            if ("".equals(group.trim())) continue;
            if (!main.rulesManager.biomeGroupMappings.containsKey(group))
                Utils.logger.info("invalid biome group: " + group);
            else
                cachedModalList.allowedGroups.add(group);
        }

        for (final String group : cs.getStringList(ml_ExcludedGroups)){
            if ("".equals(group.trim())) continue;
            if (!main.rulesManager.biomeGroupMappings.containsKey(group))
                Utils.logger.info("invalid biome group: " + group);
            else
                cachedModalList.excludedGroups.add(group);
        }

        for (final String item : allowedItems){
            if ("".equals(item.trim())) continue;
            if ("*".equals(item.trim())){
                cachedModalList.allowAll = true;
                continue;
            }
            try {
                final Biome biome = Biome.valueOf(item.toUpperCase());
                cachedModalList.allowedList.add(biome);
            } catch (IllegalArgumentException e) {
                Utils.logger.warning("Invalid allowed biome: " + item);
            }
        }
        for (final String item : excludedItems){
            if ("".equals(item.trim())) continue;
            if ("*".equals(item.trim())){
                cachedModalList.excludeAll = true;
                continue;
            }
            try {
                final Biome biome = Biome.valueOf(item.toUpperCase());
                cachedModalList.excludedList.add(biome);
            } catch (IllegalArgumentException e) {
                Utils.logger.warning("Invalid excluded biome: " + item);
            }
        }

        return cachedModalList;
    }

    @NotNull
    private CachedModalList<String> buildCachedModalListOfString(final ConfigurationSection cs){
        final CachedModalList<String> cachedModalList = new CachedModalList<>(new TreeSet<>(String.CASE_INSENSITIVE_ORDER), new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
        if (cs == null || isCacheModalDeclarationEmpty(cs))
            return cachedModalList;

        cachedModalList.doMerge = cs.getBoolean("merge");

        for (final String item : getListFromConfigItem(cs, ml_AllowedItems)) {
            if ("".equals(item.trim())) continue;
            if ("*".equals(item.trim())){
                cachedModalList.allowAll = true;
                continue;
            }
            cachedModalList.allowedList.add(item);
        }
        cachedModalList.allowedGroups = getSetOfGroups(cs, ml_AllowedGroups);
        for (final String item : getListFromConfigItem(cs, ml_ExcludedItems)) {
            if ("".equals(item.trim())) continue;
            if ("*".equals(item.trim())){
                cachedModalList.excludeAll = true;
                continue;
            }
            cachedModalList.excludedList.add(item);
        }
        cachedModalList.excludedGroups = getSetOfGroups(cs, ml_ExcludedGroups);

        return cachedModalList;
    }

    @NotNull
    private Set<String> getSetOfGroups(@NotNull final ConfigurationSection cs, final String key){
        final List<String> groups = cs.getStringList(key);
        if (groups.isEmpty() && cs.getString(key) != null)
            groups.add(cs.getString(key));

        final Set<String> results = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (final String group : groups) {
            if ("".equals(group.trim())) continue;
            boolean invalidGroup = false;
            if (group.toLowerCase().startsWith("all")) {
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

    private boolean isCacheModalDeclarationEmpty(final ConfigurationSection cs){
        return  (
                cs.getStringList(ml_AllowedItems).isEmpty() && cs.getString(ml_AllowedItems) == null &&
                        cs.getStringList(ml_ExcludedGroups).isEmpty() && cs.getString(ml_AllowedGroups) == null &&
                        cs.getStringList(ml_ExcludedItems).isEmpty() && cs.getString(ml_ExcludedItems) == null &&
                        cs.getStringList(ml_ExcludedGroups).isEmpty() && cs.getString(ml_ExcludedGroups) == null
        );
    }

    @NotNull
    private List<String> getListFromConfigItem(@NotNull final ConfigurationSection cs, final String key){
        final List<String> result = cs.getStringList(key);
        if (result.isEmpty() && cs.getString(key) != null && !"".equals(cs.getString(key)))
            result.add(cs.getString(key));

        return result;
    }

    @NotNull
    private List<RuleInfo> parseCustomRules(final Object rulesSection) {
        final List<RuleInfo> results = new LinkedList<>();
        if (rulesSection == null) return results;

        for (final LinkedHashMap<String, Object> hashMap : (List<LinkedHashMap<String, Object>>)(rulesSection)){
            ConfigurationSection cs = objectToConfigurationSection(hashMap);
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

        parsingInfo.ruleIsEnabled = cs.getBoolean("enabled", true);
        if (cs.getString("name") != null)
            parsingInfo.setRuleName(cs.getString("name"));

        parseStrategies(objectToConfigurationSection(cs.get("strategies")));
        parseConditions(objectToConfigurationSection(cs.get("conditions")));
        parseApplySettings(objectToConfigurationSection(cs.get("apply-settings")));

        if (cs.get("allowed-entities") != null)
            parsingInfo.allowedEntities = buildCachedModalListOfString(objectToConfigurationSection(cs.get("allowed-entities")));

        parsingInfo.rulePriority = cs.getInt("priority", 0);
    }

    private void mergePreset(final ConfigurationSection cs){
        if (cs == null) return;

        final List<String> presets = cs.getStringList("use-preset");
        if (presets.isEmpty() && cs.getString("use-preset") != null)
            presets.addAll(Arrays.asList(Objects.requireNonNull(cs.getString("use-preset")).split(",")));

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

        parseFineTuning(objectToConfigurationSection(cs.get("multipliers")));
        parseEntityNameOverride(objectToConfigurationSection(cs.get("entity-name-override")));
        parseTieredColoring(objectToConfigurationSection(cs.get("tiered-coloring")));

        if (cs.getString("minLevel") != null)
            parsingInfo.restrictions_MinLevel = cs.getInt("minLevel");
        if (cs.getString("maxLevel") != null)
            parsingInfo.restrictions_MaxLevel = cs.getInt("maxLevel");

        // check for all lower case keys

        if (cs.getString("minlevel") != null)
            parsingInfo.restrictions_MinLevel = cs.getInt("minlevel");
        if (cs.getString("maxlevel") != null)
            parsingInfo.restrictions_MaxLevel = cs.getInt("maxlevel");

        if (cs.getString("no-drop-multipler-entities") != null)
            parsingInfo.conditions_NoDropEntities = buildCachedModalListOfString(objectToConfigurationSection(cs.get("no-drop-multipler-entities")));
        if (cs.getString("baby-mobs-inherit-adult-setting") != null)
            parsingInfo.babyMobsInheritAdultSetting = cs.getBoolean("baby-mobs-inherit-adult-setting");
        if (cs.getString("level-inheritance") != null)
            parsingInfo.mobLevelInheritance = cs.getBoolean("level-inheritance");
        if (cs.getString("creeper-max-damage-radius") != null)
            parsingInfo.creeperMaxDamageRadius = cs.getInt("creeper-max-damage-radius");
        if (cs.getString("use-custom-item-drops-for-mobs") != null)
            parsingInfo.customDrops_UseForMobs = cs.getBoolean("use-custom-item-drops-for-mobs");
        if (cs.getString("custom-drops-override") != null)
            parsingInfo.customDrops_UseOverride = cs.getBoolean("custom-drops-override");
        if (cs.getString("use-droptable-id") != null)
            parsingInfo.customDrop_DropTableId = cs.getString("use-droptable-id");
        if (cs.getString("nametag") != null)
            parsingInfo.nametag = cs.getString("nametag");
        if (cs.getString("creature-death-nametag") != null)
            parsingInfo.nametag_CreatureDeath = cs.getString("creature-death-nametag");
        if (cs.getString("creature-nametag-always-visible") != null)
            parsingInfo.CreatureNametagAlwaysVisible = cs.getBoolean("creature-nametag-always-visible");
        if (cs.getString("sunlight-intensity") != null)
            parsingInfo.sunlightBurnAmount = cs.getDouble("sunlight-intensity");
    }

    private void parseConditions(final ConfigurationSection conditions){
        if (conditions  == null) return;

        if (conditions.get("worlds") != null)
            parsingInfo.conditions_Worlds = buildCachedModalListOfString(objectToConfigurationSection(conditions.get("worlds")));
        parseExternalCompat(objectToConfigurationSection(conditions.get("level-plugins")));

        if (conditions.getString("minLevel") != null)
            parsingInfo.conditions_MinLevel = conditions.getInt("minLevel");
        if (conditions.getString("maxLevel") != null)
            parsingInfo.conditions_MaxLevel = conditions.getInt("maxLevel");

        if (conditions.getString("stop-processing") != null)
            parsingInfo.stopProcessingRules = conditions.getBoolean("stop-processing");
        if (conditions.getString("chance") != null)
            parsingInfo.conditions_Chance = conditions.getDouble("chance");
        final String mobCustomNameStatus = conditions.getString("mob-customname-status");
        if (mobCustomNameStatus != null) {
            try {
                parsingInfo.conditions_MobCustomnameStatus = MobCustomNameStatusEnum.valueOf(mobCustomNameStatus.toUpperCase());
            } catch (Exception e) {
                Utils.logger.warning("Invalid value for " + mobCustomNameStatus);
            }
        }

        final String mobTamedStatus = conditions.getString("mob-tamed-status");
        if (mobTamedStatus != null) {
            try {
                parsingInfo.conditions_MobTamedStatus = MobTamedStatusEnum.valueOf(mobTamedStatus.toUpperCase());
            } catch (Exception e) {
                Utils.logger.warning("Invalid value for " + mobTamedStatus);
            }
        }

        if (conditions.getString("apply-above-y") != null)
            parsingInfo.conditions_ApplyAboveY = conditions.getInt("apply-above-y");
        if (conditions.getString("apply-below-y") != null)
            parsingInfo.conditions_ApplyBelowY = conditions.getInt("apply-below-y");
        if (conditions.getString("allowed-worldguard-regions") != null)
            parsingInfo.conditions_WGRegions = buildCachedModalListOfString(objectToConfigurationSection(conditions.get("allowed-worldguard-regions")));
        if (conditions.getString("allowed-spawn-reasons") != null)
            parsingInfo.conditions_SpawnReasons = buildCachedModalListOfSpawnReason(objectToConfigurationSection(conditions.get("allowed-spawn-reasons")));
        if (conditions.getString("custom-names") != null)
            parsingInfo.conditions_CustomNames = buildCachedModalListOfString(objectToConfigurationSection(conditions.get("custom-names")));
        if (conditions.get("entities") != null)
            parsingInfo.conditions_Entities = buildCachedModalListOfString(objectToConfigurationSection(conditions.get("entities")));
        if (conditions.get("biomes") != null)
            parsingInfo.conditions_Biomes = buildCachedModalListOfBiome(objectToConfigurationSection(conditions.get("biomes")));
        if (conditions.get("apply-plugins") != null)
            parsingInfo.conditions_ApplyPlugins = buildCachedModalListOfString(objectToConfigurationSection(conditions.get("apply-plugins")));
        if (conditions.get("mythicmobs-internal-names") != null)
            parsingInfo.conditions_MM_Names = buildCachedModalListOfString(objectToConfigurationSection(conditions.get("mythicmobs-internal-names")));
    }

    private void parseStrategies(final ConfigurationSection strategies){
        if (strategies == null) return;

        if (strategies.getString("max-random-variance") != null)
            parsingInfo.maxRandomVariance = strategies.getInt("max-random-variance", 0);
        if (strategies.getString("random") != null)
            parsingInfo.useRandomLevelling = strategies.getBoolean("random");

        ConfigurationSection cs_YDistance = objectToConfigurationSection(strategies.get("y-coordinate"));
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

        ConfigurationSection cs_SpawnDistance = objectToConfigurationSection(strategies.get("distance-from-spawn"));
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
                parseBlendedLevelling(objectToConfigurationSection(cs_SpawnDistance.get("blended-levelling")), spawnDistanceStrategy);

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

        final ConfigurationSection cs_Custom = objectToConfigurationSection(cs.get("custom-mob-level"));
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

            final FineTuningAttributes attribs = parseFineTuningValues(objectToConfigurationSection(cs_Custom.get(mobName)));
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
    private ConfigurationSection objectToConfigurationSection(final Object object){
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
