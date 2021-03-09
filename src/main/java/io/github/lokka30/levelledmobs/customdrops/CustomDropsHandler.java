package io.github.lokka30.levelledmobs.customdrops;

import io.github.lokka30.levelledmobs.LevelledMobs;
import io.github.lokka30.levelledmobs.compatibility.MC1_16_Compat;
import io.github.lokka30.levelledmobs.misc.Addition;
import io.github.lokka30.levelledmobs.misc.Utils;
import me.lokka30.microlib.MessageUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.MemorySection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
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

public class CustomDropsHandler {
    private final LevelledMobs instance;

    public TreeMap<EntityType, CustomDropInstance> customDropsitems;
    public TreeMap<CustomDropsUniversalGroups, CustomDropInstance> customDropsitems_groups;
    public HashSet<EntityType> groups_HostileMobs;
    public HashSet<EntityType> groups_AquaticMobs;
    public HashSet<EntityType> groups_PassiveMobs;
    public HashSet<EntityType> groups_NetherMobs;

    public CustomDropsHandler(final LevelledMobs instance){
        this.instance = instance;
        this.customDropsitems = new TreeMap<>();
        this.customDropsitems_groups = new TreeMap<>();

        buildUniversalGroups();

        if (instance.settingsCfg.getBoolean("use-custom-item-drops-for-mobs") && instance.customDropsCfg != null)
            parseCustomDrops(instance.customDropsCfg);
    }

