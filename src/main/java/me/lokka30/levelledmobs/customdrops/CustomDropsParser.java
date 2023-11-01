/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.customdrops;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.managers.NBTManager;
import me.lokka30.levelledmobs.misc.CachedModalList;
import me.lokka30.levelledmobs.misc.CustomUniversalGroups;
import me.lokka30.levelledmobs.misc.DebugType;
import me.lokka30.levelledmobs.misc.YmlParsingHelper;
import me.lokka30.levelledmobs.result.NBTApplyResult;
import me.lokka30.levelledmobs.rules.RuleInfo;
import me.lokka30.levelledmobs.util.Utils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Parses all data from customdrops.yml and places into the corresponding java classes
 *
 * @author stumper66
 * @since 3.0.0
 */
@SuppressWarnings("unchecked")
public class CustomDropsParser {

    CustomDropsParser(final LevelledMobs main, final CustomDropsHandler handler) {
        this.main = main;
        this.defaults = new CustomDropsDefaults();
        this.defaults.groupId = defaultName;
        this.handler = handler;
        this.ymlHelper = new YmlParsingHelper();
    }

    private final LevelledMobs main;
    final YmlParsingHelper ymlHelper;
    private final CustomDropsDefaults defaults;
    private final CustomDropsHandler handler;
    private boolean hasMentionedNBTAPI_Missing;
    public boolean dropsUtilizeNBTAPI;
    private CustomDropBase dropBase;
    private CustomDropInstance dropInstance;
    private final String defaultName = "default";

    public void loadDrops(final YamlConfiguration customDropsCfg) {
        this.dropsUtilizeNBTAPI = false;
        if (customDropsCfg == null) {
            return;
        }

        boolean isDropsEnabledForAnyRule = false;

        for (final List<RuleInfo> rules : main.rulesManager.rulesInEffect.values()) {
            for (final RuleInfo ruleInfo : rules) {
                if (ruleInfo.customDrops_UseForMobs != null && ruleInfo.customDrops_UseForMobs) {
                    isDropsEnabledForAnyRule = true;
                    break;
                }
            }
            if (isDropsEnabledForAnyRule) {
                break;
            }
        }

        if (isDropsEnabledForAnyRule) {
            handler.clearGroupIdMappings();
            parseCustomDrops(customDropsCfg);
        }

        Utils.debugLog(main, DebugType.CUSTOM_DROPS, "Group Limits: " + handler.groupLimitsMap);
    }

    public @NotNull CustomDropsDefaults getDefaults(){
        return this.defaults;
    }

    private void processDefaults(final ConfigurationSection cs) {
        if (cs == null) {
            Utils.logger.warning("Defaults section was null");
            return;
        }

        // configure bogus items so we can utilize the existing attribute parse logic
        final CustomDropItem drop = new CustomDropItem(this.defaults);
        drop.setMaterial(Material.AIR);
        drop.isDefaultDrop = true;
        dropInstance = new CustomDropInstance(EntityType.AREA_EFFECT_CLOUD);
        dropInstance.customItems.add(drop);

        // this sets the drop and dropinstance defaults
        parseCustomDropsAttributes(drop, cs);

        // now we'll use the attributes here for defaults
        this.defaults.setDefaultsFromDropItem(drop);
        this.defaults.override = dropInstance.overrideStockDrops;
        this.defaults.overallChance = dropInstance.overallChance;
        this.defaults.overallPermissions.addAll(dropInstance.overallPermissions);
        handler.customDropIDs.put(this.defaults.groupId, dropInstance);
    }

