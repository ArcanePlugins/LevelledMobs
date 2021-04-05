package me.lokka30.levelledmobs.customdrops;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.compatibility.MC1_16_Compat;
import me.lokka30.levelledmobs.misc.Addition;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.microlib.MessageUtils;
import me.lokka30.microlib.VersionUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.MemorySection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author stumper66
 */
public class CustomDropsHandler {
    private final LevelledMobs instance;

    public final TreeMap<EntityType, CustomDropInstance> customDropsitems;
    public final TreeMap<CustomDropsUniversalGroups, CustomDropInstance> customDropsitems_groups;
    public HashSet<EntityType> groups_HostileMobs;
    public HashSet<EntityType> groups_AquaticMobs;
    public HashSet<EntityType> groups_PassiveMobs;
    public HashSet<EntityType> groups_NetherMobs;
    public final CustomDropsDefaults defaults;

    public CustomDropsHandler(final LevelledMobs instance) {
        this.instance = instance;
        this.customDropsitems = new TreeMap<>();
        this.customDropsitems_groups = new TreeMap<>();
        this.defaults = new CustomDropsDefaults();

        buildUniversalGroups();

        if (instance.settingsCfg.getBoolean("use-custom-item-drops-for-mobs"))
            parseCustomDrops(instance.customDropsCfg);
    }

    public CustomDropResult getCustomItemDrops(final LivingEntity livingEntity, final int level, final List<ItemStack> drops, final boolean isLevellable, final boolean equippedOnly){

        final List<CustomDropsUniversalGroups> applicableGroups = getApllicableGroupsForMob(livingEntity, isLevellable);
        final boolean isSpawner = livingEntity.getPersistentDataContainer().has(instance.levelManager.isSpawnerKey, PersistentDataType.STRING);
        //CustomDropResult customDropResult = CustomDropResult.NO_OVERRIDE;
        final CustomDropProcessingInfo processingInfo = new CustomDropProcessingInfo();
        processingInfo.livingEntity = livingEntity;
        processingInfo.equippedOnly = equippedOnly;
        processingInfo.isSpawner = isSpawner;
        processingInfo.level = level;
        processingInfo.newDrops = drops;
        processingInfo.addition = BigDecimal.valueOf(instance.mobDataManager.getAdditionsForLevel(livingEntity, Addition.CUSTOM_ITEM_DROP, level))
                .setScale(0, RoundingMode.HALF_DOWN).intValueExact(); // truncate double to int

        processingInfo.doNotMultiplyDrops =
                (Utils.isBabyMob(livingEntity) && instance.configUtils.noDropMultiplierEntities.contains("BABY_" + livingEntity.getType().toString())) ||
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

        final List<CustomDropsUniversalGroups> groupsList = new LinkedList<>();
        for (final CustomDropsUniversalGroups group : applicableGroups){
            if (!customDropsitems_groups.containsKey(group)) continue;

            groupsList.add(group);
        }

        buildDropsListFromGroupsAndEntity(groupsList, livingEntity.getType(), processingInfo);
        getCustomItemDrops2(processingInfo); // payload

        final int postCount = drops.size();

        if (instance.settingsCfg.getStringList("debug-misc").contains("custom-drops")) {
            if (equippedOnly && !drops.isEmpty()){
                Utils.logger.info("&7Custom equipment for " + livingEntity.getName());
                StringBuilder sb = new StringBuilder();
                for (final ItemStack drop : drops) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(drop.getType().name());
                }
                Utils.logger.info("   " + sb.toString());
            } else if (!equippedOnly) {
                Utils.logger.info(String.format("&8 --- &7Custom items added: &b%s&7.", postCount));
            }
        }

