package me.lokka30.levelledmobs.customdrops;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.CustomUniversalGroups;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.levelledmobs.rules.RuleInfo;
import me.lokka30.microlib.MessageUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CustomDropsParser {
    public CustomDropsParser(final LevelledMobs main, final CustomDropsHandler handler){
        this.main = main;
        this.defaults = new CustomDropsDefaults();
        this.handler = handler;
    }

    private final LevelledMobs main;
    public final CustomDropsDefaults defaults;
    private final CustomDropsHandler handler;

    public void loadDrops(final YamlConfiguration customDropsCfg){
        if (customDropsCfg == null) return;

        boolean isDropsEnabledForAnyRule = false;

        for (final List<RuleInfo> rules : main.rulesManager.rulesInEffect.values()){
            for (final RuleInfo ruleInfo : rules) {
                if (ruleInfo.customDrops_UseForMobs != null && ruleInfo.customDrops_UseForMobs) {
                    isDropsEnabledForAnyRule = true;
                    break;
                }
            }
            if (isDropsEnabledForAnyRule) break;
        }

        if (isDropsEnabledForAnyRule)
            parseCustomDrops(customDropsCfg);
    }

    private void processDefaults(@NotNull MemorySection ms){
        Map<String, Object> vals = ms.getValues(false);
        ConfigurationSection cs = objectToConfigurationSection(vals);

        if (cs == null){
            Utils.logger.warning("Unable to process defaults, cs was null");
            return;
        }

        // configure bogus items so we can utilize the existing attribute parse logic
        CustomDropItem drop = new CustomDropItem(this.defaults);
        drop.setMaterial(Material.AIR);
        CustomDropInstance dropInstance = new CustomDropInstance(EntityType.AREA_EFFECT_CLOUD);
        dropInstance.customItems.add(drop);

        // this sets the drop and dropinstance defaults
        parseCustomDropsAttributes(drop, cs, dropInstance);

        // now we'll use the attributes here for defaults
        this.defaults.setDefaultsFromDropItem(drop);
        this.defaults.override = dropInstance.overrideStockDrops;
    }

    private void parseCustomDrops(final ConfigurationSection config){
        if (config == null) return;

        handler.customItemGroups = new TreeMap<>();
        final Object defaultObj = config.get("defaults");

        if (defaultObj != null && defaultObj.getClass().equals(MemorySection.class)){
            processDefaults((MemorySection) defaultObj);
        }

        if (config.get("drop-table") != null) {
            final MemorySection ms = (MemorySection) config.get("drop-table");
            if (ms != null) {
                final Map<String, Object> itemGroups = ms.getValues(true);

                for (final String itemGroupName : itemGroups.keySet()) {
                    final CustomDropInstance dropInstance = new CustomDropInstance(EntityType.AREA_EFFECT_CLOUD); // entity type doesn't matter
                    parseCustomDrops2((List<?>) itemGroups.get(itemGroupName), dropInstance);
                    if (!dropInstance.customItems.isEmpty()) {
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
                if ("".equals(mobTypeOrGroup)) continue;
                if (mobTypeOrGroup.startsWith("file-version")) continue;

                CustomUniversalGroups universalGroup = null;
                final boolean isEntityTable = (mobTypeOrGroup.equalsIgnoreCase("drop-table"));
                final boolean isUniversalGroup = mobTypeOrGroup.toLowerCase().startsWith("all_");
                CustomDropInstance dropInstance;

                if (isUniversalGroup) {
                    try {
                        universalGroup = CustomUniversalGroups.valueOf(mobTypeOrGroup.toUpperCase());
                    } catch (Exception e) {
                        Utils.logger.warning("invalid universal group in customdrops.yml: " + mobTypeOrGroup);
                        continue;
                    }
                    dropInstance = new CustomDropInstance(universalGroup);
                } else if (!isEntityTable) {
                    if (mobTypeOrGroup.equalsIgnoreCase("defaults"))
                        continue;

                    try {
                        entityType = EntityType.valueOf(mobTypeOrGroup.toUpperCase());
                    } catch (Exception e) {
                        Utils.logger.warning("invalid mob type in customdrops.yml: " + mobTypeOrGroup);
                        continue;
                    }
                    dropInstance = new CustomDropInstance(entityType);
                } else {
                    // item groups, we processed them beforehand
                    continue;
                }

                dropInstance.overrideStockDrops = this.defaults.override;

                if (!isEntityTable) {
                    if (config.getList(item) != null) {
                        // standard drop processing
                        parseCustomDrops2(config.getList(item), dropInstance);
                    } else if (config.get(item) instanceof MemorySection){
                        // drop is using a item group
                        final MemorySection ms = (MemorySection) config.get(item);
                        if (ms == null) continue;

                        final String useEntityDropId = ms.getString("usedroptable");
                        if (useEntityDropId != null && !handler.customItemGroups.containsKey(useEntityDropId))
                            Utils.logger.warning("Did not find droptable id match for name: " + useEntityDropId);
                        else if (useEntityDropId == null)
                            Utils.logger.warning("Found a drop-table reference with no id!");
                        else {
                            final CustomDropInstance refDrop = handler.customItemGroups.get(useEntityDropId);
                            for (CustomDropBase itemDrop : refDrop.customItems)
                                dropInstance.customItems.add(itemDrop instanceof CustomDropItem ?
                                        ((CustomDropItem) itemDrop).cloneItem() :
                                        ((CustomCommand) itemDrop).cloneItem());
                            if (refDrop.utilizesGroupIds) dropInstance.utilizesGroupIds = true;
                            if (refDrop.overrideStockDrops) dropInstance.overrideStockDrops = true;
                        }
                    }
                } // end if not entity table

                if (!dropInstance.customItems.isEmpty()) {
                    if (isUniversalGroup) {
                        if (handler.customDropsitems_groups.containsKey(universalGroup))
                            handler.customDropsitems_groups.get(universalGroup).combineDrop(dropInstance);
                        else
                            handler.customDropsitems_groups.put(universalGroup, dropInstance);
                    }
                    else {
                        if (handler.customDropsitems.containsKey(entityType))
                            handler.customDropsitems.get(entityType).combineDrop(dropInstance);
                        else
                            handler.customDropsitems.put(entityType, dropInstance);
                    }
                }
            } // next mob or group
        } // next root item from file

        if (main.settingsCfg.getStringList("debug-misc").contains("CUSTOM_DROPS")) {
            Utils.logger.info(String.format("custom drops: %s, custom groups: %s, item groups: %s",
                    handler.customDropsitems.size(), handler.customDropsitems_groups.size(), handler.customItemGroups.size()));

            showCustomDropsDebugInfo();
        }
    }

    private void parseCustomDrops2(final List<?> itemConfigurations, final CustomDropInstance dropInstance){
        if (itemConfigurations == null) return;

        for (final Object itemObject : itemConfigurations) {
            if (itemObject instanceof String) {
                // just the string was given
                final CustomDropItem item = new CustomDropItem(this.defaults);
                final String materialName = (String) itemObject;

                if ("override".equalsIgnoreCase(materialName)){
                    dropInstance.overrideStockDrops = true;
                    continue;
                }

                addMaterialToDrop(materialName, dropInstance, item);
                continue;
            }
            final ConfigurationSection itemConfiguration = objectToConfigurationSection(itemObject);
            if (itemConfiguration == null) continue;

            final Set<Map.Entry<String, Object>> ItemsToCheck = itemConfiguration.getValues(false).entrySet();

            if (ItemsToCheck.isEmpty() && itemObject.getClass().equals(LinkedHashMap.class)){
                // empty list means a material name was provided with no attributes
                final LinkedHashMap<String, Object> materials = (LinkedHashMap<String, Object>) itemObject;
                for (final String materialName :  materials.keySet()){
                    final CustomDropItem item = new CustomDropItem(this.defaults);
                    addMaterialToDrop(materialName, dropInstance, item);
                }
            }

            for (final Map.Entry<String,Object> itemEntry : ItemsToCheck) {
                final String materialName = itemEntry.getKey();

                if (checkForMobOverride(itemEntry, dropInstance)) continue;

                if ("overall_chance".equalsIgnoreCase(materialName)){
                    if (itemEntry.getValue() instanceof Double)
                        dropInstance.overallChance = (Double) itemEntry.getValue();
                    else if (itemEntry.getValue() instanceof Integer)
                        dropInstance.overallChance =  Double.valueOf((Integer) itemEntry.getValue());

                    continue;
                }

                if ("usedroptable".equalsIgnoreCase(materialName)) {
                    if (itemEntry.getValue() == null){
                        Utils.logger.warning("Found a drop-table reference with no id!");
                        continue;
                    }

                    final String useEntityDropId = itemEntry.getValue().toString();

                    if (handler.customItemGroups == null || !handler.customItemGroups.containsKey(useEntityDropId))
                        Utils.logger.warning("Did not find droptable id match for name: " + useEntityDropId);
                    else {
                        final CustomDropInstance refDrop = handler.customItemGroups.get(useEntityDropId);
                        for (CustomDropBase itemDrop : refDrop.customItems)
                            dropInstance.customItems.add(itemDrop instanceof CustomDropItem ?
                                    ((CustomDropItem) itemDrop).cloneItem() :
                                    ((CustomCommand) itemDrop).cloneItem());
                        if (refDrop.utilizesGroupIds) dropInstance.utilizesGroupIds = true;
                        if (refDrop.overrideStockDrops) dropInstance.overrideStockDrops = true;
                    }
                    continue;
                }

                final ConfigurationSection itemInfoConfiguration = objectToConfigurationSection(itemEntry.getValue());
                if (itemInfoConfiguration == null) continue;

                CustomDropBase dropBase;
                if ("customCommand".equalsIgnoreCase(materialName))
                    dropBase = new CustomCommand();
                else {
                    final CustomDropItem item = new CustomDropItem(this.defaults);
                    if (!addMaterialToDrop(materialName, dropInstance, item)) continue;
                    dropBase = item;
                }

                parseCustomDropsAttributes(dropBase, itemInfoConfiguration, dropInstance);
            }
        } // next item
    }

    private void parseCustomDropsAttributes(@NotNull final CustomDropBase dropBase, @NotNull final ConfigurationSection cs, final CustomDropInstance dropInstance){
        dropBase.chance = cs.getDouble("chance", this.defaults.chance);
        dropBase.minLevel = cs.getInt("minlevel", this.defaults.minLevel);
        dropBase.maxLevel = cs.getInt("maxlevel", this.defaults.maxLevel);
        dropBase.playerCausedOnly = cs.getBoolean("player-caused", this.defaults.playerCausedOnly);
        dropBase.maxDropGroup = cs.getInt("maxdropgroup", this.defaults.maxDropGroup);

        if (!Utils.isNullOrEmpty(cs.getString("groupid"))) {
            dropBase.groupId = cs.getString("groupid");
            dropInstance.utilizesGroupIds = true;
        }

        if (dropBase instanceof CustomCommand) {
            CustomCommand customCommand = (CustomCommand) dropBase;
            customCommand.command = cs.getString("command");
            customCommand.commandName = cs.getString("name");

            if (Utils.isNullOrEmpty(customCommand.command))
                Utils.logger.warning("no command was specified for custom command");
            else
                dropInstance.customItems.add(dropBase);
            return;
        }

        final CustomDropItem item = (CustomDropItem) dropBase;

        if (!Utils.isNullOrEmpty(cs.getString("amount"))) {
            if (!item.setAmountRangeFromString(cs.getString("amount")))
                Utils.logger.warning(String.format("Invalid number or number range for amount on %s, %s", dropInstance.getMobOrGroupName(), cs.getString("amount")));
        }

        checkEquippedChance(item, cs);
        parseItemFlags(item, cs.getString("itemflags"), dropInstance);
        item.priority = cs.getInt("priority", this.defaults.priority);
        item.noMultiplier = cs.getBoolean("nomultiplier", this.defaults.noMultiplier);
        item.noSpawner = cs.getBoolean("nospawner", this.defaults.noSpawner);
        item.customModelDataId = cs.getInt("custommodeldata", this.defaults.customModelData);

        if (!Utils.isNullOrEmpty(cs.getString("override")))
            dropInstance.overrideStockDrops = cs.getBoolean("override");

        if (!Utils.isNullOrEmpty(cs.getString("damage"))) {
            if (!item.setDamageRangeFromString(cs.getString("damage")))
                Utils.logger.warning(String.format("Invalid number range for damage on %s, %s", dropInstance.getMobOrGroupName(), cs.getString("damage")));
        }
        if (!cs.getStringList("lore").isEmpty())
            item.lore = cs.getStringList("lore");
        if (!Utils.isNullOrEmpty(cs.getString("name")))
            item.customName = cs.getString("name");

        if (!Utils.isNullOrEmpty(cs.getString("excludemobs"))) {
            String[] excludes = Objects.requireNonNull(cs.getString("excludemobs")).split(";");
            item.excludedMobs.clear();
            for (final String exclude : excludes)
                item.excludedMobs.add(exclude.trim());
        }

        final Object enchantmentsSection = cs.get("enchantments");
        if (enchantmentsSection != null){
            final ConfigurationSection enchantments = objectToConfigurationSection(enchantmentsSection);
            if (enchantments != null) {
                final Map<String, Object> enchantMap = enchantments.getValues(false);
                for (final String enchantName : enchantMap.keySet()) {
                    final Object value = enchantMap.get(enchantName);

                    int enchantLevel = 1;
                    if (value != null && Utils.isInteger(value.toString()))
                        enchantLevel = Integer.parseInt(value.toString());

                    final Enchantment en = Enchantment.getByKey(NamespacedKey.minecraft(enchantName.toLowerCase()));
                    if (en != null)
                        item.getItemStack().addUnsafeEnchantment(en, enchantLevel);
                    else
                        Utils.logger.warning("Invalid enchantment: " + enchantName);
                }
            }
        } // end enchantments

        applyMetaAttributes(item);
    }

    private void applyMetaAttributes(@NotNull final CustomDropItem item){
        final ItemMeta meta = item.getItemStack().getItemMeta();
        if (meta == null) return;

        boolean madeChanges = false;

        if (item.lore != null && !item.lore.isEmpty()){
            meta.setLore(Utils.colorizeAllInList(item.lore));
            item.getItemStack().setItemMeta(meta);
            madeChanges = true;
        }

        if (item.customName != null && !"".equals(item.customName)){
            meta.setDisplayName(MessageUtils.colorizeAll(item.customName));
            item.getItemStack().setItemMeta(meta);
            madeChanges = true;
        }

        if (item.customModelDataId != this.defaults.customModelData){
            meta.setCustomModelData(item.customModelDataId);
            item.getItemStack().setItemMeta(meta);
            madeChanges = true;
        }

        if (item.itemFlags != null && !item.itemFlags.isEmpty()){
            for (final ItemFlag flag : item.itemFlags)
                meta.addItemFlags(flag);

            madeChanges = true;
        }

        if (madeChanges)
            item.getItemStack().setItemMeta(meta);
    }

    private void parseItemFlags(final CustomDropItem item, final String itemFlags, final CustomDropInstance dropInstance){
        if (Utils.isNullOrEmpty(itemFlags)) return;
        List<ItemFlag> flagList = new LinkedList<>();

        for (final String flag : itemFlags.replace(',',';').split(";")){
            try{
                ItemFlag newFlag = ItemFlag.valueOf(flag.trim().toUpperCase());
                flagList.add(newFlag);
            }
            catch (Exception e){
                Utils.logger.warning(String.format("Invalid itemflag: %s, item: %s, mobOrGroup: %s",
                        flag, item.getMaterial().name(), dropInstance.getMobOrGroupName()));
            }
        }

        if (flagList.size() > 0) item.itemFlags = flagList;
    }

    private void checkEquippedChance(final CustomDropItem item, @NotNull final ConfigurationSection itemInfoConfiguration){
        final String temp = itemInfoConfiguration.getString("equipped");
        if (Utils.isNullOrEmpty(temp)) return;

        if ("false".equalsIgnoreCase(temp)){
            item.equippedSpawnChance = 0.0;
            return;
        }
        else if ("true".equalsIgnoreCase(temp)){
            item.equippedSpawnChance = 1.0;
            return;
        }

        item.equippedSpawnChance = itemInfoConfiguration.getDouble("equipped", this.defaults.equippedSpawnChance);
    }

    private ConfigurationSection objectToConfigurationSection(final Object object){
        if (object == null) return null;

        if (object instanceof ConfigurationSection) {
            return (ConfigurationSection) object;
        } else if (object instanceof Map){
            final MemoryConfiguration result = new MemoryConfiguration();
            result.addDefaults((Map<String, Object>) object);
            return result.getDefaultSection();
        } else {
            Utils.logger.warning("couldn't parse Config of type: " + object.getClass().getSimpleName() + ", value: " + object);
            return null;
        }
    }

    private boolean addMaterialToDrop(String materialName, CustomDropInstance dropInstance, CustomDropItem item){

        materialName = Utils.replaceEx(materialName, "mob_head", "player_head");
        materialName = Utils.replaceEx(materialName, "mobhead", "player_head");

        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (Exception e) {
            Utils.logger.warning(String.format("Invalid material type specified in customdrops.yml for: %s, %s", dropInstance.getMobOrGroupName(), materialName));
            return false;
        }

        item.setMaterial(material);
        dropInstance.customItems.add(item);

        return true;
    }

    private boolean checkForMobOverride(@NotNull final Map.Entry<String,Object> itemEntry, CustomDropInstance dropInstance){
        if (itemEntry.getKey().equalsIgnoreCase("override")){
            final Object value = itemEntry.getValue();
            if (value.getClass().equals(Boolean.class)) {
                dropInstance.overrideStockDrops = (boolean) value;
                return true;
            }
        }

        return false;
    }

    private void showCustomDropsDebugInfo(){
        for (final EntityType ent : handler.customDropsitems.keySet()) {
            final CustomDropInstance dropInstance = handler.customDropsitems.get(ent);
            final String override = dropInstance.overrideStockDrops ? " (override)" : "";
            final String overallChance = dropInstance.overallChance != null ? " (overall_chance: " + dropInstance.overallChance + ")" : "";
            Utils.logger.info("mob: " + ent.name() + override + overallChance);
            for (final CustomDropBase baseItem : dropInstance.customItems) {
                showCustomDropsDebugInfo2(baseItem);
            }
        }

        for (final CustomUniversalGroups group : handler.customDropsitems_groups.keySet()) {
            final CustomDropInstance dropInstance = handler.customDropsitems_groups.get(group);
            final String override = dropInstance.overrideStockDrops ? " (override)" : "";
            final String overallChance = dropInstance.overallChance != null ? " (overall_chance: " + dropInstance.overallChance + ")" : "";
            Utils.logger.info("group: " + group.name() + override + overallChance);
            for (final CustomDropBase baseItem : dropInstance.customItems) {
                showCustomDropsDebugInfo2(baseItem);
            }
        }
    }

    private void showCustomDropsDebugInfo2(final CustomDropBase baseItem){
        final CustomCommand command = baseItem instanceof CustomCommand ?
                (CustomCommand) baseItem : null;
        final CustomDropItem item = baseItem instanceof CustomDropItem ?
                (CustomDropItem) baseItem : null;

        final StringBuilder sb = new StringBuilder();
        if (item != null)
            sb.append(String.format("    %s, amount: %s, chance: %s", item.getMaterial(), item.getAmountAsString(), baseItem.chance));
        else
            sb.append(String.format("    custom command, chance: %s", baseItem.chance));

        if (baseItem.minLevel > -1) {
            sb.append(", minL: ");
            sb.append(baseItem.minLevel);
        }
        if (baseItem.maxLevel > -1) {
            sb.append(", maxL: ");
            sb.append(baseItem.maxLevel);
        }

        if (baseItem.noSpawner) sb.append(", nospn");

        if (command != null){
            if (!Utils.isNullOrEmpty(command.commandName)) {
                sb.append(", name: ");
                sb.append(command.commandName);
            }

            Utils.logger.info(sb.toString());
            return;
        }

        if (item == null) return; // this shuts up the IDE for possible null reference

        if (item.noMultiplier) sb.append(", nomultp");
        if (item.lore != null && !item.lore.isEmpty()) sb.append(", hasLore");
        if (item.customName != null && !"".equals(item.customName)) sb.append(", hasName");
        if (item.getDamage() != 0 || item.getHasDamageRange()) {
            sb.append(", dmg: ");
            sb.append(item.getDamageAsString());
        }
        if (!item.excludedMobs.isEmpty()) sb.append(", hasExcludes");
        if (item.equippedSpawnChance > 0.0) {
            sb.append(", equipChance: ");
            sb.append(item.equippedSpawnChance);
        }
        if (!Utils.isNullOrEmpty(item.groupId)) {
            sb.append(", gId: ");
            sb.append(item.groupId);
            if (item.maxDropGroup > 0){
                sb.append(", maxDropGroup: ");
                sb.append(item.maxDropGroup);
            }
        }
        if (item.priority > 0) {
            sb.append(", pri: ");
            sb.append(item.priority);
        }
        if (item.itemFlags != null && !item.itemFlags.isEmpty()){
            sb.append(", itemflags: ");
            sb.append(item.itemFlags.size());
        }

        Utils.logger.info(sb.toString());
        sb.setLength(0);

        final ItemMeta meta = item.getItemStack().getItemMeta();
        if (meta != null) {
            boolean isFirst = true;
            for (final Enchantment enchant : meta.getEnchants().keySet()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(String.format("%s (%s)", enchant.getKey().getKey(), item.getItemStack().getItemMeta().getEnchants().get(enchant)));
            }
        }

        if (sb.length() > 0) Utils.logger.info("         " + sb);
    }
}