    private void parseCustomDrops(final @NotNull ConfigurationSection config) {
        handler.customItemGroups = new TreeMap<>();

        processDefaults(objectToConfigurationSection2(config, "defaults"));

        final String dropTableKey = ymlHelper.getKeyNameFromConfig(config, "drop-table");
        if (config.get(dropTableKey) != null) {
            final MemorySection ms = (MemorySection) config.get(dropTableKey);
            if (ms != null) {
                final Map<String, Object> itemGroups = ms.getValues(true);

                for (final Map.Entry<String, Object> itemGroup : itemGroups.entrySet()) {
                    final String itemGroupName = itemGroup.getKey();
                    dropInstance = new CustomDropInstance(
                        EntityType.AREA_EFFECT_CLOUD); // entity type doesn't matter
                    parseCustomDrops2((List<?>) itemGroup.getValue());
                    if (!dropInstance.customItems.isEmpty() || dropInstance.getOverrideStockDrops()) {
                        handler.customItemGroups.put(itemGroupName, dropInstance);
                        handler.customDropIDs.put(itemGroupName, dropInstance);
                    }
                }
            }
        }

        for (final String item : config.getKeys(false)) {
            final String[] mobTypeOrGroups;
            EntityType entityType = null;
            mobTypeOrGroups = item.split(";");

            for (String mobTypeOrGroup : mobTypeOrGroups) {
                mobTypeOrGroup = mobTypeOrGroup.trim();
                if (mobTypeOrGroup.isEmpty()) {
                    continue;
                }
                if (mobTypeOrGroup.toLowerCase().startsWith("file-version")) {
                    continue;
                }

                CustomUniversalGroups universalGroup = null;
                final boolean isEntityTable = (mobTypeOrGroup.equalsIgnoreCase("drop-table"));
                final boolean isUniversalGroup = mobTypeOrGroup.toLowerCase().startsWith("all_");

                if (isUniversalGroup) {
                    try {
                        universalGroup = CustomUniversalGroups.valueOf(
                            mobTypeOrGroup.toUpperCase());
                    } catch (final Exception e) {
                        Utils.logger.warning(
                            "invalid universal group in customdrops.yml: " + mobTypeOrGroup);
                        continue;
                    }
                    dropInstance = new CustomDropInstance(universalGroup);
                } else if (!isEntityTable) {
                    if (mobTypeOrGroup.equalsIgnoreCase("defaults")) {
                        continue;
                    }

                    boolean isBabyMob = false;
                    if (mobTypeOrGroup.toLowerCase().startsWith("baby_")) {
                        isBabyMob = true;
                        mobTypeOrGroup = mobTypeOrGroup.substring(5);
                    }

                    try {
                        entityType = EntityType.valueOf(mobTypeOrGroup.toUpperCase());
                    } catch (final Exception e) {
                        Utils.logger.warning(
                            "invalid mob type in customdrops.yml: " + mobTypeOrGroup);
                        continue;
                    }
                    dropInstance = new CustomDropInstance(entityType, isBabyMob);
                } else {
                    // item groups, we processed them beforehand
                    continue;
                }

                dropInstance.overrideStockDrops = this.defaults.override;
                dropInstance.overallChance = this.defaults.overallChance;

                if (!isEntityTable) {
                    if (config.getList(item) != null) {
                        // standard drop processing
                        parseCustomDrops2(config.getList(item));
                    } else if (config.get(item) instanceof MemorySection) {
                        // drop is using a item group
                        final ConfigurationSection csItem = objectToConfigurationSection2(config,
                            item);
                        if (csItem == null) {
                            continue;
                        }

                        final String useEntityDropId = ymlHelper.getString(csItem, "usedroptable");
                        if (useEntityDropId != null && !handler.customItemGroups.containsKey(
                            useEntityDropId)) {
                            Utils.logger.warning(
                                "Did not find droptable id match for name: " + useEntityDropId);
                        } else if (useEntityDropId == null) {
                            Utils.logger.warning("Found a drop-table reference with no id!");
                        } else {
                            final CustomDropInstance refDrop = handler.customItemGroups.get(
                                useEntityDropId);
                            for (final CustomDropBase itemDrop : refDrop.customItems) {
                                dropInstance.customItems.add(itemDrop instanceof CustomDropItem ?
                                    ((CustomDropItem) itemDrop).cloneItem() :
                                    ((CustomCommand) itemDrop).cloneItem());
                            }
                            if (refDrop.utilizesGroupIds) {
                                dropInstance.utilizesGroupIds = true;
                            }
                            if (refDrop.getOverrideStockDrops()) {
                                dropInstance.overrideStockDrops = true;
                            }
                        }
                    }
                } // end if not entity table

                if (!dropInstance.customItems.isEmpty() || dropInstance.getOverrideStockDrops()) {
                    if (isUniversalGroup) {
                        if (handler.getCustomDropsitems_groups().containsKey(
                            universalGroup.toString())) {
                            handler.getCustomDropsitems_groups().get(universalGroup.toString())
                                .combineDrop(dropInstance);
                        } else {
                            handler.addCustomDropGroup(universalGroup.toString(), dropInstance);
                        }
                    } else {
                        final Map<EntityType, CustomDropInstance> dropMap = dropInstance.isBabyMob ?
                            handler.customDropsitems_Babies : handler.getCustomDropsitems();

                        if (dropMap.containsKey(entityType)) {
                            dropMap.get(entityType).combineDrop(dropInstance);
                        } else {
                            dropMap.put(entityType, dropInstance);
                            handler.addCustomDropItem(entityType, dropInstance);
                        }
                    }
                }
            } // next mob or group
        } // next root item from file

        if (main.companion.debugsEnabled.contains(DebugType.CUSTOM_DROPS)) {
            int dropsCount = 0;
            int commandsCount = 0;
            for (final CustomDropInstance cdi : handler.getCustomDropsitems().values()) {
                for (final CustomDropBase base : cdi.customItems) {
                    if (base instanceof CustomDropItem) {
                        dropsCount++;
                    } else if (base instanceof CustomCommand) {
                        commandsCount++;
                    }
                }
            }

            final StringBuilder sbMain = new StringBuilder();
            final int itemsCount =
                handler.getCustomDropsitems_groups().size() + handler.customDropsitems_Babies.size();
            sbMain.append(String.format(
                "drop instances: %s, custom groups: %s, item groups: %s, items: %s, commands: %s, ",
                handler.getCustomDropsitems().size(), itemsCount, handler.customItemGroups.size(),
                dropsCount, commandsCount));

            showCustomDropsDebugInfo(sbMain);
        }
    }

