/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.plugin.bukkit.rule.parsing;

import de.leonhard.storage.Yaml;
import de.leonhard.storage.sections.FlatFileSection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import me.lokka30.levelledmobs.plugin.bukkit.LevelledMobs;
import me.lokka30.levelledmobs.plugin.bukkit.file.FileHandler;
import me.lokka30.levelledmobs.plugin.bukkit.rule.Group;
import me.lokka30.levelledmobs.plugin.bukkit.rule.Rule;
import me.lokka30.levelledmobs.plugin.bukkit.rule.action.DefaultRuleActionType;
import me.lokka30.levelledmobs.plugin.bukkit.rule.action.RuleAction;
import me.lokka30.levelledmobs.plugin.bukkit.rule.action.type.ExecuteAction;
import me.lokka30.levelledmobs.plugin.bukkit.rule.condition.DefaultRuleConditionType;
import me.lokka30.levelledmobs.plugin.bukkit.rule.condition.RuleCondition;
import me.lokka30.levelledmobs.plugin.bukkit.rule.condition.type.EntityTypeCondition;
import me.lokka30.levelledmobs.plugin.bukkit.rule.condition.type.IsLevelledCondition;
import me.lokka30.levelledmobs.plugin.bukkit.rule.condition.type.LightLevelFromBlockCondition;
import me.lokka30.levelledmobs.plugin.bukkit.rule.condition.type.LightLevelFromSkyCondition;
import me.lokka30.levelledmobs.plugin.bukkit.rule.listener.RuleListener;
import me.lokka30.levelledmobs.plugin.bukkit.rule.option.DefaultRuleOptionType;
import me.lokka30.levelledmobs.plugin.bukkit.rule.option.RuleOption;
import me.lokka30.levelledmobs.plugin.bukkit.rule.option.type.TemporaryDoNotUseOption;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

/**
 * @author lokka30
 * @since 4.0.0 This class parses rules from the Rules configuration into Rule objects that are
 * accessed by the plugin. It also parses other components of the Rules system, such as groups and
 * presets.
 */
public class RuleParser {

    private final HashSet<Group<EntityType>> mobGroups = new HashSet<>();

    public @NotNull
    HashSet<Group<EntityType>> getMobGroups() {
        return mobGroups;
    }

    private final HashSet<Group<Biome>> biomeGroups = new HashSet<>();

    public @NotNull
    HashSet<Group<Biome>> getBiomeGroups() {
        return biomeGroups;
    }

    private final HashSet<RuleListener> ruleListeners = new HashSet<>();

    public @NotNull
    HashSet<RuleListener> getRuleListeners() {
        return ruleListeners;
    }

    private final HashSet<Rule> presets = new HashSet<>();

    public @NotNull
    HashSet<Rule> getPresets() {
        return presets;
    }

    public void parse() {
        clearCache();

        addRuleGroups();
        addRulePresets();
        addRuleListeners();
    }

    void clearCache() {
        // listeners
        getRuleListeners().clear();

        // presets
        getPresets().clear();

        // groups
        getMobGroups().clear();
        getBiomeGroups().clear();
    }

    void addRuleGroups() {
        addMobRuleGroups();
        addBiomeRuleGroups();
    }

