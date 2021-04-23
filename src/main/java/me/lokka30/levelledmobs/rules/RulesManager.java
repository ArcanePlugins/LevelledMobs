package me.lokka30.levelledmobs.rules;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.levelledmobs.rules.strategies.SpawnDistanceStrategy;
import me.lokka30.levelledmobs.rules.strategies.YDistanceStrategy;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;

public class RulesManager {
    public RulesManager(final LevelledMobs main){
        //this.main = main;
    }

    //final private LevelledMobs main;
    private RuleParsingInfo parsingInfo;
    private static final String c_Max_Random_Variance = "max-random-variance";
    private static final String c_Worlds = "worlds";
    private static final String c_Calculation = "calculation";
    private static final String c_Strategies = "strategies";
    private static final String c_Conditions = "conditions";

    public void parseRulesMain(final YamlConfiguration config){
        parseDefaults(config.get("default-rule"));
        RuleParsingInfo defaults = this.parsingInfo;
        final List<RuleParsingInfo> customRules = parseCustomRules(config.get("custom-rules"));
        final List<RuleParsingInfo> presets = parsePresets(config.get("presets"));

        Utils.logger.info("--------------------------------- default values below -------------------------------");
        showAllValues(defaults);
        for (RuleParsingInfo rpi : customRules) {
            Utils.logger.info("--------------------------------- custom-rule below ----------------------------------");
            showAllValues(rpi);
        }
        for (RuleParsingInfo rpi : presets) {
            Utils.logger.info("--------------------------------- preset rule below ----------------------------------");
            showAllValues(rpi);
        }
        Utils.logger.info("--------------------------------------------------------------------------------------");
    }

    private void parseDefaults(final Object objDefaults) {
        final ConfigurationSection cs = objectToConfigurationSection(objDefaults);
        if (cs == null){
            Utils.logger.info("default-rule section was null");
            return;
        }

        this.parsingInfo = new RuleParsingInfo();
        parseValues(cs);
    }

    @NotNull
    private List<RuleParsingInfo> parsePresets(final Object objPresets){
        final ConfigurationSection cs = objectToConfigurationSection(objPresets);
        final List<RuleParsingInfo> results = new LinkedList<>();
        if (cs == null) return results;

        for (final String key : cs.getKeys(false)){
            this.parsingInfo = new RuleParsingInfo();
            final ConfigurationSection cs_Key = objectToConfigurationSection(cs.get(key));
            if (cs_Key == null){
                Utils.logger.warning("nothing was specified for preset: " + key);
                continue;
            }

            this.parsingInfo.presetType = parsePresetType(cs_Key, key);
            if (this.parsingInfo.presetType == PresetType.NONE) continue;

            this.parsingInfo.isPreset = true;
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

    private PresetType parsePresetType(final ConfigurationSection cs, final String key){
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

    private void showAllValues(final RuleParsingInfo pi){
        // this is only used for dev work

        try {
            for(final Field f : pi.getClass().getDeclaredFields()) {
                final Object value = f.get(pi);
                    Utils.logger.info("name: " + f.getName() + ", value: " + (value == null ? "(null)" : value));
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @NotNull
    private List<RuleParsingInfo> parseCustomRules(final Object rulesSection) {
        final List<RuleParsingInfo> results = new LinkedList<>();
        if (rulesSection == null) return results;

        for (final LinkedHashMap<String, Object> hashMap : (List<LinkedHashMap<String, Object>>)(rulesSection)){
            ConfigurationSection cs = objectToConfigurationSection(hashMap);
            if (cs == null) {
                Utils.logger.info("cs was null (parsing rules)");
                continue;
            }

            this.parsingInfo = new RuleParsingInfo();
            parseValues(cs);
            results.add(this.parsingInfo);
        }

        return results;
    }

    private void parseValues(final ConfigurationSection cs){
        parsingInfo.ruleIsEnabled = cs.getBoolean("enabled", true);
        parseWorldList(objectToConfigurationSection(cs.get(c_Worlds)));
        parseStrategies(objectToConfigurationSection(cs.get(c_Strategies)));
        parseCalculation(cs.get(c_Calculation));
        parseCustomVariables(cs.get("calculation.custom-variables"));
        parseConditions(objectToConfigurationSection(cs.get(c_Conditions)));

        parsingInfo.maxRandomVariance = cs.getInt(c_Max_Random_Variance, 0);
        parsingInfo.minLevel = cs.getInt("restrictions.minLevel", 1);
        parsingInfo.maxLevel = cs.getInt("restrictions.maxLevel", 10);
        parsingInfo.minLevel = cs.getInt("level-limits.min", parsingInfo.minLevel);
        parsingInfo.maxLevel = cs.getInt("level-limits.max", parsingInfo.maxLevel);
    }

    private void parseCustomVariables(final Object customVariablesObj){
        final ConfigurationSection customVariables = objectToConfigurationSection(customVariablesObj);
        if (customVariables == null) return;

        for (final String key : customVariables.getKeys(true)){
            final String customVariable = customVariables.getString(key);
            if (!Utils.isNullOrEmpty(customVariable))
                parsingInfo.calculation_CustomVariables.put(key, customVariable);
        }
    }

    private void parseConditions(final ConfigurationSection conditions){
        if (conditions  == null) return;

        parsingInfo.calculation_Entities = conditions.getStringList("entities");
        if (parsingInfo.calculation_Entities.isEmpty() && !Utils.isNullOrEmpty(conditions.getString("entities")))
            parsingInfo.calculation_Entities.add(conditions.getString("entities"));
    }

    private void parseCalculation(final Object calculationObj){
        final ConfigurationSection calculation = objectToConfigurationSection(calculationObj);
        if (calculation  == null) return;

        parsingInfo.calculation_Formula = calculation.getString("formula");
    }

    private void parseStrategies(final ConfigurationSection strategies){
        if (strategies == null) return;

        parsingInfo.strategies_Preset = strategies.getString("preset");

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

    private void parseWorldList(final ConfigurationSection worlds){
        if (worlds == null) return;

        parsingInfo.worlds_UsePreset = worlds.getString("preset");
        parsingInfo.worlds_Mode = worlds.getString("mode");
        parsingInfo.worlds_List.addAll(worlds.getStringList("list"));

        // this will allow the user to use an arraylist or a single string
        if (parsingInfo.worlds_List.isEmpty() && worlds.getString("list") != null)
            parsingInfo.worlds_List.add(worlds.getString("list"));
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