    private void parseCustomDrops2(final List<?> itemConfigurations) {
        if (itemConfigurations == null) {
            return;
        }

        for (final Object itemObject : itemConfigurations) {
            if (itemObject instanceof final String materialName) {
                // just the string was given
                final CustomDropItem item = new CustomDropItem(this.defaults);

                if ("override".equalsIgnoreCase(materialName)) {
                    dropInstance.overrideStockDrops = true;
                    continue;
                }

                addMaterialToDrop(materialName, item);
                continue;
            }
            final ConfigurationSection itemConfiguration = objectToConfigurationSection_old(
                itemObject);
            if (itemConfiguration == null) {
                continue;
            }

            final Set<Map.Entry<String, Object>> itemsToCheck = itemConfiguration.getValues(false)
                .entrySet();

            if (itemsToCheck.isEmpty() && itemObject.getClass().equals(LinkedHashMap.class)) {
                // empty list means a material name was provided with no attributes
                final LinkedHashMap<String, Object> materials = (LinkedHashMap<String, Object>) itemObject;
                boolean needsContinue = false;
                for (final String materialName : materials.keySet()) {
                    final CustomDropItem item = new CustomDropItem(this.defaults);

                    if (addMaterialToDrop(materialName, item)) {
                        needsContinue = true;
                        break;
                    }
                }
                if (needsContinue) {
                    continue;
                }
            }

            for (final Map.Entry<String, Object> itemEntry : itemsToCheck) {
                final String materialName = itemEntry.getKey();

                if (checkForMobOverride(itemEntry)) {
                    continue;
                }

                if ("overall_chance".equalsIgnoreCase(materialName)) {
                    if (itemEntry.getValue() instanceof Number) {
                        dropInstance.overallChance = ((Number) itemEntry.getValue()).floatValue();
                    }
                    continue;
                } else if ("overall_permission".equalsIgnoreCase(materialName)) {
                    if (itemEntry.getValue() instanceof String) {
                        dropInstance.overallPermissions.add((String) itemEntry.getValue());
                    } else if (itemEntry.getValue() instanceof ArrayList) {
                        dropInstance.overallPermissions.addAll(
                            (ArrayList<String>) itemEntry.getValue());
                    }

                    continue;
                }

                if ("usedroptable".equalsIgnoreCase(materialName)) {
                    if (itemEntry.getValue() == null) {
                        Utils.logger.warning("Found a drop-table reference with no id!");
                        continue;
                    }

                    final String useEntityDropId = itemEntry.getValue().toString();

                    if (handler.customItemGroups == null || !handler.customItemGroups.containsKey(
                        useEntityDropId)) {
                        Utils.logger.warning(
                            "Did not find droptable id match for name: " + useEntityDropId);
                    } else {
                        final CustomDropInstance refDrop = handler.customItemGroups.get(
                            useEntityDropId);
                        for (final CustomDropBase itemDrop : refDrop.customItems) {
                            dropInstance.customItems.add(itemDrop instanceof CustomDropItem ?
                                ((CustomDropItem) itemDrop).cloneItem() :
                                ((CustomCommand) itemDrop).cloneItem());
                        }
                        if (refDrop.utilizesGroupIds) {
                            dropInstance.utilizesGroupIds = true;
                        }
                        if (refDrop.getOverrideStockDrops()) {
                            dropInstance.overrideStockDrops = true;
                        }
                    }
                    continue;
                }

                final ConfigurationSection itemInfoConfiguration = objectToConfigurationSection_old(
                    itemEntry.getValue());
                if (itemInfoConfiguration == null) {
                    continue;
                }

                if ("customCommand".equalsIgnoreCase(materialName)) {
                    dropBase = new CustomCommand(defaults);
                } else {
                    final CustomDropItem item = new CustomDropItem(this.defaults);
                    item.externalType = ymlHelper.getString(itemInfoConfiguration, "type",
                            this.defaults.externalType);
                    item.externalAmount = ymlHelper.getDouble2(itemInfoConfiguration,
                            "external-amount", this.defaults.externalAmount);
                    item.externalExtras = parseExternalExtras(itemInfoConfiguration);

                    if (!addMaterialToDrop(materialName, item)) {
                        continue;
                    }

                    if (item.isExternalItem && main.companion.externalCompatibilityManager.doesLMIMeetVersionRequirement()) {
                        if (!handler.lmItemsParser.getExternalItem(item, null)) {
                            continue;
                        }
                    }

                    dropBase = item;
                }

                parseCustomDropsAttributes(dropBase, itemInfoConfiguration);
            }
        } // next item
    }

    private @Nullable Map<String, Object> parseExternalExtras(final @NotNull ConfigurationSection cs){
        final ConfigurationSection cs2 = ymlHelper.objTo_CS(cs, "extras");
        if (cs2 == null) return null;

        final Map<String, Object> results = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (final String name : cs2.getKeys(false)) {
            final Object value = cs2.get(name);
            if (value != null) results.put(name, value);
        }

        if (results.isEmpty())
            return null;
        else
            return results;
    }

    private void parseCustomDropsAttributes(final @NotNull CustomDropBase dropBase,
        final @NotNull ConfigurationSection cs) {
        dropBase.chance = ymlHelper.getFloat(cs, "chance", this.defaults.chance);
        dropBase.useChunkKillMax = ymlHelper.getBoolean(cs, "use-chunk-kill-max",
            this.defaults.useChunkKillMax);
        dropBase.permissions.addAll(this.defaults.permissions);
        dropBase.permissions.addAll(ymlHelper.getStringSet(cs, "permission"));
        dropBase.minLevel = ymlHelper.getInt(cs, "minlevel", this.defaults.minLevel);
        dropBase.maxLevel = ymlHelper.getInt(cs, "maxlevel", this.defaults.maxLevel);

        dropBase.minPlayerLevel = ymlHelper.getInt(cs, "min-player-level",
            this.defaults.minPlayerLevel);
        dropBase.maxPlayerLevel = ymlHelper.getInt(cs, "max-player-level",
            this.defaults.maxPlayerLevel);
        dropBase.playerLevelVariable = ymlHelper.getString(cs, "player-level-variable",
            this.defaults.playerLevelVariable);
        dropBase.playeerVariableMatches.addAll(ymlHelper.getStringOrList(cs, "player-variable-match-value"));

        dropBase.playerCausedOnly = ymlHelper.getBoolean(cs, "player-caused",
            this.defaults.playerCausedOnly);
        dropBase.maxDropGroup = ymlHelper.getInt(cs, "maxdropgroup", this.defaults.maxDropGroup);
        if (!dropBase.isDefaultDrop){
            dropBase.groupId = ymlHelper.getString(cs, "groupid");
        }

        if (dropBase.hasGroupId()) {
            handler.setDropInstanceFromId(dropBase.groupId, dropInstance);
        }

        dropInstance.utilizesGroupIds = dropBase.hasGroupId();
        parseGroupLimits(dropBase, cs);

        if (!Utils.isNullOrEmpty(ymlHelper.getString(cs, "amount"))) {
            if (!dropBase.setAmountRangeFromString(ymlHelper.getString(cs, "amount"))) {
                Utils.logger.warning(
                    String.format("Invalid number or number range for amount on %s, %s",
                        dropInstance.getMobOrGroupName(), ymlHelper.getString(cs, "amount")));
            }
        }

        if (!Utils.isNullOrEmpty(ymlHelper.getString(cs, "overall_chance"))) {
            dropInstance.overallChance = ymlHelper.getFloat(cs, "overall_chance");
            if (dropInstance.overallChance == 0.0) {
                dropInstance.overallChance = null;
            }
        }

        if (cs.get(ymlHelper.getKeyNameFromConfig(cs, "overall_permission")) != null) {
            dropInstance.overallPermissions.addAll(
                ymlHelper.getStringSet(cs, "overall_permission"));
        }

        dropBase.causeOfDeathReqs = buildCachedModalListOfDamageCause(cs,
                this.defaults.causeOfDeathReqs);

        if (dropBase instanceof final CustomCommand customCommand) {
            parseCustomCommand(customCommand, cs);
            return;
        }

        parseCustomItem(cs, (CustomDropItem) dropBase);
    }