        return processingInfo.hasOverride ?
                CustomDropResult.HAS_OVERRIDE : CustomDropResult.NO_OVERRIDE;
    }

    private void buildDropsListFromGroupsAndEntity(final List<CustomDropsUniversalGroups> groups, final EntityType entityType, final CustomDropProcessingInfo processingInfo){
        final List<CustomItemDrop> drops = new LinkedList<>();
        boolean usesGroupIds = false;
        boolean hasOverride = false;

        for (CustomDropsUniversalGroups group : groups){
            CustomDropInstance dropInstance = customDropsitems_groups.get(group);
            drops.addAll(dropInstance.customItems);
            if (dropInstance.utilizesGroupIds) usesGroupIds = true;
            if (dropInstance.overrideStockDrops) hasOverride = true;
        }

        if (customDropsitems.containsKey(entityType)){
            CustomDropInstance dropInstance = customDropsitems.get(entityType);
            drops.addAll(dropInstance.customItems);
            if (dropInstance.utilizesGroupIds) usesGroupIds = true;
            if (dropInstance.overrideStockDrops) hasOverride = true;
        }

        if (usesGroupIds)
            Collections.shuffle(drops);

        if (entityType.equals(EntityType.PIG)){
            StringBuilder sb = new StringBuilder();
            for (CustomItemDrop item : drops){
                if (sb.length() > 0) sb.append(", ");
                sb.append(item.getMaterial().name());
            }
        }

        processingInfo.combinedDrops = drops;
        processingInfo.hasOverride = hasOverride;
    }

    private void getCustomItemDrops2(final CustomDropProcessingInfo info){

        for (final CustomItemDrop drop : info.combinedDrops){
            if (info.equippedOnly && !drop.isEquipped) continue;

            if (drop.excludedMobs.contains(info.livingEntity.getName())){
                if (!info.equippedOnly && instance.settingsCfg.getStringList("debug-misc").contains("custom-drops")) {
                    Utils.logger.info(String.format(
                            "&8 - &7Mob: &b%s&7, item: %s, mob was excluded", info.livingEntity.getName(), drop.getMaterial().name()));
                }
                continue;
            }

            final boolean hasGroupId = !Utils.isNullOrEmpty(drop.groupId);
            if (hasGroupId && info.groupIDsProcessed.contains(drop.groupId)) continue;

            boolean doDrop = true;
            if (drop.maxLevel > -1 && info.level > drop.maxLevel) doDrop = false;
            if (drop.minLevel > -1 && info.level < drop.minLevel) doDrop = false;
            if (drop.noSpawner && info.isSpawner)  doDrop = false;
            if (!doDrop){
                if (!info.equippedOnly && instance.settingsCfg.getStringList("debug-misc").contains("custom-drops")) {
                    final ItemStack itemStack = info.deathByFire ? getCookedVariantOfMeat(drop.getItemStack()) : drop.getItemStack();
                    Utils.logger.info(String.format("&8- &7level: &b%s&7, fromSpawner: &b%s&7, item: &b%s&7, minL: &b%s&7, maxL: &b%s&7, nospawner: &b%s&7, dropped: &bfalse",
                            info.level, info.isSpawner, itemStack.getType().name(), drop.minLevel, drop.maxLevel, drop.noSpawner));
                }
                continue;
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
            if (didNotMakeChance) continue;
            // if we made it this far then the item will be dropped

            final ItemStack newItem = info.deathByFire ?
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

            if (hasGroupId) info.groupIDsProcessed.add(drop.groupId);

            info.newDrops.add(newItem);
        }
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

    @Nonnull
    private List<CustomDropsUniversalGroups> getApllicableGroupsForMob(final LivingEntity le, final boolean isLevelled){
        final List<CustomDropsUniversalGroups> groups = new ArrayList<>();
        groups.add(CustomDropsUniversalGroups.ALL_MOBS);

        if (isLevelled) groups.add(CustomDropsUniversalGroups.ALL_LEVELLABLE_MOBS);
        final EntityType eType = le.getType();

        if (le instanceof Monster || le instanceof Boss || groups_HostileMobs.contains(eType)){
            groups.add(CustomDropsUniversalGroups.ALL_HOSTILE_MOBS);
        }

        if (le instanceof WaterMob || groups_AquaticMobs.contains(eType)){
            groups.add(CustomDropsUniversalGroups.ALL_AQUATIC_MOBS);
        }

        if (le.getWorld().getEnvironment().equals(World.Environment.NORMAL)){
            groups.add(CustomDropsUniversalGroups.ALL_OVERWORLD_MOBS);
        } else if (le.getWorld().getEnvironment().equals(World.Environment.NETHER)){
            groups.add(CustomDropsUniversalGroups.ALL_NETHER_MOBS);
        }

        if (le instanceof Flying || eType.equals(EntityType.PARROT) || eType.equals(EntityType.BAT)){
            groups.add(CustomDropsUniversalGroups.ALL_FLYING_MOBS);
        }

        // why bats aren't part of Flying interface is beyond me
        if (!(le instanceof Flying) && !(le instanceof WaterMob) && !(le instanceof Boss) && !(eType.equals(EntityType.BAT))){
            groups.add(CustomDropsUniversalGroups.ALL_GROUND_MOBS);
        }

        if (le instanceof WaterMob || groups_AquaticMobs.contains(eType)){
            groups.add(CustomDropsUniversalGroups.ALL_AQUATIC_MOBS);
        }

        if (le instanceof Animals || le instanceof WaterMob || groups_PassiveMobs.contains(eType)){
            groups.add(CustomDropsUniversalGroups.ALL_PASSIVE_MOBS);
        }

        return groups;
    }

    private void processDefaults(MemorySection ms){
        Map<String, Object> vals = ms.getValues(false);
        ConfigurationSection cs = objectToConfigurationSection(vals);

        if (cs == null){
            Utils.logger.warning("Unable to process defaults, cs was null");
            return;
        }

        // configure bogus items so we can utilize the existing attribute parse logic
        CustomItemDrop drop = new CustomItemDrop(this.defaults);
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
                    if (!dropInstance.customItems.isEmpty())
                        customItemGroups.put(itemGroupName, dropInstance);
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

                CustomDropsUniversalGroups universalGroup = null;
                final boolean isEntityTable = (mobTypeOrGroup.equalsIgnoreCase("drop-table"));
                final boolean isUniversalGroup = mobTypeOrGroup.toLowerCase().startsWith("all_");
                CustomDropInstance dropInstance;

                if (isUniversalGroup) {
                    try {
                        universalGroup = CustomDropsUniversalGroups.valueOf(mobTypeOrGroup.toUpperCase());
                    } catch (Exception e) {
                        Utils.logger.warning("invalid universal group in customdrops.yml: " + mobTypeOrGroup);
                        continue;
                    }
                    dropInstance = new CustomDropInstance(universalGroup);
                } else if (!isEntityTable) {
                    if (mobTypeOrGroup.equalsIgnoreCase("defaults")){
//                        final Object msTemp = config.get(item);
//                        if (msTemp != null) processDefaults((MemorySection) msTemp);
                        continue;
                    }
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
                            for (CustomItemDrop itemDrop : refDrop.customItems)
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
                final CustomItemDrop item = new CustomItemDrop(this.defaults);
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
                    final CustomItemDrop item = new CustomItemDrop(this.defaults);
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

                final CustomItemDrop item = new CustomItemDrop(this.defaults);
                addMaterialToDrop(materialName, dropInstance, item);

                parseCustomDropsAttributes(item, itemInfoConfiguration, dropInstance);
            }
        } // next item
    }

    private void parseCustomDropsAttributes(final CustomItemDrop item, final ConfigurationSection itemInfoConfiguration, final CustomDropInstance dropInstance){
        if (!Utils.isNullOrEmpty(itemInfoConfiguration.getString("amount"))) {
            if (!item.setAmountRangeFromString(itemInfoConfiguration.getString("amount")))
                Utils.logger.warning(String.format("Invalid number or number range for amount on %s, %s", dropInstance.getMobOrGroupName(), itemInfoConfiguration.getString("amount")));
        }

        item.dropChance = itemInfoConfiguration.getDouble("chance", this.defaults.chance);
        item.minLevel = itemInfoConfiguration.getInt("minlevel", this.defaults.minLevel);
        item.maxLevel = itemInfoConfiguration.getInt("maxlevel", this.defaults.maxLevel);
        item.isEquipped = itemInfoConfiguration.getBoolean("equipped", this.defaults.equipped);
        item.noMultiplier = itemInfoConfiguration.getBoolean("nomultiplier", this.defaults.noMultiplier);
        item.noSpawner = itemInfoConfiguration.getBoolean("nospawner", this.defaults.noSpawner);
        item.customModelDataId = itemInfoConfiguration.getInt("custommodeldata", this.defaults.customModelData);

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

        // set item attributes, etc here:

        if (item.lore != null && !item.lore.isEmpty()){
            final ItemMeta meta = item.getItemStack().getItemMeta();
            if (meta != null) {
                meta.setLore(Utils.colorizeAllInList(item.lore));
                item.getItemStack().setItemMeta(meta);
            }
        }

        if (item.customName != null && !"".equals(item.customName)){
            final ItemMeta meta = item.getItemStack().getItemMeta();
            if (meta != null) {
                meta.setDisplayName(MessageUtils.colorizeAll(item.customName));
                item.getItemStack().setItemMeta(meta);
            }
        }

        if (item.customModelDataId != this.defaults.customModelData){
            final ItemMeta meta = item.getItemStack().getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(item.customModelDataId);
                item.getItemStack().setItemMeta(meta);
            }
        }
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
            Utils.logger.warning("couldn't parse Config of type: " + object.getClass().getSimpleName() + ", value: " + object.toString());
            return null;
        }
    }

    private void addMaterialToDrop(final String materialName, CustomDropInstance dropInstance, CustomItemDrop item){
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
            for (final CustomItemDrop item : dropInstance.customItems) {
                showCustomDropsDebugInfo2(item);
            }
        }

        for (final CustomDropsUniversalGroups group : customDropsitems_groups.keySet()) {
            final CustomDropInstance dropInstance = customDropsitems_groups.get(group);
            final String override = dropInstance.overrideStockDrops ? " (override)" : "";
            Utils.logger.info("group: " + group.name() + override);
            for (final CustomItemDrop item : dropInstance.customItems) {
                showCustomDropsDebugInfo2(item);
            }
        }
    }

    private void showCustomDropsDebugInfo2(final CustomItemDrop item){
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
        if (item.isEquipped) sb.append(", equip");
        if (!Utils.isNullOrEmpty(item.groupId)) sb.append(", gId");

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

        if (sb.length() > 0) Utils.logger.info("         " + sb.toString());
    }

    private void buildUniversalGroups(){

        // include interfaces: Monster, Boss
        groups_HostileMobs = Stream.of(
                EntityType.ENDER_DRAGON,
                EntityType.GHAST,
                EntityType.MAGMA_CUBE,
                EntityType.PHANTOM,
                EntityType.SHULKER,
                EntityType.SLIME
        ).collect(Collectors.toCollection(HashSet::new));

        if (VersionUtils.isOneSixteen())
            groups_HostileMobs.addAll(MC1_16_Compat.getHostileMobs());

        // include interfaces: Animals, WaterMob
        groups_PassiveMobs = Stream.of(
                EntityType.IRON_GOLEM,
                EntityType.SNOWMAN
        ).collect(Collectors.toCollection(HashSet::new));

        if (VersionUtils.isOneSixteen())
            groups_HostileMobs.addAll(MC1_16_Compat.getPassiveMobs());

        // include interfaces: WaterMob
        groups_AquaticMobs = Stream.of(
                EntityType.DROWNED,
                EntityType.ELDER_GUARDIAN,
                EntityType.GUARDIAN,
                EntityType.TURTLE
        ).collect(Collectors.toCollection(HashSet::new));
    }
}
