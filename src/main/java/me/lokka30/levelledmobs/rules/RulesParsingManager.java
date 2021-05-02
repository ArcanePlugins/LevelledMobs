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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class RulesParsingManager {
    public RulesParsingManager(final LevelledMobs main){
        this.main = main;
        this.rulePresets = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    }

    final private LevelledMobs main;
    private RuleInfo parsingInfo;
    @NotNull
    public final Map<String, RuleInfo> rulePresets;
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
        this.main.rulesManager.rulesInEffect.add(defaultRule);
        this.customRules = parseCustomRules(config.get("custom-rules"));
        this.main.rulesManager.rulesInEffect.addAll(customRules);
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

        for (final String key : cs.getKeys(false)){
            final ConfigurationSection cs_Key = objectToConfigurationSection(cs.get(key));
            if (cs_Key == null){
                Utils.logger.warning("nothing was specified for preset: " + key);
                continue;
            }

            this.parsingInfo = new RuleInfo("preset " + key);
            this.parsingInfo.presetType = parsePresetType(cs_Key, key);
            if (this.parsingInfo.presetType == PresetType.NONE) continue;

            this.parsingInfo.presetName = key;
            parsePresets_Value(objectToConfigurationSection(cs_Key.get("value")));

            results.add(this.parsingInfo);
        }

        return results;
    }

    private void parsePresets_Value(final ConfigurationSection cs){
        if (cs == null) return;

        if (this.parsingInfo.presetType == PresetType.CONDITIONS)
            parseConditions(cs);
        else if (this.parsingInfo.presetType == PresetType.WORLDS)
            parseWorldList(cs);
        else if (this.parsingInfo.presetType == PresetType.STRATEGIES)
            parseStrategies(cs);
    }

    private PresetType parsePresetType(@NotNull final ConfigurationSection cs, final String key){
        final String preset = cs.getString("preset-type");
        PresetType presetType = PresetType.NONE;
        if (preset == null){
            Utils.logger.warning("No preset type was specified for preset: " + key);
            return presetType;
        }

        try{
            presetType = PresetType.valueOf(preset.toUpperCase());
        }
        catch (Exception e){
            Utils.logger.warning("Invalid preset type: " + preset);
        }

        return presetType;
    }

    private CachedModalList buildCachedModalList(final ConfigurationSection cs){
        CachedModalList cachedModalList = new CachedModalList();
        if (cs == null) return cachedModalList;

        String listMode = cs.getString("mode");
        if (listMode == null) listMode = "";
        switch (listMode.toUpperCase()) {
            case "ALL":
                cachedModalList.listMode = ModalListMode.ALL; break;
            case "BLACKLIST":
                cachedModalList.listMode = ModalListMode.BLACKLIST; break;
            case "WHITELIST":
                cachedModalList.listMode = ModalListMode.WHITELIST; break;
            default:
                cachedModalList.listMode = ModalListMode.WHITELIST;
                if ("".equals(listMode))
                    Utils.logger.warning("No list mode was specified");
                else
                    Utils.logger.warning("Invalid list mode: " + listMode);
                break;
        }

        final List<String> items = cs.getStringList("list");
        if (items.isEmpty() && cs.getString("list") != null)
            items.add(cs.getString("list"));

        for (final String item : items){
            if (item.toLowerCase().startsWith("all_")){
                try{
                    final CustomUniversalGroups group = CustomUniversalGroups.valueOf(item.toUpperCase());
                    cachedModalList.groups.add(group);
                }
                catch (IllegalArgumentException e){
                    Utils.logger.warning("Invalid custom group: " + item);
                }
            }
            else
                cachedModalList.items.put(item, null);
        }

        return cachedModalList;
    }

    @NotNull
    private List<RuleInfo> parseCustomRules(final Object rulesSection) {
        final List<RuleInfo> results = new LinkedList<>();
        if (rulesSection == null) return results;

        for (final LinkedHashMap<String, Object> hashMap : (List<LinkedHashMap<String, Object>>)(rulesSection)){
            ConfigurationSection cs = objectToConfigurationSection(hashMap);
            if (cs == null) {
                Utils.logger.info("cs was null (parsing rules)");
                continue;
            }

            this.parsingInfo = new RuleInfo("rule " + results.size());
            parseValues(cs);
            results.add(this.parsingInfo);
        }

        return results;
    }

    private void parseValues(final ConfigurationSection cs){
        parsingInfo.ruleIsEnabled = cs.getBoolean("enabled", true);
        parseCustomVariables(cs.get("calculation.custom-variables"));

        parseStrategies(objectToConfigurationSection(cs.get("strategies")));
        parseCalculation(objectToConfigurationSection(cs.get("calculation")));
        parseConditions(objectToConfigurationSection(cs.get("conditions")));
        parseLevelLimits(objectToConfigurationSection(cs.get("level-limits")));
        parseRestrictions(objectToConfigurationSection(cs.get("restrictions")));
        parseEntityNameOverride(objectToConfigurationSection(cs.get("entity-name-override")));
        parseTieredColoring(objectToConfigurationSection(cs.get("tiered-coloring")));
        parseExternalCompat(objectToConfigurationSection(cs.get("level-plugins")));

        if (cs.get("allowed-entities") != null)
            parsingInfo.allowedEntities = buildCachedModalList(objectToConfigurationSection(cs.get("allowed-entities")));

        parsingInfo.maxRandomVariance = cs.getInt("max-random-variance", 0);
        parsingInfo.nametag = cs.getString("nametag");
        parsingInfo.nametag_CreatureDeath = cs.getString("creature-death-nametag");
        if (cs.getString("creature-nametag-always-visible") != null)
            parsingInfo.CreatureNametagAlwaysVisible = cs.getBoolean("creature-nametag-always-visible");
        if (cs.getString("baby-mobs-inherit-adult-setting") != null)
            parsingInfo.babyMobsInheritAdultSetting = cs.getBoolean("baby-mobs-inherit-adult-setting");
        if (cs.getString("level-inheritance") != null)
            parsingInfo.mobLevelInheritance = cs.getBoolean("level-inheritance");
        if (cs.getString("creeper-max-damage-radius") != null)
            parsingInfo.creeperMaxDamageRadius = cs.getInt("creeper-max-damage-radius");
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

    private void parseLevelLimits(final ConfigurationSection cs){
        if (cs == null) return;

        if (cs.getString("min") != null)
            parsingInfo.conditions_MinLevel = cs.getInt("min");
        if (cs.getString("max") != null)
            parsingInfo.conditions_MaxLevel = cs.getInt("max");
    }

    private void parseRestrictions(final ConfigurationSection cs){
        if (cs == null) return;

        parseFineTuning(objectToConfigurationSection(cs.get("fine-tuning")));

        if (cs.getString("minLevel") != null)
            parsingInfo.restrictions_MinLevel = cs.getInt("minLevel");
        if (cs.getString("maxLevel") != null)
            parsingInfo.restrictions_MaxLevel = cs.getInt("maxLevel");

        // check for all lower case keys

        if (cs.getString("minlevel") != null)
            parsingInfo.restrictions_MinLevel = cs.getInt("minlevel");
        if (cs.getString("maxlevel") != null)
            parsingInfo.restrictions_MaxLevel = cs.getInt("maxlevel");
    }

    private void parseCustomVariables(final Object customVariablesObj){
        final ConfigurationSection customVariables = objectToConfigurationSection(customVariablesObj);
        if (customVariables == null) return;

        final Map<String, String> variables = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (final String key : customVariables.getKeys(true)){
            final String customVariable = customVariables.getString(key);
            if (!Utils.isNullOrEmpty(customVariable)) variables.put(key, customVariable);
        }

        if (!variables.isEmpty()) parsingInfo.calculation_CustomVariables = variables;
    }

    private void parseConditions(final ConfigurationSection conditions){
        if (conditions  == null) return;

        if (!Utils.isNullOrEmpty(conditions.getString("preset"))){
            final String presetName = conditions.getString("preset");

            if (this.rulePresets.containsKey(presetName)) {
                final RuleInfo mergingPreset = this.rulePresets.get(presetName);
                if (mergingPreset.presetType == PresetType.CONDITIONS)
                    parsingInfo.mergePresetRules(mergingPreset);
                else
                    Utils.logger.warning(String.format("rule id: %s, specified CONDITIONS preset '%s' but it was of type %s",
                            parsingInfo.getInternalId(), presetName, mergingPreset.presetType));
            }
            else
                Utils.logger.warning(String.format("rule id: %s, specified non-existant preset '%s'",
                        parsingInfo.getInternalId(), presetName));
        }

        parseWorldList(objectToConfigurationSection(conditions.get("worlds")));

        if (conditions.getString("minLevel") != null)
            parsingInfo.conditions_MinLevel = conditions.getInt("minLevel");
        if (conditions.getString("maxLevel") != null)
            parsingInfo.conditions_MaxLevel = conditions.getInt("maxLevel");

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

        if (conditions.get("entities") != null)
            parsingInfo.conditions_Entities = buildCachedModalList(objectToConfigurationSection(conditions.get("entities")));
        if (conditions.get("biomes") != null)
            parsingInfo.conditions_Biomes = buildCachedModalList(objectToConfigurationSection(conditions.get("biomes")));
        if (conditions.get("apply-plugins") != null)
            parsingInfo.conditions_ApplyPlugins = buildCachedModalList(objectToConfigurationSection(conditions.get("apply-plugins")));
    }

    private void parseCalculation(final ConfigurationSection calculation){
        if (calculation  == null) return;

        parsingInfo.calculation_Formula = calculation.getString("formula");
    }

    private void parseStrategies(final ConfigurationSection strategies){
        if (strategies == null) return;

        if (!Utils.isNullOrEmpty(strategies.getString("preset"))){
            final String presetName = strategies.getString("preset");
            if (this.rulePresets.containsKey(presetName)) {
                final RuleInfo mergingPreset = this.rulePresets.get(presetName);
                if (mergingPreset.presetType == PresetType.STRATEGIES)
                    parsingInfo.mergePresetRules(mergingPreset);
                else
                    Utils.logger.warning(String.format("rule id: %s, specified STRATEGIES preset '%s' but it was of type %s",
                            parsingInfo.getInternalId(), presetName, mergingPreset.presetType));
            }
            else
                Utils.logger.warning(String.format("rule id: %s, specified non-existant preset '%s'",
                        parsingInfo.getInternalId(), presetName));
        }

        parseStategiesRandom(objectToConfigurationSection(strategies.get("random")));

        ConfigurationSection cs_SpawnDistance = objectToConfigurationSection(strategies.get("distance-from-spawn"));
        if (cs_SpawnDistance != null){
            SpawnDistanceStrategy spawnDistanceStrategy = new SpawnDistanceStrategy();
            spawnDistanceStrategy.increaseLevelDistance = cs_SpawnDistance.getInt("increase-level-distance");
            spawnDistanceStrategy.startDistance = cs_SpawnDistance.getInt("start-distance");

            String temp = cs_SpawnDistance.getString("spawn-location.x");
            if (!Utils.isNullOrEmpty(temp) && !"default".equalsIgnoreCase(temp) && Utils.isInteger(temp))
                spawnDistanceStrategy.spawnLocation_X = Integer.parseInt(temp);
            temp = cs_SpawnDistance.getString("spawn-location.z");
            if (!Utils.isNullOrEmpty(temp) && !"default".equalsIgnoreCase(temp) && Utils.isInteger(temp))
                spawnDistanceStrategy.spawnLocation_Z = Integer.parseInt(temp);

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
                Utils.logger.warning("Invalid entity type: " + mobName + " for fine-tuning in rule: " + parsingInfo.getInternalId());
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
            attribs.maxHealth = cs.getInt("max-health");
        if (cs.getString("movement-speed") != null)
            attribs.movementSpeed = cs.getDouble("movement-speed");
        if (cs.getString("attack-damage") != null)
            attribs.attackDamage = cs.getInt("attack-damage");
        if (cs.getString("ranged-attack-damage") != null)
            attribs.rangedAttackDamage = cs.getDouble("ranged-attack-damage");
        if (cs.getString("item-drop") != null)
            attribs.itemDrop = cs.getInt("item-drop");
        if (cs.getString("xp-drop") != null)
            attribs.xpDrop = cs.getInt("xp-drop");

        return attribs;
    }

    private void parseStategiesRandom(final ConfigurationSection cs){
        if (cs == null) return;

        parsingInfo.random_BiasFactor = cs.getDouble("bias-factor");
    }

    private void parseWorldList(final ConfigurationSection worlds){
        if (worlds == null) return;

        if (!Utils.isNullOrEmpty(worlds.getString("preset"))){
            final String presetName = worlds.getString("preset");
            if (this.rulePresets.containsKey(presetName)) {
                final RuleInfo mergingPreset = this.rulePresets.get(presetName);
                if (mergingPreset.presetType == PresetType.WORLDS)
                    parsingInfo.mergePresetRules(mergingPreset);
                else
                    Utils.logger.warning(String.format("rule id: %s, specified WORLDS preset '%s' but it was of type %s",
                            parsingInfo.getInternalId(), presetName, mergingPreset.presetType));
            }
            else
                Utils.logger.warning(String.format("rule id: %s, specified non-existant preset '%s'",
                        parsingInfo.getInternalId(), presetName));
        }

        if (worlds.getString("list") != null)
            parsingInfo.worlds = buildCachedModalList(worlds);
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