    private void parseCustomItem(final @NotNull ConfigurationSection cs, final @NotNull CustomDropItem item){
        checkEquippedChance(item, cs);
        parseItemFlags(item, cs);

        item.onlyDropIfEquipped = ymlHelper.getBoolean(cs, "only-drop-if-equipped",
                this.defaults.onlyDropIfEquipped);
        item.equipOnHelmet = ymlHelper.getBoolean(cs, "equip-on-helmet", this.defaults.equipOnHelmet);
        item.equipOffhand = ymlHelper.getBoolean(cs, "equip-offhand", this.defaults.equipOffhand);
        item.priority = ymlHelper.getInt(cs, "priority", this.defaults.priority);
        item.noMultiplier = ymlHelper.getBoolean(cs, "nomultiplier", this.defaults.noMultiplier);
        item.noSpawner = ymlHelper.getBoolean(cs, "nospawner", this.defaults.noSpawner);
        item.customModelDataId = ymlHelper.getInt(cs, "custommodeldata",
                this.defaults.customModelData);
        item.externalType = ymlHelper.getString(cs, "type", this.defaults.externalType);
        item.externalAmount = ymlHelper.getDouble2(cs, "external-amount",
                this.defaults.externalAmount);
        item.mobHeadTexture = ymlHelper.getString(cs, "mobhead-texture");
        final String mobHeadIdStr = ymlHelper.getString(cs, "mobhead-id");
        if (mobHeadIdStr != null) {
            try {
                item.customPlayerHeadId = UUID.fromString(mobHeadIdStr);
            } catch (final Exception e) {
                Utils.logger.warning("Invalid UUID: " + mobHeadIdStr);
            }
        }

        dropInstance.overrideStockDrops = ymlHelper.getBoolean2(cs, "override",
                this.defaults.override);

        if (!Utils.isNullOrEmpty(ymlHelper.getString(cs, "damage"))) {
            if (!item.setDamageRangeFromString(ymlHelper.getString(cs, "damage"))) {
                Utils.logger.warning(String.format("Invalid number range for damage on %s, %s",
                        dropInstance.getMobOrGroupName(), ymlHelper.getString(cs, "damage")));
            }
        }
        if (!cs.getStringList(ymlHelper.getKeyNameFromConfig(cs, "lore")).isEmpty()) {
            item.lore = cs.getStringList(ymlHelper.getKeyNameFromConfig(cs, "lore"));
        }
        item.customName = ymlHelper.getString(cs, "name", item.customName);

        if (!Utils.isNullOrEmpty(ymlHelper.getString(cs, "excludemobs"))) {
            final String[] excludes = Objects.requireNonNull(ymlHelper.getString(cs, "excludemobs"))
                    .split(";");
            item.excludedMobs.clear();
            for (final String exclude : excludes) {
                item.excludedMobs.add(exclude.trim());
            }
        }

        parseEnchantments(objectToConfigurationSection2(cs, "enchantments"), item);
        item.nbtData = ymlHelper.getString(cs, "nbt-data", this.defaults.nbtData);
        if (item.getMaterial() != Material.AIR && !Utils.isNullOrEmpty(item.nbtData)) {
            if (ExternalCompatibilityManager.hasNbtApiInstalled()) {
                final NBTApplyResult result = NBTManager.applyNBT_Data_Item(item, item.nbtData);
                if (result.hadException()) {
                    Utils.logger.warning(
                            String.format("custom drop %s for %s has invalid NBT data: %s",
                                    item.getMaterial(), dropInstance.getMobOrGroupName(),
                                    result.exceptionMessage));
                } else if (result.itemStack != null) {
                    item.setItemStack(result.itemStack);
                    this.dropsUtilizeNBTAPI = true;
                }
            } else if (!hasMentionedNBTAPI_Missing) {
                Utils.logger.warning(
                        "NBT Data has been specified in customdrops.yml but required plugin NBTAPI is not installed!");
                hasMentionedNBTAPI_Missing = true;
            }
        }

        applyMetaAttributes(item);
    }

    private void parseGroupLimits(final @NotNull CustomDropBase base, final @NotNull ConfigurationSection csParent){
        final ConfigurationSection cs = objTo_CS(csParent, "group-limits");
        if (cs == null) {
            return;
        }

        if(!base.hasGroupId()) return;

        final GroupLimits limits = new GroupLimits();
        limits.capPerItem = ymlHelper.getInt(cs, "cap-per-item");
        limits.capTotal = ymlHelper.getInt(cs, "cap-total");
        limits.capEquipped = ymlHelper.getInt(cs, "cap-equipped");
        limits.capSelect = ymlHelper.getInt(cs, "cap-select");
        limits.retries = ymlHelper.getInt(cs, "retries");

        if (!limits.isEmpty() || base.isDefaultDrop){
            handler.groupLimitsMap.put(base.groupId, limits);
        }
    }

    private void parseCustomCommand(final @NotNull CustomCommand customCommand, final @NotNull ConfigurationSection cs){
        customCommand.commands.addAll(ymlHelper.getStringOrList(cs, "command"));
        customCommand.commandName = ymlHelper.getString(cs, "name");
        customCommand.delay = ymlHelper.getInt(cs, "delay", 0);
        customCommand.runOnSpawn = ymlHelper.getBoolean(cs, "run-on-spawn", false);
        customCommand.runOnDeath = ymlHelper.getBoolean(cs, "run-on-death", true);
        parseRangedVariables(customCommand, cs);

        if (customCommand.commands.isEmpty()) {
            Utils.logger.warning("no command was specified for custom command");
        } else {
            dropInstance.customItems.add(dropBase);
        }
    }