    public CustomDropResult getCustomItemDrops(final LivingEntity livingEntity, final int level, final List<ItemStack> drops, final boolean isLevellable, final boolean equippedOnly){

        final int preCount = drops.size();
        final List<CustomDropsUniversalGroups> applicableGroups = getApllicableGroupsForMob(livingEntity, isLevellable);
        final boolean isSpawner = livingEntity.getPersistentDataContainer().has(instance.levelManager.isSpawnerKey, PersistentDataType.STRING);
        CustomDropResult customDropResult = CustomDropResult.NO_OVERRIDE;

        for (final CustomDropsUniversalGroups group : applicableGroups){
            if (!customDropsitems_groups.containsKey(group)) continue;

            CustomDropInstance dropInstance = customDropsitems_groups.get(group);
            if (dropInstance.overrideStockDrops) customDropResult = CustomDropResult.HAS_OVERRIDE;

            // if we are using groupIds then shuffle the list so it doesn't potentially drop the same item each time
            if (dropInstance.utilizesGroupIds)
                Collections.shuffle(dropInstance.customItems);

            getCustomItemDrops2(livingEntity, level, dropInstance, drops, isSpawner, equippedOnly);
        }

        if (customDropsitems.containsKey(livingEntity.getType())){
            CustomDropInstance dropInstance = customDropsitems.get(livingEntity.getType());
            if (dropInstance.overrideStockDrops) customDropResult = CustomDropResult.HAS_OVERRIDE;

            // if we are using groupIds then shuffle the list so it doesn't potentially drop the same item each time
            if (dropInstance.utilizesGroupIds)
                Collections.shuffle(dropInstance.customItems);

            getCustomItemDrops2(livingEntity, level, dropInstance, drops, isSpawner, equippedOnly);
        }

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
                ArrayList<String> applicableGroupsNames = new ArrayList<>();
                applicableGroups.forEach(applicableGroup -> applicableGroupsNames.add(applicableGroup.toString()));

                Utils.logger.info("&7Custom drops for " + livingEntity.getName());
                Utils.logger.info("&8- &7Groups: &b" + String.join("&7, &b", applicableGroupsNames) + "&7.");
                Utils.logger.info(String.format("&8 --- &7Precount: &b%s&7, postcount: &b%s&7.", preCount, postCount));
            }
        }

        return customDropResult;
    }

    private void getCustomItemDrops2(final LivingEntity livingEntity, final int level, final CustomDropInstance dropInstance,
                                     final List<ItemStack> newDrops, final boolean isSpawner, final boolean equippedOnly){

        for (final CustomItemDrop drop : dropInstance.customItems){
            if (equippedOnly && !drop.isEquipped) continue;

            if (drop.excludedMobs.contains(livingEntity.getName())){
                if (!equippedOnly && instance.settingsCfg.getStringList("debug-misc").contains("custom-drops")) {
                    Utils.logger.info(String.format(
                            "&8 - &7Mob: &b%s&7, item: %s, mob was excluded", livingEntity.getName(), drop.getMaterial().name()));
                }
                continue;
            }

            boolean doDrop = true;
            if (drop.maxLevel > -1 && level > drop.maxLevel) doDrop = false;
            if (drop.minLevel > -1 && level < drop.minLevel) doDrop = false;
            if (drop.noSpawner && isSpawner)  doDrop = false;
            if (!doDrop){
                if (!equippedOnly && instance.settingsCfg.getStringList("debug-misc").contains("custom-drops")) {
                    Utils.logger.info(String.format("&8- &7Mob: &b%s&7, level: &b%s&7, fromSpawner: &b%s&7, item: &b%s&7, minL: &b%s&7, maxL: &b%s&7, nospawner: &b%s&7, dropped: &bfalse",
                            livingEntity.getName(), level, isSpawner, drop.getMaterial().name(), drop.minLevel, drop.maxLevel, drop.noSpawner));
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
                if (!drop.noMultiplier) {
                    final int addition = BigDecimal.valueOf(instance.mobDataManager.getAdditionsForLevel(livingEntity, Addition.CUSTOM_ITEM_DROP, level))
                            .setScale(0, RoundingMode.HALF_DOWN).intValueExact(); // truncate double to int
                    newDropAmount = newDropAmount + (newDropAmount * addition);
                }

                chanceRole = (double) ThreadLocalRandom.current().nextInt(0, 100001) * 0.00001;
                if (1.0 - chanceRole >= drop.dropChance) didNotMakeChance = true;
            }

            if (!equippedOnly && instance.settingsCfg.getStringList("debug-misc").contains("custom-drops")) {
                Utils.logger.info(String.format(
                        "&8 - &7Mob: &b%s&7, item: &b%s&7, amount: &b%s&7, newAmount: &b%s&7, chance: &b%s&7, chanceRole: &b%s&7, dropped: &b%s&7.",
                        livingEntity.getName(), drop.getMaterial().name(), drop.getAmountAsString(), newDropAmount, drop.dropChance, chanceRole, !didNotMakeChance)
                );
            }
            if (didNotMakeChance) continue;

            // if we made it this far then the item will be dropped
            final ItemStack newItem = drop.getItemStack().clone();

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

            if (newDropAmount > newItem.getMaxStackSize()) newDropAmount = newItem.getMaxStackSize();
            if (newDropAmount != 1) newItem.setAmount(newDropAmount);

            newDrops.add(newItem);
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
        }
        else if (le.getWorld().getEnvironment().equals(World.Environment.NETHER)){
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

    private void parseCustomDrops(final ConfigurationSection config){
        final TreeMap<String, CustomDropInstance> customItemGroups = new TreeMap<>();

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
                    try {
                        entityType = EntityType.valueOf(mobTypeOrGroup.toUpperCase());
                    } catch (Exception e) {
                        Utils.logger.warning("invalid mob type in customdrops.yml: " + mobTypeOrGroup);
                        continue;
                    }
                    dropInstance = new CustomDropInstance(entityType);
                }
                else {
                    // item groups, we processed them beforehand
                    continue;
                }

                if (!isEntityTable) {
                    if (config.getList(item) != null) {
                        // standard drop processing
                        parseCustomDrops2(config.getList(item), dropInstance);
                    }
                    else if (config.get(item) instanceof MemorySection){
                        // drop is using a item group
                        final MemorySection ms = (MemorySection) config.get(item);
                        if (ms == null) continue;

                        final String useEntityDropId = ms.getString("usedroptable");
                        if (!customItemGroups.containsKey(useEntityDropId))
                            Utils.logger.warning("Did not find droptable id match for name: " + useEntityDropId);
                        else {
                            final CustomDropInstance refDrop = customItemGroups.get(useEntityDropId);
                            for (CustomItemDrop itemDrop : refDrop.customItems)
                                dropInstance.customItems.add(itemDrop.cloneItem());

                            // process any further customizations
                            if (config.get(item) instanceof MemorySection){
                                final MemorySection ms2 = (MemorySection) config.get(item);
                                if (ms2 != null) {
                                    final Map<String, Object> values = ms2.getValues(false);
                                    final ConfigurationSection cs = objectToConfigurationSection(values);

                                    for (final CustomItemDrop itemDrop : dropInstance.customItems)
                                        parseCustomDropsAttributes(itemDrop, cs, dropInstance);
                                }
                            }
                        }
                    }
                } // end if not entity table

                if (!dropInstance.customItems.isEmpty()) {
                    if (isUniversalGroup)
                        customDropsitems_groups.put(universalGroup, dropInstance);
                    else
                        customDropsitems.put(entityType, dropInstance);
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
                final CustomItemDrop item = new CustomItemDrop();
                final String materialName = (String) itemObject;

                if ("override".equalsIgnoreCase(materialName)){
                    dropInstance.overrideStockDrops = true;
                    continue;
                }

                Material material;
                try {
                    material = Material.valueOf(materialName.toUpperCase());
                } catch (Exception e) {
                    Utils.logger.warning(String.format("Invalid material type specified in customdrops.yml for: %s, %s", dropInstance.getMobOrGroupName(), materialName));
                    continue;
                }
                item.setMaterial(material);
                dropInstance.customItems.add(item);
                continue;
            }
            final ConfigurationSection itemConfiguration = objectToConfigurationSection(itemObject);
            if (itemConfiguration == null) continue;

            for (final Map.Entry<String,Object> itemEntry : itemConfiguration.getValues(false).entrySet()) {
                final String materialName = itemEntry.getKey();
                final ConfigurationSection itemInfoConfiguration = objectToConfigurationSection(itemEntry.getValue());
                if (itemInfoConfiguration == null) {
                    continue;
                }

                final CustomItemDrop item = new CustomItemDrop();

                Material material;
                try {
                    material = Material.valueOf(materialName.toUpperCase());
                } catch (Exception e) {
                    Utils.logger.warning(String.format("Invalid material type specified in customdrops.yml for: %s, %s", dropInstance.getMobOrGroupName(), materialName));
                    continue;
                }

                item.setMaterial(material);
                dropInstance.customItems.add(item);

                parseCustomDropsAttributes(item, itemInfoConfiguration, dropInstance);
            }
        } // next item
    }

    private void parseCustomDropsAttributes(final CustomItemDrop item, final ConfigurationSection itemInfoConfiguration, final CustomDropInstance dropInstance){
        if (!Utils.isNullOrEmpty(itemInfoConfiguration.getString("amount"))) {
            if (!item.setAmountRangeFromString(itemInfoConfiguration.getString("amount")))
                Utils.logger.warning(String.format("Invalid number or number range for amount on %s, %s", dropInstance.getMobOrGroupName(), itemInfoConfiguration.getString("amount")));
        }
        if (itemInfoConfiguration.getDouble("chance", -1.0) > -1.0)
            item.dropChance = itemInfoConfiguration.getDouble("chance");
        if (itemInfoConfiguration.getInt("minlevel", -1) > -1)
            item.minLevel = itemInfoConfiguration.getInt("minlevel");
        if (itemInfoConfiguration.getInt("maxlevel", -1) > -1)
            item.maxLevel = itemInfoConfiguration.getInt("maxlevel");
        if (itemInfoConfiguration.getInt("groupid", -1) > -1) {
            item.groupId = itemInfoConfiguration.getInt("groupid");
            dropInstance.utilizesGroupIds = true;
        }
        if (!Utils.isNullOrEmpty(itemInfoConfiguration.getString("damage"))) {
            if (!item.setDamageRangeFromString(itemInfoConfiguration.getString("damage")))
                Utils.logger.warning(String.format("Invalid number range for damage on %s, %s", dropInstance.getMobOrGroupName(), itemInfoConfiguration.getString("damage")));
        }
        if (!itemInfoConfiguration.getStringList("lore").isEmpty())
            item.lore = itemInfoConfiguration.getStringList("lore");
        if (itemInfoConfiguration.getBoolean("nomultiplier"))
            item.noMultiplier = itemInfoConfiguration.getBoolean("nomultiplier");
        if (itemInfoConfiguration.getBoolean("nospawner"))
            item.noSpawner = itemInfoConfiguration.getBoolean("nospawner");
        if (!Utils.isNullOrEmpty(itemInfoConfiguration.getString("name")))
            item.customName = itemInfoConfiguration.getString("name");
        if (itemInfoConfiguration.getBoolean("equipped"))
            item.isEquipped = itemInfoConfiguration.getBoolean("equipped");
        if (itemInfoConfiguration.getBoolean("override"))
            dropInstance.overrideStockDrops = true;
        if (!Utils.isNullOrEmpty(itemInfoConfiguration.getString("excludemobs"))) {
            String[] excludes = Objects.requireNonNull(itemInfoConfiguration.getString("excludemobs")).split(";");
            item.excludedMobs.clear();
            for (final String exclude : excludes)
                item.excludedMobs.add(exclude.trim());
        }
        if (itemInfoConfiguration.getInt("custommodeldata", -1) > -1)
            item.customModelDataId = itemInfoConfiguration.getInt("custommodeldata");

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

        if (item.customModelDataId > -1){
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

    private void showCustomDropsDebugInfo(){
        for (final EntityType ent : customDropsitems.keySet()) {
            final String override = customDropsitems.get(ent).overrideStockDrops ? " (override)" : "";
            Utils.logger.info("mob: " + ent.name() + override);
            for (final CustomItemDrop item : customDropsitems.get(ent).customItems) {
                showCustomDropsDebugInfo2(item);
            }
        }

        for (final CustomDropsUniversalGroups group : customDropsitems_groups.keySet()) {
            final String override = customDropsitems_groups.get(group).overrideStockDrops ? " (override)" : "";
            Utils.logger.info("group: " + group.name() + override);
            for (final CustomItemDrop item : customDropsitems_groups.get(group).customItems) {
                showCustomDropsDebugInfo2(item);
            }
        }
    }

    private void showCustomDropsDebugInfo2(final CustomItemDrop item){
        String msg = String.format("    %s, amount: %s, chance: %s, minL: %s, maxL: %s",
                item.getMaterial(), item.getAmountAsString(), item.dropChance, item.minLevel, item.maxLevel);

        if (item.noMultiplier) msg += ", nomultp";
        if (item.noSpawner) msg += ", nospn";
        if (item.lore != null && !item.lore.isEmpty()) msg += ", hasLore";
        if (item.customName != null && !"".equals(item.customName)) msg += ", hasName";
        if (item.getDamage() != 0 || item.getHasDamageRange())
            msg += ", dmg: " + item.getDamageAsString();
        if (!item.excludedMobs.isEmpty()) msg += ", hasExcludes";

        final StringBuilder sb = new StringBuilder();
        final ItemMeta meta = item.getItemStack().getItemMeta();
        if (meta != null) {
            for (final Enchantment enchant : meta.getEnchants().keySet()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(String.format("%s (%s)", enchant.getKey().getKey(), item.getItemStack().getItemMeta().getEnchants().get(enchant)));
            }
        }
        Utils.logger.info(msg);
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

        if (instance.isMCVersion_16_OrHigher)
            groups_HostileMobs.addAll(MC1_16_Compat.getHostileMobs());

        // include interfaces: Animals, WaterMob
        groups_PassiveMobs = Stream.of(
                EntityType.IRON_GOLEM,
                EntityType.SNOWMAN
        ).collect(Collectors.toCollection(HashSet::new));

        if (instance.isMCVersion_16_OrHigher)
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
