package me.lokka30.levelledmobs.rules;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.misc.*;
import me.lokka30.levelledmobs.rules.strategies.SpawnDistanceStrategy;
import me.lokka30.levelledmobs.rules.strategies.YDistanceStrategy;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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

    public void parseRulesMain(final YamlConfiguration config){
        if (config == null) return;

        this.rulePresets.clear();
        this.main.rulesManager.rulesInEffect.clear();

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
        parsingInfo.tieredColoringInfos = new ArrayList<>(4);
        parsingInfo.tieredColoringInfos.add(TieredColoringInfo.createFromString("1-3", "&a"));
        parsingInfo.tieredColoringInfos.add(TieredColoringInfo.createFromString("4-6", "&e"));
        parsingInfo.tieredColoringInfos.add(TieredColoringInfo.createFromString("7-10", "&c"));
        parsingInfo.tieredColoringInfos.add(TieredColoringInfo.createDefault("&1"));

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
        if (cs == null) return cachedModalList;
        if (cs.getString("mode") == null && cs.getString("list") == null) return cachedModalList;

        cachedModalList.listMode = getModalListMode(cs.getString("mode"), cs.getCurrentPath());

        final List<String> items = cs.getStringList("list");
        if (items.isEmpty() && cs.getString("list") != null)
            items.add(cs.getString("list"));

        for (final String item : items){
            try{
                final CreatureSpawnEvent.SpawnReason reason = CreatureSpawnEvent.SpawnReason.valueOf(item.toUpperCase());
                cachedModalList.items.add(reason);
            }
            catch (IllegalArgumentException e){
                Utils.logger.warning("Invalid spawn reason: " + item);
            }
        }

        return cachedModalList;
    }

    @NotNull
    private CachedModalList<String> buildCachedModalListOfString(final ConfigurationSection cs){
        CachedModalList<String> cachedModalList = new CachedModalList<>(new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
        if (cs == null) return cachedModalList;
        if (cs.getString("mode") == null && cs.getString("list") == null) return cachedModalList;

        cachedModalList.listMode = getModalListMode(cs.getString("mode"), cs.getCurrentPath());

        final List<String> items = cs.getStringList("list");
        if (items.isEmpty() && cs.getString("list") != null)
            items.add(cs.getString("list"));

        for (final String item : items){
            if (item.toLowerCase().startsWith("all_")){
                try{
                    final CustomUniversalGroups group = CustomUniversalGroups.valueOf(item.toUpperCase());
                    cachedModalList.items.add(group.toString());
                }
                catch (IllegalArgumentException e){
                    Utils.logger.warning("Invalid custom group: " + item);
                }
            }
            else
                cachedModalList.items.add(item);
        }

        return cachedModalList;
    }

    @NotNull
    private ModalListMode getModalListMode(final String item, final String path){
        if (item == null || "".equals(item)){
            Utils.logger.warning("No list mode was specified, " + path);
            return ModalListMode.ALL;
        }

        switch (item.toUpperCase()) {
            case "ALL":
                return ModalListMode.ALL;
            case "BLACKLIST":
                return ModalListMode.BLACKLIST;
            case "WHITELIST":
                return ModalListMode.WHITELIST;
            default:
                Utils.logger.warning("Invalid list mode: " + item);
                return ModalListMode.ALL;
        }
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

        final List<ExternalCompatibilityManager.ExternalCompatibility> results = new LinkedList<>();

        for (final String key : cs.getKeys(false)){
            boolean value = cs.getBoolean(key);
            if (!value) continue;

            ExternalCompatibilityManager.ExternalCompatibility compat;
            try{
                compat = ExternalCompatibilityManager.ExternalCompatibility.valueOf(key.toUpperCase());
                results.add(compat);
            }
            catch (IllegalArgumentException e){
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
        parsingInfo.nametag = cs.getString("nametag");
        parsingInfo.nametag_CreatureDeath = cs.getString("creature-death-nametag");
        if (cs.getString("creature-nametag-always-visible") != null)
            parsingInfo.CreatureNametagAlwaysVisible = cs.getBoolean("creature-nametag-always-visible");
    }

    private void parseConditions(final ConfigurationSection conditions){
        if (conditions  == null) return;

        parseWorldList(objectToConfigurationSection(conditions.get("worlds")));
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
            try{
                parsingInfo.conditions_MobCustomnameStatus = MobCustomNameStatusEnum.valueOf(mobCustomNameStatus.toUpperCase());
            }
            catch (Exception e){
                Utils.logger.warning("Invalid value for " + mobCustomNameStatus);
            }
        }

        final String mobTamedStatus = conditions.getString("mob-tamed-status");
        if (mobTamedStatus != null) {
            try{
                parsingInfo.conditions_MobTamedStatus = MobTamedStatusEnum.valueOf(mobTamedStatus.toUpperCase());
            }
            catch (Exception e){
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
            parsingInfo.conditions_Biomes = buildCachedModalListOfString(objectToConfigurationSection(conditions.get("biomes")));
        if (conditions.get("apply-plugins") != null)
            parsingInfo.conditions_ApplyPlugins = buildCachedModalListOfString(objectToConfigurationSection(conditions.get("apply-plugins")));
    }

    private void parseStrategies(final ConfigurationSection strategies){
        if (strategies == null) return;

        if (strategies.getString("max-random-variance") != null)
            parsingInfo.maxRandomVariance = strategies.getInt("max-random-variance", 0);
        if (strategies.getString("random") != null)
            parsingInfo.useRandomLevelling = strategies.getBoolean("random");

        ConfigurationSection cs_SpawnDistance = objectToConfigurationSection(strategies.get("distance-from-spawn"));
        if (cs_SpawnDistance != null){
            SpawnDistanceStrategy spawnDistanceStrategy = new SpawnDistanceStrategy();
            spawnDistanceStrategy.increaseLevelDistance = cs_SpawnDistance.getInt("increase-level-distance");
            spawnDistanceStrategy.startDistance = cs_SpawnDistance.getInt("start-distance");
            spawnDistanceStrategy.spawnLocation_X = parseOptionalSpawnCoordinate("spawn-location.x", cs_SpawnDistance);
            spawnDistanceStrategy.spawnLocation_Z = parseOptionalSpawnCoordinate("spawn-location.z", cs_SpawnDistance);
            parseBlendedLevelling(objectToConfigurationSection(cs_SpawnDistance.get("blended-levelling")), spawnDistanceStrategy);

            this.parsingInfo.levellingStrategies.add(spawnDistanceStrategy);
        }

        ConfigurationSection cs_YDistance = objectToConfigurationSection(strategies.get("y-coordinate"));
        if (cs_YDistance != null){
            YDistanceStrategy yDistanceStrategy = new YDistanceStrategy();
            yDistanceStrategy.startingYLevel = cs_YDistance.getInt("start");
            yDistanceStrategy.endingYLevel = cs_YDistance.getInt("end");
            yDistanceStrategy.yPeriod = cs_YDistance.getInt("period");

            this.parsingInfo.levellingStrategies.add(yDistanceStrategy);
        }
    }

    private void parseBlendedLevelling(final ConfigurationSection cs, final @NotNull SpawnDistanceStrategy spawnDistanceStrategy){
        if (cs == null) return;

        spawnDistanceStrategy.blendedLevellingEnabled = cs.getBoolean("enabled");
        spawnDistanceStrategy.transition_Y_Height = cs.getInt("transition-y-height");
        spawnDistanceStrategy.lvlMultiplier = cs.getDouble("lvl-multiplier");
        spawnDistanceStrategy.multiplierPeriod = cs.getInt("multiplier-period");
        spawnDistanceStrategy.scaleDownward = cs.getBoolean("scale-downward");
    }

    private Integer parseOptionalSpawnCoordinate(final String path, final ConfigurationSection cs){
        if (cs.getString(path) == null) return null;
        if ("default".equalsIgnoreCase(cs.getString(path))) return null;

        return (cs.getInt(path));
    }

    private void parseFineTuning(final ConfigurationSection cs){
        if (cs == null) return;

        parsingInfo.defaultFineTuning = parseFineTuningValues(cs);

        final ConfigurationSection cs_Custom = objectToConfigurationSection(cs.get("custom-mob-level"));
        if (cs_Custom == null) return;

        final Map<String, FineTuningAttributes> fineTuning = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (final String mobName : cs_Custom.getKeys(false)){
            String checkName = mobName;
            if (checkName.toLowerCase().startsWith("baby_"))
                checkName = checkName.substring(5);

            EntityType entityType;
            try{
                entityType = EntityType.valueOf(checkName.toUpperCase());
            }
            catch (IllegalArgumentException e){
                Utils.logger.warning("Invalid entity type: " + mobName + " for fine-tuning in rule: " + parsingInfo.getRuleName());
                continue;
            }

            final FineTuningAttributes attribs = parseFineTuningValues(objectToConfigurationSection(cs_Custom.get(mobName)));
            if (attribs == null) continue;

            attribs.applicableEntity = entityType;
            fineTuning.put(mobName, attribs);
        }

        if (!fineTuning.isEmpty()) parsingInfo.fineTuning = fineTuning;
    }

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

    private void parseWorldList(final ConfigurationSection worlds){
        if (worlds == null) return;

        if (worlds.getString("list") != null)
            parsingInfo.worlds = buildCachedModalListOfString(worlds);
    }

    @NotNull
    private static List<String> getListOrItemFromConfig(final String name, final ConfigurationSection cs){
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
        }
        else {
            Utils.logger.warning("couldn't parse Config of type: " + object.getClass().getSimpleName() + ", value: " + object);
            return null;
        }
    }

}