    private @NotNull CachedModalList<DeathCause> buildCachedModalListOfDamageCause(
        final ConfigurationSection cs,
        final CachedModalList<DeathCause> defaultValue) {
        if (cs == null) {
            return defaultValue;
        }

        final CachedModalList<DeathCause> cachedModalList = new CachedModalList<>();
        final Object simpleStringOrArray = cs.get(
            ymlHelper.getKeyNameFromConfig(cs, "cause-of-death"));
        ConfigurationSection cs2 = null;
        List<String> useList = null;

        if (simpleStringOrArray instanceof ArrayList) {
            useList = new LinkedList<>((ArrayList<String>) simpleStringOrArray);
        } else if (simpleStringOrArray instanceof String) {
            useList = List.of((String) simpleStringOrArray);
        }

        if (useList == null) {
            final String useKeyName = ymlHelper.getKeyNameFromConfig(cs, "cause-of-death");

            cs2 = objTo_CS(cs, useKeyName);
        }
        if (cs2 == null && useList == null) {
            return defaultValue;
        }

        cachedModalList.doMerge = ymlHelper.getBoolean(cs2, "merge");
        if (cs2 != null) {
            final String allowedList = ymlHelper.getKeyNameFromConfig(cs2, "allowed-list");
            useList = YmlParsingHelper.getListFromConfigItem(cs2, allowedList);
        }

        for (final String item : useList) {
            if (item.trim().isEmpty()) {
                continue;
            }
            if ("*".equals(item.trim())) {
                cachedModalList.allowAll = true;
                continue;
            }
            try {
                final DeathCause cause = DeathCause.valueOf(item.trim().toUpperCase());
                cachedModalList.allowedList.add(cause);
            } catch (final IllegalArgumentException ignored) {
                Utils.logger.warning("Invalid damage cause: " + item);
            }
        }
        if (cs2 == null) {
            return cachedModalList;
        }

        final String excludedList = ymlHelper.getKeyNameFromConfig(cs2, "excluded-list");

        for (final String item : YmlParsingHelper.getListFromConfigItem(cs2, excludedList)) {
            if (item.trim().isEmpty()) {
                continue;
            }
            if ("*".equals(item.trim())) {
                cachedModalList.excludeAll = true;
                continue;
            }
            try {
                final DeathCause cause = DeathCause.valueOf(item.trim().toUpperCase());
                cachedModalList.excludedList.add(cause);
            } catch (final IllegalArgumentException ignored) {
                Utils.logger.warning("Invalid damage cause: " + item);
            }
        }

        if (cachedModalList.isEmpty() && !cachedModalList.allowAll && !cachedModalList.excludeAll) {
            return defaultValue;
        }

        return cachedModalList;
    }

    private void parseEnchantments(final @Nullable ConfigurationSection cs, final @NotNull CustomDropItem item) {
        if (cs == null) {
            return;
        }

        final Map<String, Object> enchantMap = cs.getValues(false);

        for (final Map.Entry<String, Object> enchants : enchantMap.entrySet()) {
            final String enchantName = enchants.getKey();
            final Object value = enchants.getValue();

            if (value instanceof LinkedHashMap){
                // contains enchantment chances

                final Enchantment en = Enchantment.getByKey(
                        NamespacedKey.minecraft(enchantName.toLowerCase()));

                if (en == null){
                    Utils.logger.warning("Invalid enchantment: " + enchantName);
                    continue;
                }

                final Map<Object, Object> enchantments = (Map<Object, Object>) value;
                parseEnchantmentChances(en, enchantments, item);
                continue;
            }

            int enchantLevel = 1;
            if (value != null && Utils.isInteger(value.toString())) {
                enchantLevel = Integer.parseInt(value.toString());
            }

            final Enchantment en = Enchantment.getByKey(
                NamespacedKey.minecraft(enchantName.toLowerCase()));
            if (en != null) {
                if (item.getMaterial() == Material.ENCHANTED_BOOK) {
                    final EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemStack()
                        .getItemMeta();
                    if (meta != null) {
                        meta.addStoredEnchant(en, enchantLevel, true);
                        item.getItemStack().setItemMeta(meta);
                    }
                } else {
                    item.getItemStack().addUnsafeEnchantment(en, enchantLevel);
                }
            } else {
                Utils.logger.warning("Invalid enchantment: " + enchantName);
            }
        }

    }

    private void parseEnchantmentChances(final @NotNull Enchantment enchantment,
                                         final @NotNull Map<Object, Object> enchantmentsMap,
                                         final @NotNull CustomDropItem item){
        final Map<Integer, Float> items = new TreeMap<>();
        Integer defaultLevel = null;
        Boolean doShuttle = null;

        /*
        * ENCHANTMENTS:
        *  sharpness:
        *    1: 0.4
        *    2: 0.5
        *    3: 0.6
        *    default: 1
        */

        for (final Map.Entry<Object, Object> map : enchantmentsMap.entrySet()){
            if ("shuffle".equalsIgnoreCase(map.getKey().toString())){
                if ("false".equalsIgnoreCase(map.getValue().toString()))
                    doShuttle = false;
                continue;
            }

            final boolean isDefault = defaultName.equalsIgnoreCase(map.getKey().toString());
            int enchantmentLevel = 0;

            if (!isDefault) {
                if (!Utils.isInteger(map.getKey().toString())) {
                    Utils.logger.warning(String.format("Enchantment: %s, invalid enchantment level %s",
                            enchantment, map.getKey()));
                    continue;
                }
                enchantmentLevel = Integer.parseInt(map.getKey().toString());
            }

            double chanceValue;
            try{
                chanceValue = Double.parseDouble(map.getValue().toString());
            }
            catch (Exception ignored){
                Utils.logger.warning(String.format("Enchantment: %s, invalid chance specified: %s",
                        enchantment, map.getValue()));
                continue;
            }

            if (isDefault)
                defaultLevel = (int) chanceValue;
            else
                items.put(enchantmentLevel, (float)chanceValue);
        }

        if (items.isEmpty()) return;

        if (item.enchantmentChances == null)
            item.enchantmentChances = new EnchantmentChances();

        if (doShuttle != null || defaultLevel != null) {
            final EnchantmentChances.ChanceOptions opts = item.enchantmentChances.options.computeIfAbsent(
                    enchantment, k-> new EnchantmentChances.ChanceOptions());

            if (defaultLevel != null)
                opts.defaultLevel = defaultLevel;
            if (doShuttle != null)
                opts.doShuffle = false;
        }

        item.enchantmentChances.items.put(enchantment, items);
    }

