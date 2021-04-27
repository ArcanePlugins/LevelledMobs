package me.lokka30.levelledmobs.customdrops;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.Addition;
import me.lokka30.levelledmobs.misc.CustomUniversalGroups;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.microlib.MessageUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.MemorySection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The main CustomDropsclass that holds useful functions for
 * parsing, instantizing and more of custom drop items
 *
 * @author stumper66
 */
public class CustomDropsHandler {
    private final LevelledMobs instance;

    public final TreeMap<EntityType, CustomDropInstance> customDropsitems;
    public final TreeMap<CustomUniversalGroups, CustomDropInstance> customDropsitems_groups;
    public final TreeMap<String, CustomDropInstance> customDropIDs;
    public final CustomDropsDefaults defaults;

    public CustomDropsHandler(final LevelledMobs instance) {
        this.instance = instance;
        this.customDropsitems = new TreeMap<>();
        this.customDropsitems_groups = new TreeMap<>();
        this.customDropIDs = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.defaults = new CustomDropsDefaults();

        if (instance.settingsCfg.getBoolean("use-custom-item-drops-for-mobs"))
            parseCustomDrops(instance.customDropsCfg);
    }

    public CustomDropResult getCustomItemDrops(final LivingEntity livingEntity, final int level, final List<ItemStack> drops, final boolean isLevellable, final boolean equippedOnly) {

        final List<CustomUniversalGroups> applicableGroups = instance.companion.getApllicableGroupsForMob(livingEntity, isLevellable, true);

        boolean isSpawner = false;
        if (livingEntity.getPersistentDataContainer().has(instance.levelManager.spawnReasonKey, PersistentDataType.STRING)) {
            //noinspection ConstantConditions
            isSpawner = livingEntity.getPersistentDataContainer().get(instance.levelManager.spawnReasonKey, PersistentDataType.STRING).equals(CreatureSpawnEvent.SpawnReason.SPAWNER.toString());
        }

        //CustomDropResult customDropResult = CustomDropResult.NO_OVERRIDE;
        final CustomDropProcessingInfo processingInfo = new CustomDropProcessingInfo();
        processingInfo.livingEntity = livingEntity;
        processingInfo.equippedOnly = equippedOnly;
        processingInfo.isSpawner = isSpawner;
        processingInfo.level = level;
        processingInfo.newDrops = drops;
        processingInfo.wasKilledByPlayer = livingEntity.getKiller() != null;
        processingInfo.addition = BigDecimal.valueOf(instance.mobDataManager.getAdditionsForLevel(livingEntity, Addition.CUSTOM_ITEM_DROP, level))
                .setScale(0, RoundingMode.HALF_DOWN).intValueExact(); // truncate double to int
        if (equippedOnly && livingEntity.getPersistentDataContainer().has(instance.blockPlaceListener.keySpawner_CustomDropId, PersistentDataType.STRING)){
            processingInfo.customDropId = livingEntity.getPersistentDataContainer().get(instance.blockPlaceListener.keySpawner_CustomDropId, PersistentDataType.STRING);
            processingInfo.hasCustomDropId = !Utils.isNullOrEmpty(processingInfo.customDropId);
        }

        processingInfo.doNotMultiplyDrops =
                (Utils.isBabyMob(livingEntity) && instance.configUtils.noDropMultiplierEntities.contains("BABY_" + livingEntity.getType())) ||
                        instance.configUtils.noDropMultiplierEntities.contains(livingEntity.getType().toString());

        if (livingEntity.getLastDamageCause() != null){
            final EntityDamageEvent.DamageCause damageCause = livingEntity.getLastDamageCause().getCause();
            processingInfo.deathByFire = (damageCause == EntityDamageEvent.DamageCause.FIRE ||
                    damageCause == EntityDamageEvent.DamageCause.FIRE_TICK ||
                    damageCause == EntityDamageEvent.DamageCause.LAVA);
        }

        if (!equippedOnly && instance.settingsCfg.getStringList("debug-misc").contains("custom-drops")) {
            List<String> applicableGroupsNames = new LinkedList<>();
            applicableGroups.forEach(applicableGroup -> applicableGroupsNames.add(applicableGroup.toString()));

            String mobLevel = level > 0 ? " (level " + level + ")" : "";
            Utils.logger.info("&7Custom drops for " + livingEntity.getName() + mobLevel);
            Utils.logger.info("&8- &7Groups: &b" + String.join("&7, &b", applicableGroupsNames) + "&7.");
        }

        final List<CustomUniversalGroups> groupsList = new LinkedList<>();
        for (final CustomUniversalGroups group : applicableGroups){
            if (!customDropsitems_groups.containsKey(group)) continue;

            groupsList.add(group);
        }

        buildDropsListFromGroupsAndEntity(groupsList, livingEntity.getType(), processingInfo);
        getCustomItemsFromDropInstance(processingInfo); // payload

        final int postCount = drops.size();

        if (instance.settingsCfg.getStringList("debug-misc").contains("custom-drops")) {
            if (equippedOnly && !drops.isEmpty()){
                if (level > -1)
                    Utils.logger.info(String.format("&7Custom equipment for %s (%s)", livingEntity.getName(), level));
                else
                    Utils.logger.info("&7Custom equipment for " + livingEntity.getName());
                StringBuilder sb = new StringBuilder();
                for (final ItemStack drop : drops) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(drop.getType().name());
                }
                Utils.logger.info("   " + sb);
            } else if (!equippedOnly) {
                Utils.logger.info(String.format("&8 --- &7Custom items added: &b%s&7.", postCount));
            }
        }

