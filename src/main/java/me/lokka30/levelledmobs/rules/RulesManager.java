package me.lokka30.levelledmobs.rules;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.Utils;
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

        Utils.logger.info("--------------------------------- default values below -------------------------------");
        showAllValues(defaults);
        for (RuleParsingInfo rpi : customRules) {
            Utils.logger.info("--------------------------------- custom-rule below ----------------------------------");
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
        parseWorldList(cs.get(c_Worlds));
        parseStrategies(cs.get(c_Strategies));
        parseCalculation(cs.get(c_Calculation));
        parseCustomVariables(cs.get("calculation.custom-variables"));
        parseConditions(cs.get(c_Conditions));

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

    private void parseConditions(final Object conditionsObj){
        final ConfigurationSection conditions = objectToConfigurationSection(conditionsObj);
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

    private void parseStrategies(final Object strategiesObj){
        final ConfigurationSection strategies = objectToConfigurationSection(strategiesObj);
        if (strategies == null) return;

        parsingInfo.strategies_Preset = strategies.getString("preset");
    }

    private void parseWorldList(final Object worldsObj){
        final ConfigurationSection worlds = objectToConfigurationSection(worldsObj);
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