    private void parseRangedVariables(final CustomCommand cc,
        @NotNull final ConfigurationSection cs) {
        for (final String key : cs.getKeys(false)) {
            if (!key.toLowerCase().startsWith("ranged")) {
                continue;
            }

            final String value = cs.getString(key);
            if (Utils.isNullOrEmpty(value)) {
                continue;
            }

            cc.rangedEntries.put(key, value);
        }
    }

    private void applyMetaAttributes(@NotNull final CustomDropItem item) {
        final ItemMeta meta = item.getItemStack().getItemMeta();
        if (meta == null) {
            return;
        }

        boolean madeChanges = false;

        if (item.customModelDataId != this.defaults.customModelData) {
            meta.setCustomModelData(item.customModelDataId);
            item.getItemStack().setItemMeta(meta);
            madeChanges = true;
        }

        if (item.itemFlags != null && !item.itemFlags.isEmpty()) {
            for (final ItemFlag flag : item.itemFlags) {
                meta.addItemFlags(flag);
            }

            madeChanges = true;
        }

        if (madeChanges) {
            item.getItemStack().setItemMeta(meta);
        }
    }

    private void parseItemFlags(final CustomDropItem item, final ConfigurationSection cs) {
        if (cs == null) {
            return;
        }

        item.itemFlagsStrings = cs.getStringList(
            ymlHelper.getKeyNameFromConfig(cs, "item_flags"));
        if (item.itemFlagsStrings.isEmpty()) {
            item.itemFlagsStrings = cs.getStringList(
                    ymlHelper.getKeyNameFromConfig(cs, "item-flags"));
        }

        if (item.itemFlagsStrings.isEmpty() && this.defaults.itemFlagsStrings != null) {
            item.itemFlagsStrings = this.defaults.itemFlagsStrings;
        }

        String itemFlags = null;

        if (item.itemFlagsStrings.isEmpty()) {
            itemFlags = ymlHelper.getString(cs, "itemflags");
            if (Utils.isNullOrEmpty(itemFlags)) {
                itemFlags = ymlHelper.getString(cs, "item_flags");
            }
            if (Utils.isNullOrEmpty(itemFlags)) {
                itemFlags = ymlHelper.getString(cs, "item-flags");
            }
        }

        if (item.itemFlagsStrings.isEmpty() && Utils.isNullOrEmpty(itemFlags)) {
            return;
        }
        final List<ItemFlag> results = new LinkedList<>();
        item.itemFlagsStrings = item.itemFlagsStrings.isEmpty() ?
            List.of(itemFlags.replace(',', ';').split(";")) : item.itemFlagsStrings;

        for (final String flag : item.itemFlagsStrings) {
            try {
                final ItemFlag newFlag = ItemFlag.valueOf(flag.trim().toUpperCase());
                results.add(newFlag);
            } catch (final Exception e) {
                Utils.logger.warning(String.format("Invalid itemflag: %s, item: %s, mobOrGroup: %s",
                    flag, item.getMaterial().name(), dropInstance.getMobOrGroupName()));
            }
        }

        if (!results.isEmpty()) {
            item.itemFlags = results;
        }
    }

    private void checkEquippedChance(final CustomDropItem item,
        @NotNull final ConfigurationSection cs) {
        final String temp = ymlHelper.getString(cs, "equipped");
        if (Utils.isNullOrEmpty(temp)) {
            return;
        }

        if ("false".equalsIgnoreCase(temp)) {
            item.equippedSpawnChance = 0.0F;
            return;
        } else if ("true".equalsIgnoreCase(temp)) {
            item.equippedSpawnChance = 1.0F;
            return;
        }

        item.equippedSpawnChance = ymlHelper.getFloat(cs, "equipped",
            this.defaults.equippedSpawnChance);
    }

    private @Nullable ConfigurationSection objectToConfigurationSection2(final ConfigurationSection cs,
        final String path) {
        if (cs == null) {
            return null;
        }
        final String useKey = ymlHelper.getKeyNameFromConfig(cs, path);
        final Object object = cs.get(useKey);

        if (object == null) {
            return null;
        }

        if (object instanceof ConfigurationSection) {
            return (ConfigurationSection) object;
        } else if (object instanceof Map) {
            final MemoryConfiguration result = new MemoryConfiguration();
            result.addDefaults((Map<String, Object>) object);
            return result.getDefaultSection();
        } else {
            final String currentPath = Utils.isNullOrEmpty(cs.getCurrentPath()) ?
                path : cs.getCurrentPath() + "." + path;
            Utils.logger.warning(String.format(
                "%s: couldn't parse Config of type: %s, value: %s",
                    currentPath, object.getClass().getSimpleName(), object));
            return null;
        }
    }

    private @Nullable ConfigurationSection objectToConfigurationSection_old(final Object object) {
        if (object == null) {
            return null;
        }

        if (object instanceof ConfigurationSection) {
            return (ConfigurationSection) object;
        } else if (object instanceof Map) {
            final MemoryConfiguration result = new MemoryConfiguration();
            result.addDefaults((Map<String, Object>) object);
            return result.getDefaultSection();
        } else {
            Utils.logger.warning(
                "couldn't parse Config of type: " + object.getClass().getSimpleName() + ", value: "
                    + object);
            return null;
        }
    }

