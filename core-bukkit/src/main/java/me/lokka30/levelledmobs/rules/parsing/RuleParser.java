/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.rules.parsing;

import de.leonhard.storage.Yaml;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.file.FileHandler;
import me.lokka30.levelledmobs.rules.Group;
import me.lokka30.levelledmobs.rules.Rule;
import me.lokka30.levelledmobs.rules.RuleListener;
import me.lokka30.levelledmobs.rules.action.RuleActionContainer;
import me.lokka30.levelledmobs.rules.condition.RuleConditionContainer;
import me.lokka30.levelledmobs.rules.option.RuleOption;
import me.lokka30.levelledmobs.util.Utils;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;

/**
 * @author lokka30
 * @since v4.0.0
 * This class parses rules from the Rules configuration
 * into Rule objects that are accessed by the plugin.
 * It also parses other components of the Rules system,
 * such as groups and presets.
 */
public class RuleParser {

    private final LevelledMobs main;
    public RuleParser(final @NotNull LevelledMobs main) {
        this.main = main;
    }

    private final HashSet<Group<EntityType>> mobGroups = new HashSet<>();
    public @NotNull HashSet<Group<EntityType>> getMobGroups() { return mobGroups; }

    private final HashSet<Group<Biome>> biomeGroups = new HashSet<>();
    public @NotNull HashSet<Group<Biome>> getBiomeGroups() { return biomeGroups; }

    private final HashSet<RuleListener> ruleListeners = new HashSet<>();
    public @NotNull HashSet<RuleListener> getRuleListeners() { return ruleListeners; }

    private final HashSet<Rule> presets = new HashSet<>();
    public @NotNull HashSet<Rule> getPresets() { return presets; }

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
        for(
                String mobGroupName : main.getFileHandler().getGroupsFile().getData()
                .getSection("mob-groups").singleLayerKeySet()
        ) {
            EnumSet<EntityType> entityTypes = EnumSet.noneOf(EntityType.class);

            for(
                    String entityTypeStr : main.getFileHandler().getGroupsFile().getData()
                    .getStringList("mob-groups." + mobGroupName)
            ) {
                EntityType entityType;
                try {
                    entityType = EntityType.valueOf(entityTypeStr.toUpperCase(Locale.ROOT));
                } catch(IllegalArgumentException ex) {
                    Utils.LOGGER.error("Invalid entity type specified '&b" + entityTypeStr + "&7' in the mob " +
                            "group named '&b" + mobGroupName + "&7'! Please fix this ASAP.");
                    continue;
                }

                if(entityTypes.contains(entityType)) {
                    Utils.LOGGER.error("Entity type '&b" + entityTypeStr.toUpperCase(Locale.ROOT) + "&7' has been listed " +
                            "listed more than once in the mob group named '&b" + mobGroupName + "&7'! " +
                            "Please fix this ASAP.");
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
        for(
                String biomeGroupName : main.getFileHandler().getGroupsFile().getData()
                .getSection("biome-groups").singleLayerKeySet()
        ) {
            EnumSet<Biome> biomes = EnumSet.noneOf(Biome.class);

            for(
                    String biomeStr : main.getFileHandler().getGroupsFile().getData()
                    .getStringList("biome-groups." + biomeGroupName)
            ) {
                Biome biome;
                try {
                    biome = Biome.valueOf(biomeStr.toUpperCase(Locale.ROOT));
                } catch(IllegalArgumentException ex) {
                    Utils.LOGGER.error("Invalid biome specified '&b" + biomeStr + "&7' in the biome " +
                            "group named '&b" + biomeGroupName + "&7'! Please fix this ASAP.");
                    continue;
                }

                if(biomes.contains(biome)) {
                    Utils.LOGGER.error("Biome '&b" + biomeStr.toUpperCase(Locale.ROOT) + "&7' has been listed " +
                            "listed more than once in the biome group named '&b" + biomeGroupName + "&7'! " +
                            "Please fix this ASAP.");
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
        main.getFileHandler().getPresetsFile().getData()
                .getSection("presets").singleLayerKeySet()
                .forEach(presetId -> {
                    Optional<Rule> preset = parseRule(true, presetId, "presets." + presetId);
                    if(preset.isEmpty()) {
                        Utils.LOGGER.error("Unable to register preset '&b" + presetId + "&7' due to a parsing error.");
                    } else {
                        presets.add(preset.get());
                    }
                });
    }

    @NotNull
    Optional<Rule> parseRule(
            boolean isPreset,
            @NotNull final String identifier,
            @NotNull final String path
    ) {
        //TODO
        final FileHandler fh = main.getFileHandler();
        final Yaml data = isPreset ? fh.getPresetsFile().getData() : fh.getListenersFile().getData();

        final Optional<String> description = Optional.ofNullable(data.getString(path + ".description"));

        final HashSet<Rule> presetsInRule = new HashSet<>();
        if(!isPreset) {
            data.getStringList(path + ".use-presets").forEach(presetId -> {
                final Optional<Rule> presetInRule = presets.stream()
                        .filter(preset -> preset.identifier().equals(presetId))
                        .findFirst();
                if(presetInRule.isEmpty()) {
                    Utils.LOGGER.error("Rule '&b" + identifier + "&7' wants to use preset '&b" + presetId + "&7', but that exact preset is not configured.");
                } else {
                    presetsInRule.add(presetInRule.get());
                }
            });
        }

        final HashSet<RuleConditionContainer> conditions = new HashSet<>();
        //TODO

        final HashSet<RuleActionContainer> actions = new HashSet<>();
        //TODO

        final HashSet<RuleOption> options = new HashSet<>();
        //TODO

        return Optional.of(new Rule(
                isPreset,
                identifier,
                description,
                conditions,
                actions,
                options,
                presets
        ));
    }

    boolean hasPreset(String presetId) {
        return presets.stream().anyMatch(preset -> preset.identifier().equals(presetId));
    }

    void addRuleListeners() {
        //TODO
    }
}