        return processingInfo.hasOverride ?
                CustomDropResult.HAS_OVERRIDE : CustomDropResult.NO_OVERRIDE;
    }

    private void buildDropsListFromGroupsAndEntity(final List<CustomUniversalGroups> groups, final EntityType entityType, final CustomDropProcessingInfo processingInfo){
        //final List<CustomDropItem> drops = new LinkedList<>();
        final Map<Integer, List<CustomDropItem>> drops = new HashMap<>();
        boolean usesGroupIds = false;
        boolean hasOverride = false;

        for (CustomUniversalGroups group : groups){
            CustomDropInstance dropInstance = customDropsitems_groups.get(group);
            //drops.addAll(dropInstance.customItems);
            for (final CustomDropItem item : dropInstance.customItems){
                final int priority = -item.priority;
                if (drops.containsKey(priority))
                    drops.get(priority).add(item);
                else {
                    final List<CustomDropItem> items = new LinkedList<>();
                    items.add(item);
                    drops.put(priority, items);
                }
            }
            if (dropInstance.utilizesGroupIds) usesGroupIds = true;
            if (dropInstance.overrideStockDrops) hasOverride = true;
        }

        if (customDropsitems.containsKey(entityType)){
            CustomDropInstance dropInstance = customDropsitems.get(entityType);
            for (final CustomDropItem item : dropInstance.customItems){
                final int priority = -item.priority;
                if (drops.containsKey(priority))
                    drops.get(priority).add(item);
                else {
                    final List<CustomDropItem> items = new LinkedList<>();
                    items.add(item);
                    drops.put(priority, items);
                }
            }
            if (dropInstance.utilizesGroupIds) usesGroupIds = true;
            if (dropInstance.overrideStockDrops) hasOverride = true;
        }

        if (usesGroupIds){
            for (final int pri : drops.keySet())
                Collections.shuffle(drops.get(pri));
        }

        processingInfo.prioritizedDrops = drops;
        processingInfo.hasOverride = hasOverride;
    }

    private void getCustomItemsFromDropInstance(final CustomDropProcessingInfo info){

        if (info.equippedOnly && info.hasCustomDropId){
            if (!this.customDropIDs.containsKey(info.customDropId)){
                Utils.logger.warning("custom drop id '" + info.customDropId + "' was not found in customdrops");
                return;
            }

            final CustomDropInstance instance = this.customDropIDs.get(info.customDropId);
            for (final CustomDropItem item : instance.customItems)
                getDropsFromCustomDropItem(info, item);
        }
        else {
            for (final int itemPriority : info.prioritizedDrops.keySet()) {
                final List<CustomDropItem> items = info.prioritizedDrops.get(itemPriority);

                for (final CustomDropItem drop : items)
                    getDropsFromCustomDropItem(info, drop);
            }
        }
    }

    private void getDropsFromCustomDropItem(final CustomDropProcessingInfo info, final CustomDropItem drop){

        if (info.equippedOnly && drop.equippedSpawnChance <= 0.0) return;
        if (!info.equippedOnly && drop.dropOnlyWhenKilledByPlayer && !info.wasKilledByPlayer) return;

        if (drop.excludedMobs.contains(info.livingEntity.getName())){
            if (!info.equippedOnly && instance.settingsCfg.getStringList("debug-misc").contains("custom-drops")) {
                Utils.logger.info(String.format(
                        "&8 - &7Mob: &b%s&7, item: %s, mob was excluded", info.livingEntity.getName(), drop.getMaterial().name()));
            }
            return;
        }

        final boolean hasGroupId = !Utils.isNullOrEmpty(drop.groupId);
        if (!info.equippedOnly && hasGroupId){
            int count = 0;
            if (info.groupIDsDroppedAlready.containsKey(drop.groupId))
                count = info.groupIDsDroppedAlready.get(drop.groupId);

            if (drop.maxDropGroup > 0 && count >= drop.maxDropGroup || drop.maxDropGroup == 0 && count > 0){
                if (instance.settingsCfg.getStringList("debug-misc").contains("custom-drops")) {
                    Utils.logger.info(String.format("&8- &7level: &b%s&7, item: &b%s&7, gId: &b%s&7, maxDropGroup: &b%s&7, groupDropCount: &b%s&7, dropped: &bfalse",
                            info.level, drop.getMaterial().name(), drop.groupId, drop.maxDropGroup, count));
                }
                return;
            }
        }

        boolean doDrop = true;

        if (info.equippedOnly && drop.equippedSpawnChance < 1.0) {
            final double chanceRole = (double) ThreadLocalRandom.current().nextInt(0, 100001) * 0.00001;
            if (1.0 - chanceRole >= drop.equippedSpawnChance){
                if (instance.settingsCfg.getStringList("debug-misc").contains("custom-drops")) {
                    Utils.logger.info(String.format("&8- Mob: &b%s&7, &7level: &b%s&7, item: &b%s&7, spawnchance: &b%s&7, chancerole: &b%s&7, did not make spawn chance",
                            info.livingEntity.getName(), info.level, drop.getMaterial().name(), drop.equippedSpawnChance, chanceRole));
                }
                return;
            }
        }

        if (drop.maxLevel > -1 && info.level > drop.maxLevel) doDrop = false;
        if (drop.minLevel > -1 && info.level < drop.minLevel) doDrop = false;
        if (drop.noSpawner && info.isSpawner)  doDrop = false;
        if (!doDrop){
            if (!info.equippedOnly && instance.settingsCfg.getStringList("debug-misc").contains("custom-drops")) {
                final ItemStack itemStack = info.deathByFire ? getCookedVariantOfMeat(drop.getItemStack()) : drop.getItemStack();
                Utils.logger.info(String.format("&8- &7level: &b%s&7, fromSpawner: &b%s&7, item: &b%s&7, minL: &b%s&7, maxL: &b%s&7, nospawner: &b%s&7, dropped: &bfalse",
                        info.level, info.isSpawner, itemStack.getType().name(), drop.minLevel, drop.maxLevel, drop.noSpawner));
            }
            return;
        }

        int newDropAmount = drop.getAmount();
        if (drop.getHasAmountRange()){
            final int change = ThreadLocalRandom.current().nextInt(0, drop.getAmountRangeMax() - drop.getAmountRangeMin() + 1);
            newDropAmount = drop.getAmountRangeMin() + change;
        }

        boolean didNotMakeChance = false;
        double chanceRole = 0.0;

        if (drop.dropChance < 1.0){
            chanceRole = (double) ThreadLocalRandom.current().nextInt(0, 100001) * 0.00001;
            if (1.0 - chanceRole >= drop.dropChance) didNotMakeChance = true;
        }

        if (didNotMakeChance && !info.equippedOnly && instance.settingsCfg.getStringList("debug-misc").contains("custom-drops")) {
            if (instance.settingsCfg.getStringList("debug-misc").contains("custom-drops")){
                final ItemStack itemStack = info.deathByFire ? getCookedVariantOfMeat(drop.getItemStack()) : drop.getItemStack();
                Utils.logger.info(String.format(
                        "&8 - &7item: &b%s&7, amount: &b%s&7, chance: &b%s&7, chanceRole: &b%s&7, dropped: &bfalse&7.",
                        itemStack.getType().name(), drop.getAmountAsString(), drop.dropChance, chanceRole)
                );
            }
        }
        if (didNotMakeChance) return;
        // if we made it this far then the item will be dropped

        ItemStack newItem = info.deathByFire ?
                getCookedVariantOfMeat(drop.getItemStack().clone()) :
                drop.getItemStack().clone();

        newItem.setAmount(newDropAmount);

        if (!drop.noMultiplier && !info.doNotMultiplyDrops) {
            instance.levelManager.multiplyDrop(info.livingEntity, newItem, info.addition, true);
            newDropAmount = newItem.getAmount();
        }
        else if (newDropAmount > newItem.getMaxStackSize()) newDropAmount = newItem.getMaxStackSize();

        if (newItem.getAmount() != newDropAmount) newItem.setAmount(newDropAmount);

        if (instance.settingsCfg.getStringList("debug-misc").contains("custom-drops")){
            Utils.logger.info(String.format(
                    "&8 - &7item: &b%s&7, amount: &b%s&7, newAmount: &b%s&7, chance: &b%s&7, chanceRole: &b%s&7, dropped: &btrue&7.",
                    newItem.getType().name(), drop.getAmountAsString(), newDropAmount, drop.dropChance, chanceRole));
        }

        int damage = drop.getDamage();
        if (drop.getHasDamageRange())
            damage = ThreadLocalRandom.current().nextInt(drop.getDamageRangeMin(), drop.getDamageRangeMax() + 1);

        if (damage > 0){
            ItemMeta meta = newItem.getItemMeta();
            if (meta instanceof Damageable){
                ((Damageable) meta).setDamage(damage);
                newItem.setItemMeta(meta);
            }
        }

        if (!info.equippedOnly && hasGroupId){
            final int count = info.groupIDsDroppedAlready.containsKey(drop.groupId) ?
                    info.groupIDsDroppedAlready.get(drop.groupId) + 1:
                    1;

            info.groupIDsDroppedAlready.put(drop.groupId, count);
        }

        if (newItem.getType().equals(Material.PLAYER_HEAD))
            newItem = instance.mobHeadManager.getMobHeadFromPlayerHead(newItem, info.livingEntity);

        info.newDrops.add(newItem);
    }

    private ItemStack getCookedVariantOfMeat(final ItemStack itemStack){
        switch (itemStack.getType()){
            case BEEF:
                return new ItemStack(Material.COOKED_BEEF);
            case CHICKEN:
                return new ItemStack(Material.COOKED_CHICKEN);
            case COD:
                return new ItemStack(Material.COOKED_COD);
            case MUTTON:
                return new ItemStack(Material.COOKED_MUTTON);
            case PORKCHOP:
                return new ItemStack(Material.COOKED_PORKCHOP);
            case SALMON:
                return new ItemStack(Material.COOKED_SALMON);
            default:
                return itemStack;
        }
    }

    private void processDefaults(MemorySection ms){
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
        final TreeMap<String, CustomDropInstance> customItemGroups = new TreeMap<>();

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
                        customItemGroups.put(itemGroupName, dropInstance);
                        this.customDropIDs.put(itemGroupName, dropInstance);
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
                        if (useEntityDropId != null && !customItemGroups.containsKey(useEntityDropId))
                            Utils.logger.warning("Did not find droptable id match for name: " + useEntityDropId);
                        else if (useEntityDropId == null)
                            Utils.logger.warning("Found a drop-table reference with no id!");
                        else {
                            final CustomDropInstance refDrop = customItemGroups.get(useEntityDropId);
                            for (CustomDropItem itemDrop : refDrop.customItems)
                                dropInstance.customItems.add(itemDrop.cloneItem());
                            if (refDrop.utilizesGroupIds) dropInstance.utilizesGroupIds = true;
                            if (refDrop.overrideStockDrops) dropInstance.overrideStockDrops = true;
                        }
                    }
                } // end if not entity table

                if (!dropInstance.customItems.isEmpty()) {
                    if (isUniversalGroup) {
                        if (customDropsitems_groups.containsKey(universalGroup))
                            customDropsitems_groups.get(universalGroup).combineDrop(dropInstance);
                        else
                            customDropsitems_groups.put(universalGroup, dropInstance);
                    }
                    else {
                        if (customDropsitems.containsKey(entityType))
                            customDropsitems.get(entityType).combineDrop(dropInstance);
                        else
                            customDropsitems.put(entityType, dropInstance);
                    }
                }
            } // next mob or group
        } // next root item from file

        if (instance.settingsCfg.getStringList("debug-misc").contains("custom-drops")) {
            Utils.logger.info(String.format("custom drops: %s, custom groups: %s, item groups: %s",
                    customDropsitems.size(), customDropsitems_groups.size(), customItemGroups.size()));

            showCustomDropsDebugInfo();
        }
    }

    private void parseCustomDrops2(final List<?> itemConfigurations, final CustomDropInstance dropInstance){

        if (itemConfigurations == null) {
            return;
        }

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

                final ConfigurationSection itemInfoConfiguration = objectToConfigurationSection(itemEntry.getValue());
                if (itemInfoConfiguration == null) {
                    continue;
                }

                final CustomDropItem item = new CustomDropItem(this.defaults);
                addMaterialToDrop(materialName, dropInstance, item);

                parseCustomDropsAttributes(item, itemInfoConfiguration, dropInstance);
            }
        } // next item
    }

    private void parseCustomDropsAttributes(final CustomDropItem item, final ConfigurationSection itemInfoConfiguration, final CustomDropInstance dropInstance){
        if (!Utils.isNullOrEmpty(itemInfoConfiguration.getString("amount"))) {
            if (!item.setAmountRangeFromString(itemInfoConfiguration.getString("amount")))
                Utils.logger.warning(String.format("Invalid number or number range for amount on %s, %s", dropInstance.getMobOrGroupName(), itemInfoConfiguration.getString("amount")));
        }

        item.dropChance = itemInfoConfiguration.getDouble("chance", this.defaults.chance);
        checkEquippedChance(item, itemInfoConfiguration);
        parseItemFlags(item, itemInfoConfiguration.getString("itemflags"), dropInstance);
        item.priority = itemInfoConfiguration.getInt("priority", this.defaults.priority);
        item.maxDropGroup = itemInfoConfiguration.getInt("maxdropgroup", this.defaults.maxDropGroup);
        item.minLevel = itemInfoConfiguration.getInt("minlevel", this.defaults.minLevel);
        item.maxLevel = itemInfoConfiguration.getInt("maxlevel", this.defaults.maxLevel);
        item.noMultiplier = itemInfoConfiguration.getBoolean("nomultiplier", this.defaults.noMultiplier);
        item.noSpawner = itemInfoConfiguration.getBoolean("nospawner", this.defaults.noSpawner);
        item.customModelDataId = itemInfoConfiguration.getInt("custommodeldata", this.defaults.customModelData);
        item.dropOnlyWhenKilledByPlayer = itemInfoConfiguration.getBoolean("droponlywhenkilledbyplayer", this.defaults.dropOnlyWhenKilledByPlayer);

        if (!Utils.isNullOrEmpty(itemInfoConfiguration.getString("override")))
            dropInstance.overrideStockDrops = itemInfoConfiguration.getBoolean("override");

        if (!Utils.isNullOrEmpty(itemInfoConfiguration.getString("groupid"))) {
            item.groupId = itemInfoConfiguration.getString("groupid");
            dropInstance.utilizesGroupIds = true;
        }
        if (!Utils.isNullOrEmpty(itemInfoConfiguration.getString("damage"))) {
            if (!item.setDamageRangeFromString(itemInfoConfiguration.getString("damage")))
                Utils.logger.warning(String.format("Invalid number range for damage on %s, %s", dropInstance.getMobOrGroupName(), itemInfoConfiguration.getString("damage")));
        }
        if (!itemInfoConfiguration.getStringList("lore").isEmpty())
            item.lore = itemInfoConfiguration.getStringList("lore");
        if (!Utils.isNullOrEmpty(itemInfoConfiguration.getString("name")))
            item.customName = itemInfoConfiguration.getString("name");

        if (!Utils.isNullOrEmpty(itemInfoConfiguration.getString("excludemobs"))) {
            String[] excludes = Objects.requireNonNull(itemInfoConfiguration.getString("excludemobs")).split(";");
            item.excludedMobs.clear();
            for (final String exclude : excludes)
                item.excludedMobs.add(exclude.trim());
        }

        final Object enchantmentsSection = itemInfoConfiguration.get("enchantments");
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

    private void applyMetaAttributes(final CustomDropItem item){
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

    private void checkEquippedChance(final CustomDropItem item, final ConfigurationSection itemInfoConfiguration){
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

    private void addMaterialToDrop(String materialName, CustomDropInstance dropInstance, CustomDropItem item){

        materialName = Utils.replaceEx(materialName, "mob_head", "player_head");
        materialName = Utils.replaceEx(materialName, "mobhead", "player_head");

        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (Exception e) {
            Utils.logger.warning(String.format("Invalid material type specified in customdrops.yml for: %s, %s", dropInstance.getMobOrGroupName(), materialName));
            return;
        }

        item.setMaterial(material);
        dropInstance.customItems.add(item);
    }

    private boolean checkForMobOverride(final Map.Entry<String,Object> itemEntry, CustomDropInstance dropInstance){
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
        for (final EntityType ent : customDropsitems.keySet()) {
            final CustomDropInstance dropInstance = customDropsitems.get(ent);
            final String override = dropInstance.overrideStockDrops ? " (override)" : "";
            Utils.logger.info("mob: " + ent.name() + override);
            for (final CustomDropItem item : dropInstance.customItems) {
                showCustomDropsDebugInfo2(item);
            }
        }

        for (final CustomUniversalGroups group : customDropsitems_groups.keySet()) {
            final CustomDropInstance dropInstance = customDropsitems_groups.get(group);
            final String override = dropInstance.overrideStockDrops ? " (override)" : "";
            Utils.logger.info("group: " + group.name() + override);
            for (final CustomDropItem item : dropInstance.customItems) {
                showCustomDropsDebugInfo2(item);
            }
        }
    }

    private void showCustomDropsDebugInfo2(final CustomDropItem item){
        final StringBuilder sb = new StringBuilder();
        sb.append(String.format("    %s, amount: %s, chance: %s", item.getMaterial(), item.getAmountAsString(), item.dropChance));

        if (item.minLevel > -1) {
            sb.append(", minL: ");
            sb.append(item.minLevel);
        }
        if (item.maxLevel > -1) {
            sb.append(", maxL: ");
            sb.append(item.maxLevel);
        }
        if (item.noMultiplier) sb.append(", nomultp");
        if (item.noSpawner) sb.append(", nospn");
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