    private boolean addMaterialToDrop(@NotNull String materialName,
        final CustomDropItem item) {
        materialName = Utils.replaceEx(materialName, "mob_head", "player_head");
        materialName = Utils.replaceEx(materialName, "mobhead", "player_head");

        if (materialName.contains(":")) {
            // this item is referencing a custom item from an external plugin, we will call LM_Items to get it
            if (main.companion.externalCompatibilityManager.doesLMIMeetVersionRequirement()) {
                if (!handler.lmItemsParser.parseExternalItemAttributes(materialName, item)) {
                    return false;
                }
            } else {
                if (ExternalCompatibilityManager.hasLMItemsInstalled()){
                    Utils.logger.warning(String.format(
                            "Custom drop '%s' requires plugin LM_Items but it is an old version",
                            materialName));
                }
                else {
                    Utils.logger.warning(String.format(
                            "Custom drop '%s' requires plugin LM_Items but it is not installed",
                            materialName));
                }
                return false;
            }
        } else {
            final Material material;
            if ("override".equalsIgnoreCase(materialName)){
                dropInstance.overrideStockDrops = true;
                return true;
            }
            try {
                material = Material.valueOf(materialName.toUpperCase());
            } catch (final Exception e) {
                Utils.logger.warning(
                    String.format("Invalid material type specified in customdrops.yml for: %s, %s",
                        dropInstance.getMobOrGroupName(), materialName));
                return false;
            }

            item.setMaterial(material);
        }

        dropInstance.customItems.add(item);

        return true;
    }

    private boolean checkForMobOverride(final @NotNull Map.Entry<String, Object> itemEntry) {
        if (itemEntry.getKey().equalsIgnoreCase("override")) {
            final Object value = itemEntry.getValue();
            if (value.getClass().equals(Boolean.class)) {
                dropInstance.overrideStockDrops = (boolean) value;
                return true;
            }
        }

        return false;
    }

    private void showCustomDropsDebugInfo(final StringBuilder sbMain) {
        // build string list to alphabeticalize the drops by entity type including babies
        final SortedMap<String, EntityType> typeNames = new TreeMap<>();

        for (final EntityType ent : handler.getCustomDropsitems().keySet()) {
            typeNames.put(ent.toString(), ent);
        }

        for (final EntityType ent : handler.customDropsitems_Babies.keySet()) {
            typeNames.put(ent.toString() + "_2", ent);
        }

        for (final String entTypeStr : typeNames.keySet()) {
            final boolean isBaby = entTypeStr.endsWith("_2");
            final EntityType ent = EntityType.valueOf(
                isBaby ? entTypeStr.substring(0, entTypeStr.length() - 2) : entTypeStr);
            final CustomDropInstance dropInstance = isBaby ?
                handler.customDropsitems_Babies.get(ent) : handler.getCustomDropsitems().get(ent);

            final String override = dropInstance.getOverrideStockDrops() ? " (override)" : "";
            final String overallChance = dropInstance.overallChance != null ? " (overall_chance: "
                + dropInstance.overallChance + ")" : "";
            sbMain.append(System.lineSeparator());
            sbMain.append("mob: &b");
            if (isBaby) {
                sbMain.append("(baby) ");
            }
            sbMain.append(ent.name());
            sbMain.append("&r");
            sbMain.append(override);
            sbMain.append(overallChance);
            if (!dropInstance.overallPermissions.isEmpty()) {
                sbMain.append(" (overall perms: ");
                sbMain.append(dropInstance.overallPermissions);
                sbMain.append(")");
            }

            for (final CustomDropBase baseItem : dropInstance.customItems) {
                final String result = showCustomDropsDebugInfo2(baseItem);
                if (!Utils.isNullOrEmpty(result)) {
                    sbMain.append(System.lineSeparator());
                    sbMain.append(result);
                }
            }
        }

        for (final Map.Entry<String, CustomDropInstance> customDrops : handler.getCustomDropsitems_groups().entrySet()) {
            final CustomDropInstance dropInstance = customDrops.getValue();
            final String override = dropInstance.getOverrideStockDrops() ? " (override)" : "";
            final String overallChance = dropInstance.overallChance != null ? " (overall_chance: "
                + dropInstance.overallChance + ")" : "";
            if (!sbMain.isEmpty()) {
                sbMain.append(System.lineSeparator());
            }
            sbMain.append("group: ");
            sbMain.append(customDrops.getKey());
            sbMain.append(override);
            sbMain.append(overallChance);
            for (final CustomDropBase baseItem : dropInstance.customItems) {
                final String result = showCustomDropsDebugInfo2(baseItem);
                if (!Utils.isNullOrEmpty(result)) {
                    sbMain.append(System.lineSeparator());
                    sbMain.append(result);
                }
            }
        }

        Utils.logger.info(sbMain.toString());
    }