    void addMobRuleGroups() {
        final Yaml data = LevelledMobs.getInstance().fileHandler.groupsFile.getData();
        for (
            String mobGroupName : data.getSection("mob-groups").singleLayerKeySet()
        ) {
            EnumSet<EntityType> entityTypes = EnumSet.noneOf(EntityType.class);

            for (
                String entityTypeStr : data.getStringList("mob-groups." + mobGroupName)
            ) {
                EntityType entityType;
                try {
                    entityType = EntityType.valueOf(entityTypeStr.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    LevelledMobs.logger().severe(
                        "Invalid entity type specified '&b" + entityTypeStr + "&7' in the mob " +
                            "group named '&b" + mobGroupName + "&7'! Fix this ASAP.");
                    continue;
                }

                if (entityTypes.contains(entityType)) {
                    LevelledMobs.logger().severe("Entity type '&b" + entityTypeStr.toUpperCase(Locale.ROOT)
                        + "&7' has been listed " +
                        "listed more than once in the mob group named '&b" + mobGroupName + "&7'! "
                        +
                        "Fix this ASAP.");
                    continue;
                }

                entityTypes.add(entityType);
            }

            getMobGroups().add(new Group<>(
                mobGroupName,
                entityTypes
            ));
        }
    }

    void addBiomeRuleGroups() {
        final Yaml data = LevelledMobs.getInstance().fileHandler.groupsFile.getData();
        for (
            String biomeGroupName : data.getSection("biome-groups").singleLayerKeySet()
        ) {
            EnumSet<Biome> biomes = EnumSet.noneOf(Biome.class);

            for (
                String biomeStr : data.getStringList("biome-groups." + biomeGroupName)
            ) {
                Biome biome;
                try {
                    biome = Biome.valueOf(biomeStr.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    LevelledMobs.logger().severe(
                        "Invalid biome specified '&b" + biomeStr + "&7' in the biome " +
                            "group named '&b" + biomeGroupName + "&7'! Fix this ASAP.");
                    continue;
                }

                if (biomes.contains(biome)) {
                    LevelledMobs.logger().severe(
                        "Biome '&b" + biomeStr.toUpperCase(Locale.ROOT) + "&7' has been listed " +
                            "listed more than once in the biome group named '&b" + biomeGroupName
                            + "&7'! " +
                            "Fix this ASAP.");
                    continue;
                }

                biomes.add(biome);
            }

            getBiomeGroups().add(new Group<>(
                biomeGroupName,
                biomes
            ));
        }
    }

    void addRulePresets() {
        LevelledMobs.getInstance().fileHandler.presetsFile.getData()
            .getSection("presets").singleLayerKeySet()
            .forEach(presetId -> presets.add(parseRule(true, presetId, "presets." + presetId)));
    }

    @NotNull
    Rule parseRule(
        boolean isPreset,
        @NotNull final String identifier,
        @NotNull final String path
    ) {
        final FileHandler fh = LevelledMobs.getInstance().fileHandler;
        final Yaml data =
            isPreset ? fh.presetsFile.getData() : fh.listenersFile.getData();
        final String ruleOrPreset = isPreset ? "preset" : "rule";

        // create base rule with no functionality
        final Rule rule = new Rule(
            isPreset,
            identifier,
            Optional.ofNullable(data.getString(path + ".description")),
            new HashSet<>(),
            new HashSet<>(),
            new HashSet<>(),
            new HashSet<>()
        );

        // parse presets
        if (!isPreset) {
            data.getStringList(path + ".use-presets").forEach(presetId -> {
                final Optional<Rule> presetInRule = presets.stream()
                    .filter(preset -> preset.identifier().equals(presetId))
                    .findFirst();
                if (presetInRule.isPresent()) {
                    LevelledMobs.logger().severe(
                        "Rule '&b" + identifier + "&7' wants to use preset '&b" + presetId
                            + "&7', but that exact preset is not configured. Fix this ASAP.");
                } else {
                    rule.presets().add(presetInRule.get());
                }
            });
        }

        // parse conditions
        data.getSection(path + ".conditions").singleLayerKeySet().forEach(ruleConditionTypeStr -> {
            final Optional<DefaultRuleConditionType> ruleConditionType = DefaultRuleConditionType.fromId(
                ruleConditionTypeStr);

            if (ruleConditionType.isPresent()) {
                LevelledMobs.logger().severe(
                    "The " + ruleOrPreset + " '&b" + identifier + "&7' has an invalid condition" +
                        " specified, named '&b" + ruleConditionTypeStr + "&7'. Fix this ASAP.");
            } else {
                rule.conditions().add(processRuleCondition(
                    rule,
                    ruleConditionType.get(),
                    data.getSection(path + ".conditions." + ruleConditionTypeStr)
                ));
            }
        });

        // parse actions
        data.getSection(path + ".actions").singleLayerKeySet().forEach(ruleActionTypeStr -> {
            final Optional<DefaultRuleActionType> ruleActionType = DefaultRuleActionType.fromId(
                ruleActionTypeStr);

            if (ruleActionType.isPresent()) {
                LevelledMobs.logger().severe(
                    "The " + ruleOrPreset + " '&b" + identifier + "&7' has an invalid action" +
                        " specified, named '&b" + ruleActionTypeStr + "&7'. Fix this ASAP.");
            } else {
                rule.actions().add(processRuleAction(
                    rule,
                    ruleActionType.get(),
                    data.getSection(path + ".actions." + ruleActionTypeStr)
                ));
            }
        });

        // parse options
        data.getSection(path + ".options").singleLayerKeySet().forEach(ruleOptionTypeStr -> {
            final Optional<DefaultRuleOptionType> ruleOptionType = DefaultRuleOptionType.fromId(
                ruleOptionTypeStr);

            if (ruleOptionType.isPresent()) {
                LevelledMobs.logger().severe(
                    "The " + ruleOrPreset + " '&b" + identifier + "&7' has an invalid option" +
                        " specified, named '&b" + ruleOptionTypeStr + "&7'. Fix this ASAP.");
            } else {
                rule.options().add(processRuleOption(
                    rule,
                    ruleOptionType.get(),
                    data.getSection(path + ".options." + ruleOptionTypeStr)
                ));
            }
        });

        // inherit missing actions & conditions from presets
        rule.presets().forEach(preset -> {

            // loop through conditions in preset
            preset.conditions()
                .forEach(presetCondition -> rule.conditions().forEach(ruleCondition -> {
                    if (presetCondition.id().equals(ruleCondition.id())) {
                        ruleCondition.merge(presetCondition);
                    }
                }));

            // loop through actions in preset
            preset.actions().forEach(presetAction -> rule.actions().forEach(ruleAction -> {
                if (presetAction.id().equals(ruleAction.id())) {
                    ruleAction.merge(presetAction);
                }
            }));

            // FYI: RuleOptions are not inherited from presets
        });

        return rule;
    }

    public boolean hasPreset(String presetId) {
        return presets.stream().anyMatch(preset -> preset.identifier().equals(presetId));
    }

    @NotNull
    RuleCondition processRuleCondition(
        final @NotNull Rule parentRule,
        final @NotNull DefaultRuleConditionType type,
        final @NotNull FlatFileSection section
    ) {
        switch (type) {
            case ENTITY_TYPE:
                return EntityTypeCondition.of(parentRule, section);
            case IS_LEVELLED:
                return IsLevelledCondition.of(parentRule, section);
            case LIGHT_LEVEL_FROM_BLOCK:
                return LightLevelFromBlockCondition.of(parentRule, section);
            case LIGHT_LEVEL_FROM_SKY:
                return LightLevelFromSkyCondition.of(parentRule, section);
            default:
                throw new IllegalStateException(
                    "Rule condition '&b" + type + "&7' does not have in-built processing logic!" +
                        " If this is meant to be a valid rule condition, and it is not a typo, please inform LevelledMobs"
                        +
                        " developers. Otherwise, fix this ASAP."
                );
        }
    }

    @NotNull
    RuleAction processRuleAction(
        final @NotNull Rule parentRule,
        final @NotNull DefaultRuleActionType type,
        final @NotNull FlatFileSection section
    ) {
        switch (type) {
            case EXECUTE:
                return ExecuteAction.of(parentRule, section);
            default:
                throw new IllegalStateException(
                    "Rule action '&b" + type + "&7' does not have in-built processing logic!" +
                        " If this is meant to be a valid rule action, and it is not a typo, please inform LevelledMobs"
                        +
                        " developers. Otherwise, fix this ASAP."
                );
        }
    }

    @NotNull
    RuleOption processRuleOption(
        final @NotNull Rule parentRule,
        final @NotNull DefaultRuleOptionType type,
        final @NotNull FlatFileSection section
    ) {
        switch (type) {
            case TEMPORARY_DO_NOT_USE:
                return new TemporaryDoNotUseOption(parentRule);
            default:
                throw new IllegalStateException(
                    "Rule option '&b" + type + "&7' does not have in-built processing logic!" +
                        " If this is meant to be a valid rule option, and it is not a typo, please inform LevelledMobs"
                        +
                        " developers. Otherwise, fix this ASAP."
                );
        }
    }

    void addRuleListeners() {
        //TODO
    }
}