    private @NotNull String showCustomDropsDebugInfo2(final CustomDropBase baseItem) {
        final CustomCommand command = baseItem instanceof CustomCommand ?
            (CustomCommand) baseItem : null;
        final CustomDropItem item = baseItem instanceof CustomDropItem ?
            (CustomDropItem) baseItem : null;

        final StringBuilder sb = new StringBuilder();
        if (item != null) {
            final String itemMaterial =
                item.getMaterial() != null ? item.getMaterial().toString() : "(unknown)";
            sb.append(String.format("  &b%s&r, amount: &b%s&r, chance: &b%s&r", itemMaterial,
                item.getAmountAsString(), baseItem.chance));
        } else if (baseItem instanceof final CustomCommand cc) {
            sb.append(String.format("  COMMAND, chance: &b%s&r, run-on-spawn: %s, run-on-death: %s",
                    baseItem.chance, cc.runOnSpawn, cc.runOnDeath));
        }

        if (baseItem.minLevel > -1) {
            sb.append(", minL: &b");
            sb.append(baseItem.minLevel);
            sb.append("&r");
        }
        if (baseItem.maxLevel > -1) {
            sb.append(", maxL: &b");
            sb.append(baseItem.maxLevel);
            sb.append("&r");
        }

        if (baseItem.minPlayerLevel > -1) {
            sb.append(", minPL: &b");
            sb.append(baseItem.minPlayerLevel);
            sb.append("&r");
        }
        if (baseItem.maxPlayerLevel > -1) {
            sb.append(", maxPL: &b");
            sb.append(baseItem.maxPlayerLevel);
            sb.append("&r");
        }

        if (!baseItem.permissions.isEmpty()) {
            sb.append(", perms: &b");
            sb.append(baseItem.permissions);
            sb.append("&r");
        }

        if (baseItem.noSpawner) {
            sb.append(", nospn");
        }

        if (baseItem.causeOfDeathReqs != null) {
            sb.append(", ");
            sb.append(baseItem.causeOfDeathReqs);
        }

        if (baseItem.hasGroupId()) {
            sb.append(", gId: &b");
            sb.append(baseItem.groupId);
            sb.append("&r");

            if (baseItem.maxDropGroup > 0 && !handler.groupLimitsMap.containsKey(dropBase.groupId)) {
                sb.append(", maxDropGroup: &b");
                sb.append(baseItem.maxDropGroup);
                sb.append("&r");
            }
        }
        if (baseItem.priority > 0) {
            sb.append(", pri: &b");
            sb.append(baseItem.priority);
            sb.append("&r");
        }

        if (command != null) {
            if (!Utils.isNullOrEmpty(command.commandName)) {
                sb.append(", name: &b");
                sb.append(command.commandName);
                sb.append("&r");
            }

            return sb.toString();
        }

        if (item == null) {
            return sb.toString(); // this shuts up the IDE for possible null reference
        }

        if (item.noMultiplier) {
            sb.append(", nomultp");
        }
        if (item.lore != null && !item.lore.isEmpty()) {
            sb.append(", hasLore");
        }
        if (item.customName != null && !item.customName.isEmpty()) {
            sb.append(", hasName");
        }
        if (item.getDamage() != 0 || item.getHasDamageRange()) {
            sb.append(", dmg: &b");
            sb.append(item.getDamageAsString());
            sb.append("&r");
        }
        if (!item.excludedMobs.isEmpty()) {
            sb.append(", hasExcludes");
        }
        if (item.equippedSpawnChance > 0.0) {
            sb.append(", equipChance: &b");
            sb.append(item.equippedSpawnChance);
            sb.append("&r");
        }
        if (item.onlyDropIfEquipped) {
            sb.append(", &bonlyDropIfEquipped&r");
        }
        if (item.equipOnHelmet){
            sb.append(", &bequipHelmet&r");
        }
        if (item.itemFlags != null && !item.itemFlags.isEmpty()) {
            sb.append(", itemflags: &b");
            sb.append(item.itemFlags.size());
            sb.append("&r");
        }

        if (item.isExternalItem) {
            sb.append(", ext: ");
            sb.append(item.externalPluginName);

            if (item.externalType != null) {
                sb.append(", ex-type: ");
                sb.append(item.externalType);
            }
            if (item.externalItemId != null) {
                sb.append(", ex-id: ");
                sb.append(item.externalItemId);
            }
            if (item.externalAmount != null) {
                sb.append(", ex-amt: ");
                sb.append(item.externalAmount);
            }
            if (item.externalExtras != null){
                sb.append(", ex-xtras: ");
                sb.append(item.externalExtras.size());
            }
        }

        if (item.enchantmentChances != null && !item.enchantmentChances.isEmpty()){
            final StringBuilder enchantmentLevels = new StringBuilder();
            enchantmentLevels.append("encht-lvls: ");
            for (final Enchantment enchantment : item.enchantmentChances.items.keySet()){
                if (enchantmentLevels.length() > 12) enchantmentLevels.append("; ");
                enchantmentLevels.append("&b");
                enchantmentLevels.append(enchantment.getKey().value());
                enchantmentLevels.append("&r: ");
                boolean isFirst = true;
                for (final Map.Entry<Integer, Float> chances : item.enchantmentChances.items.get(enchantment).entrySet()){
                    if (!isFirst) enchantmentLevels.append(", ");
                    enchantmentLevels.append(String.format("%s-&b%s&r",
                            chances.getKey(), chances.getValue()));

                    isFirst = false;
                }

                if (item.enchantmentChances.options.containsKey(enchantment)) {
                    EnchantmentChances.ChanceOptions opts = item.enchantmentChances.options.get(enchantment);
                    if (opts.defaultLevel != null)
                        enchantmentLevels.append(", dflt: ").append(opts.defaultLevel);
                    if (!opts.doShuffle)
                        enchantmentLevels.append(", no shfl");
                }
            }

            sb.append(System.lineSeparator());
            sb.append("    ");
            sb.append(enchantmentLevels);
        }

        if (item.getItemStack() != null) {
            final ItemMeta meta = item.getItemStack().getItemMeta();
            final StringBuilder sb2 = new StringBuilder();
            if (meta != null) {
                for (final Enchantment enchant : meta.getEnchants().keySet()) {
                    if (!sb2.isEmpty()) {
                        sb2.append(", ");
                    }
                    sb2.append(String.format("&b%s&r (%s)", enchant.getKey().getKey(),
                        item.getItemStack().getItemMeta().getEnchants().get(enchant)));
                }
            }

            if (!sb2.isEmpty()) {
                sb.append(System.lineSeparator());
                sb.append("    ");
                sb.append(sb2);
            }
        }

        return sb.toString();
    }

    private @Nullable ConfigurationSection objTo_CS(final ConfigurationSection cs, final String path) {
        if (cs == null) {
            return null;
        }
        final String useKey = ymlHelper.getKeyNameFromConfig(cs, path);
        final Object object = cs.get(useKey);

        if (object == null) {
            return null;
        }

        if (object instanceof ConfigurationSection) {
            return (ConfigurationSection) object;
        } else if (object instanceof Map) {
            final MemoryConfiguration result = new MemoryConfiguration();
            result.addDefaults((Map<String, Object>) object);
            return result.getDefaultSection();
        } else {
            final String currentPath = Utils.isNullOrEmpty(cs.getCurrentPath()) ?
                path : cs.getCurrentPath() + "." + path;
            Utils.logger.warning(
                currentPath + ": couldn't parse Config of type: " + object.getClass()
                    .getSimpleName() + ", value: " + object);
            return null;
        }
    }
}
