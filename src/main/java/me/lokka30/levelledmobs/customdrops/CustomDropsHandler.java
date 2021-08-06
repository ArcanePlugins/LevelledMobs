/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.customdrops;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.*;
import me.lokka30.microlib.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    private final LevelledMobs main;

    public final TreeMap<EntityType, CustomDropInstance> customDropsitems;
    public final TreeMap<String, CustomDropInstance> customDropsitems_groups;
    public final TreeMap<String, CustomDropInstance> customDropIDs;
    @Nullable
    public Map<String, CustomDropInstance> customItemGroups;
    public final CustomDropsParser customDropsParser;
    public NamespacedKey overallChanceKey;
    private final YmlParsingHelper ymlHelper;

    public CustomDropsHandler(final LevelledMobs main) {
        this.main = main;
        this.overallChanceKey = new NamespacedKey(main, "overallChance");
        this.customDropsitems = new TreeMap<>();
        this.customDropsitems_groups = new TreeMap<>();
        this.customDropIDs = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        customDropsParser = new CustomDropsParser(main, this);
        this.ymlHelper = customDropsParser.ymlHelper;
    }

    public CustomDropResult getCustomItemDrops(final LivingEntityWrapper lmEntity, final List<ItemStack> drops, final boolean equippedOnly) {
        final CustomDropProcessingInfo processingInfo = new CustomDropProcessingInfo();
        processingInfo.lmEntity = lmEntity;
        processingInfo.equippedOnly = equippedOnly;
        processingInfo.newDrops = drops;
        processingInfo.dropRules = main.rulesManager.getRule_UseCustomDropsForMob(lmEntity);
        synchronized (lmEntity.getLivingEntity().getPersistentDataContainer()) {
            processingInfo.isSpawner = (lmEntity.getPDC().has(main.levelManager.spawnReasonKey, PersistentDataType.STRING) &&
                    CreatureSpawnEvent.SpawnReason.SPAWNER.toString().equals(
                            lmEntity.getPDC().get(main.levelManager.spawnReasonKey, PersistentDataType.STRING))
            );

            if (equippedOnly && lmEntity.getPDC().has(main.blockPlaceListener.keySpawner_CustomDropId, PersistentDataType.STRING)){
                processingInfo.customDropId = lmEntity.getPDC().get(main.blockPlaceListener.keySpawner_CustomDropId, PersistentDataType.STRING);
                processingInfo.hasCustomDropId = !Utils.isNullOrEmpty(processingInfo.customDropId);
            }
        }
        processingInfo.wasKilledByPlayer = lmEntity.getLivingEntity().getKiller() != null;
        processingInfo.addition = BigDecimal.valueOf(main.mobDataManager.getAdditionsForLevel(lmEntity, Addition.CUSTOM_ITEM_DROP, 0.0))
                .setScale(0, RoundingMode.HALF_DOWN).intValueExact(); // truncate double to int

        processingInfo.doNotMultiplyDrops = main.rulesManager.getRule_CheckIfNoDropMultiplierEntitiy(lmEntity);

        if (lmEntity.getLivingEntity().getLastDamageCause() != null){
            final EntityDamageEvent.DamageCause damageCause = lmEntity.getLivingEntity().getLastDamageCause().getCause();
            processingInfo.deathByFire = (damageCause == EntityDamageEvent.DamageCause.FIRE ||
                    damageCause == EntityDamageEvent.DamageCause.FIRE_TICK ||
                    damageCause == EntityDamageEvent.DamageCause.LAVA);
        }

        if (!equippedOnly && ymlHelper.getStringSet(main.settingsCfg,"debug-misc").contains("CUSTOM_DROPS")) {

            String mobLevel = lmEntity.getMobLevel() > 0 ? " (level " + lmEntity.getMobLevel() + ")" : "";
            Utils.logger.info("&7Custom drops for " + lmEntity.getTypeName() + mobLevel);
            Utils.logger.info("&8- &7Groups: &b" + String.join("&7, &b", lmEntity.getApplicableGroups()) + "&7.");
        }

        final List<String> groupsList = new LinkedList<>();
        for (final String group : lmEntity.getApplicableGroups()){
            if (!customDropsitems_groups.containsKey(group)) continue;

            groupsList.add(group);
        }

        if (!buildDropsListFromGroupsAndEntity(groupsList, lmEntity.getEntityType(), processingInfo)){
            // didn't make overall chance
            if (ymlHelper.getStringSet(main.settingsCfg, "debug-misc").contains("CUSTOM_DROPS")) {
                Utils.logger.info(String.format("&7%s (%s) - didn't make overall chance", lmEntity.getTypeName(), lmEntity.getMobLevel()));
            }
            return processingInfo.hasOverride ?
                    CustomDropResult.HAS_OVERRIDE : CustomDropResult.NO_OVERRIDE;
        }

        getCustomItemsFromDropInstance(processingInfo); // payload

        final int postCount = drops.size();

        if (ymlHelper.getStringSet(main.settingsCfg,"debug-misc").contains("CUSTOM_DROPS")) {
            if (equippedOnly && !drops.isEmpty()){
                if (lmEntity.getMobLevel() > -1)
                    Utils.logger.info(String.format("&7Custom equipment for %s (%s)", lmEntity.getTypeName(), lmEntity.getMobLevel()));
                else
                    Utils.logger.info("&7Custom equipment for " + lmEntity.getTypeName());
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

    private boolean buildDropsListFromGroupsAndEntity(final List<String> groups, final EntityType entityType, @NotNull final CustomDropProcessingInfo processingInfo){
        processingInfo.prioritizedDrops = new HashMap<>();
        processingInfo.hasOverride = false;
        boolean usesGroupIds = false;

        final boolean overrideNonDropTableDrops = processingInfo.dropRules != null && processingInfo.dropRules.override;
        if (processingInfo.dropRules != null && processingInfo.dropRules.useDropTableId != null){
            final String[] useIds = processingInfo.dropRules.useDropTableId.split(",");
            for (final String id : useIds){
                if (this.customItemGroups == null || !this.customItemGroups.containsKey(id.trim())){
                    Utils.logger.warning("rule specified an invalid value for use-droptable-id: " + id);
                    continue;
                }

                final CustomDropInstance dropInstance = this.customItemGroups.get(id.trim());
                processingInfo.allDropInstances.add(dropInstance);

                for (final CustomDropBase baseItem : dropInstance.customItems)
                    processDropPriorities(baseItem, processingInfo);

                if (dropInstance.utilizesGroupIds) usesGroupIds = true;
                if (dropInstance.overrideStockDrops) processingInfo.hasOverride = true;
            }
        }

        if (!overrideNonDropTableDrops) {
            for (String group : groups) {
                final CustomDropInstance dropInstance = customDropsitems_groups.get(group);
                processingInfo.allDropInstances.add(dropInstance);

                for (final CustomDropBase baseItem : dropInstance.customItems)
                    processDropPriorities(baseItem, processingInfo);

                if (dropInstance.utilizesGroupIds) usesGroupIds = true;
                if (dropInstance.overrideStockDrops) processingInfo.hasOverride = true;
            }

            if (customDropsitems.containsKey(entityType)){
                final CustomDropInstance dropInstance = customDropsitems.get(entityType);
                processingInfo.allDropInstances.add(dropInstance);

                for (final CustomDropBase baseItem : dropInstance.customItems)
                    processDropPriorities(baseItem, processingInfo);

                if (dropInstance.utilizesGroupIds) usesGroupIds = true;
                if (dropInstance.overrideStockDrops) processingInfo.hasOverride = true;
            }
        }

        if (usesGroupIds){
            for (final int pri : processingInfo.prioritizedDrops.keySet())
                Collections.shuffle(processingInfo.prioritizedDrops.get(pri));
        }

        if (processingInfo.equippedOnly && !processingInfo.hasEquippedItems) return true;
        return checkOverallChance(processingInfo);
    }

    private void processDropPriorities(@NotNull final CustomDropBase baseItem, @NotNull final CustomDropProcessingInfo processingInfo){
        final int priority = -baseItem.priority;
        if (processingInfo.prioritizedDrops.containsKey(priority))
            processingInfo.prioritizedDrops.get(priority).add(baseItem);
        else {
            final List<CustomDropBase> items = new LinkedList<>();
            items.add(baseItem);
            processingInfo.prioritizedDrops.put(priority, items);
        }

        if (baseItem instanceof CustomDropItem && ((CustomDropItem) baseItem).equippedSpawnChance > 0.0)
            processingInfo.hasEquippedItems = true;
    }

    private void getCustomItemsFromDropInstance(@NotNull final CustomDropProcessingInfo info){
        if (info.equippedOnly && info.hasCustomDropId){
            if (!this.customDropIDs.containsKey(info.customDropId)){
                Utils.logger.warning("custom drop id '" + info.customDropId + "' was not found in customdrops");
                return;
            }

            final CustomDropInstance instance = this.customDropIDs.get(info.customDropId);

            for (final CustomDropBase baseItem : instance.customItems)
                getDropsFromCustomDropItem(info, baseItem);

            return;
        }

        // non equipped items get processed below

        for (final int itemPriority : info.prioritizedDrops.keySet()) {
            final List<CustomDropBase> items = info.prioritizedDrops.get(itemPriority);

            for (final CustomDropBase drop : items)
                getDropsFromCustomDropItem(info, drop);
        }
    }

    private boolean checkOverallChance(@NotNull final CustomDropProcessingInfo info){
        for (final CustomDropInstance dropInstance : info.allDropInstances) {
            if (dropInstance.overallChance == null || dropInstance.overallChance >= 1.0 || dropInstance.overallChance <= 0.0) continue;

            synchronized (info.lmEntity.getLivingEntity().getPersistentDataContainer()) {
                if (info.lmEntity.getPDC().has(this.overallChanceKey, PersistentDataType.INTEGER)) {
                    final int value = Objects.requireNonNull(info.lmEntity.getPDC().get(this.overallChanceKey, PersistentDataType.INTEGER));
                    return value == 1;
                }
            }

            // we'll roll the dice to see if we get any drops at all and store it in the PDC
            final double chanceRole = (double) ThreadLocalRandom.current().nextInt(0, 100001) * 0.00001;
            final boolean madeChance = 1.0 - chanceRole < dropInstance.overallChance;
            if (info.equippedOnly) {
                synchronized (info.lmEntity.getLivingEntity().getPersistentDataContainer()) {
                    info.lmEntity.getPDC().set(this.overallChanceKey, PersistentDataType.INTEGER, madeChance ? 1 : 0);
                }
            }

            return madeChance;
        }

        return true;
    }

    private void getDropsFromCustomDropItem(@NotNull final CustomDropProcessingInfo info, final CustomDropBase dropBase){
        if (dropBase instanceof CustomCommand && info.lmEntity.getLivingEntity().hasMetadata("noCommands") ||
                info.lmEntity.deathCause.equals(EntityDamageEvent.DamageCause.VOID))
            return;

        if (info.equippedOnly && dropBase instanceof CustomCommand) return;
        if (info.equippedOnly && dropBase instanceof CustomDropItem && ((CustomDropItem) dropBase).equippedSpawnChance <= 0.0) return;
        if (!info.equippedOnly && dropBase.playerCausedOnly && !info.wasKilledByPlayer) return;
        if (dropBase.noSpawner && info.isSpawner) return;

        if (dropBase.excludedMobs.contains(info.lmEntity.getTypeName())){
            if (dropBase instanceof CustomDropItem && !info.equippedOnly && ymlHelper.getStringSet(main.settingsCfg, "debug-misc").contains("CUSTOM_DROPS")) {
                final CustomDropItem dropItem = (CustomDropItem)dropBase;
                Utils.logger.info(String.format(
                        "&8 - &7Mob: &b%s&7, item: %s, mob was excluded", info.lmEntity.getTypeName(), dropItem.getMaterial().name()));
            }
            return;
        }

        boolean doDrop = dropBase.maxLevel <= -1 || info.lmEntity.getMobLevel() <= dropBase.maxLevel;
        if (dropBase.minLevel > -1 && info.lmEntity.getMobLevel() < dropBase.minLevel) doDrop = false;
        if (!doDrop){
            if (dropBase instanceof CustomDropItem) {
                final CustomDropItem dropItem = (CustomDropItem) dropBase;
                if (!info.equippedOnly && ymlHelper.getStringSet(main.settingsCfg, "debug-misc").contains("CUSTOM_DROPS")) {
                    final ItemStack itemStack = info.deathByFire ? getCookedVariantOfMeat(dropItem.getItemStack()) : dropItem.getItemStack();
                    Utils.logger.info(String.format("&8- &7level: &b%s&7, fromSpawner: &b%s&7, item: &b%s&7, minL: &b%s&7, maxL: &b%s&7, nospawner: &b%s&7, dropped: &bfalse",
                            info.lmEntity.getMobLevel(), info.isSpawner, itemStack.getType().name(), dropBase.minLevel, dropBase.maxLevel, dropBase.noSpawner));
                }
            }
            else if (dropBase instanceof CustomCommand){
                final CustomCommand customCommand = (CustomCommand) dropBase;
                Utils.logger.info(String.format("&8- custom-cmd: &7level: &b%s&7, fromSpawner: &b%s&7, minL: &b%s&7, maxL: &b%s&7, nospawner: &b%s&7, executed: &bfalse",
                        info.lmEntity.getMobLevel(), info.isSpawner, dropBase.minLevel, dropBase.maxLevel, dropBase.noSpawner));
            }
            return;
        }

        boolean didNotMakeChance = false;
        double chanceRole = 0.0;

        if (!info.equippedOnly && dropBase.chance < 1.0){
            chanceRole = (double) ThreadLocalRandom.current().nextInt(0, 100001) * 0.00001;
            if (1.0 - chanceRole >= dropBase.chance) didNotMakeChance = true;
        }

        if (didNotMakeChance && !info.equippedOnly && ymlHelper.getStringSet(main.settingsCfg, "debug-misc").contains("CUSTOM_DROPS")) {
            if (dropBase instanceof CustomDropItem){
                CustomDropItem dropItem = (CustomDropItem) dropBase;
                final ItemStack itemStack = info.deathByFire ? getCookedVariantOfMeat(dropItem.getItemStack()) : dropItem.getItemStack();
                Utils.logger.info(String.format(
                        "&8 - &7item: &b%s&7, amount: &b%s&7, chance: &b%s&7, chanceRole: &b%s&7, dropped: &bfalse&7.",
                        itemStack.getType().name(), dropItem.getAmountAsString(), dropBase.chance, chanceRole)
                );
            }
        }
        if (!info.equippedOnly && didNotMakeChance) return;


        final boolean hasGroupId = !Utils.isNullOrEmpty(dropBase.groupId);
        if (!info.equippedOnly && hasGroupId){
            int count = 0;
            if (info.groupIDsDroppedAlready.containsKey(dropBase.groupId))
                count = info.groupIDsDroppedAlready.get(dropBase.groupId);

            if (dropBase.maxDropGroup > 0 && count >= dropBase.maxDropGroup || dropBase.maxDropGroup == 0 && count > 0){
                if (ymlHelper.getStringSet(main.settingsCfg, "debug-misc").contains("CUSTOM_DROPS")) {
                    if (dropBase instanceof CustomDropItem) {
                        Utils.logger.info(String.format("&8- &7level: &b%s&7, item: &b%s&7, gId: &b%s&7, maxDropGroup: &b%s&7, groupDropCount: &b%s&7, dropped: &bfalse",
                                info.lmEntity.getMobLevel(), ((CustomDropItem) dropBase).getMaterial().name(), dropBase.groupId, dropBase.maxDropGroup, count));
                    } else {
                        Utils.logger.info(String.format("&8- &7level: &b%s&7, item: custom command, gId: &b%s&7, maxDropGroup: &b%s&7, groupDropCount: &b%s&7, executed: &bfalse",
                                info.lmEntity.getMobLevel(), dropBase.groupId, dropBase.maxDropGroup, count));
                    }
                }
                return;
            }
        }

        if (dropBase instanceof CustomCommand) {
            // ------------------------------------------ commands get executed here then function returns ---------------------------------------------------
            executeCommand((CustomCommand) dropBase, info);

            if (hasGroupId){
                final int count = info.groupIDsDroppedAlready.containsKey(dropBase.groupId) ?
                        info.groupIDsDroppedAlready.get(dropBase.groupId) + 1 :
                        1;

                info.groupIDsDroppedAlready.put(dropBase.groupId, count);

                if (main.settingsCfg.getStringList("debug-misc").contains("CUSTOM_DROPS")) {
                    Utils.logger.info(String.format("&8- &7level: &b%s&7, item: custom command, gId: &b%s&7, maxDropGroup: &b%s&7, groupDropCount: &b%s&7, executed: &btrue",
                            info.lmEntity.getMobLevel(), dropBase.groupId, dropBase.maxDropGroup, count));
                }
            } else if (main.settingsCfg.getStringList("debug-misc").contains("CUSTOM_DROPS")) {
                Utils.logger.info(String.format("&8- &7level: &b%s&7, item: custom command, gId: &b%s&7, maxDropGroup: &b%s&7, executed: &btrue",
                        info.lmEntity.getMobLevel(), dropBase.groupId, dropBase.maxDropGroup));
            }

            return;
            // -----------------------------------------------------------------------------------------------------------------------------------------------
        }
        CustomDropItem dropItem = (CustomDropItem) dropBase;

        if (info.equippedOnly && dropItem.equippedSpawnChance < 1.0) {
            chanceRole = (double) ThreadLocalRandom.current().nextInt(0, 100001) * 0.00001;
            if (1.0 - chanceRole >= dropItem.equippedSpawnChance){
                if (ymlHelper.getStringSet(main.settingsCfg, "debug-misc").contains("CUSTOM_DROPS")) {
                    Utils.logger.info(String.format("&8- Mob: &b%s&7, &7level: &b%s&7, item: &b%s&7, spawnchance: &b%s&7, chancerole: &b%s&7, did not make spawn chance",
                            info.lmEntity.getTypeName(), info.lmEntity.getMobLevel(), dropItem.getMaterial().name(), dropItem.equippedSpawnChance, chanceRole));
                }
                return;
            }
        }

        int newDropAmount = dropItem.getAmount();
        if (dropItem.getHasAmountRange()){
            final int change = ThreadLocalRandom.current().nextInt(0, dropItem.getAmountRangeMax() - dropItem.getAmountRangeMin() + 1);
            newDropAmount = dropItem.getAmountRangeMin() + change;
        }

        // if we made it this far then the item will be dropped

        ItemStack newItem = info.deathByFire ?
                getCookedVariantOfMeat(dropItem.getItemStack().clone()) :
                dropItem.getItemStack().clone();

        newItem.setAmount(newDropAmount);

        if (!dropItem.noMultiplier && !info.doNotMultiplyDrops) {
            main.levelManager.multiplyDrop(info.lmEntity, newItem, info.addition, true);
            newDropAmount = newItem.getAmount();
        } else if (newDropAmount > newItem.getMaxStackSize()) newDropAmount = newItem.getMaxStackSize();

        if (newItem.getAmount() != newDropAmount) newItem.setAmount(newDropAmount);

        if (ymlHelper.getStringSet(main.settingsCfg, "debug-misc").contains("CUSTOM_DROPS")){
            Utils.logger.info(String.format(
                    "&8 - &7item: &b%s&7, amount: &b%s&7, newAmount: &b%s&7, chance: &b%s&7, chanceRole: &b%s&7, dropped: &btrue&7.",
                    newItem.getType().name(), dropItem.getAmountAsString(), newDropAmount, dropItem.chance, chanceRole));
        }

        int damage = dropItem.getDamage();
        if (dropItem.getHasDamageRange())
            damage = ThreadLocalRandom.current().nextInt(dropItem.getDamageRangeMin(), dropItem.getDamageRangeMax() + 1);

        if (damage > 0){
            ItemMeta meta = newItem.getItemMeta();
            if (meta instanceof Damageable){
                ((Damageable) meta).setDamage(damage);
                newItem.setItemMeta(meta);
            }
        }

        if (!info.equippedOnly && hasGroupId){
            final int count = info.groupIDsDroppedAlready.containsKey(dropItem.groupId) ?
                    info.groupIDsDroppedAlready.get(dropItem.groupId) + 1:
                    1;

            info.groupIDsDroppedAlready.put(dropItem.groupId, count);
        }

        if (newItem.getType().equals(Material.PLAYER_HEAD))
            newItem = main.mobHeadManager.getMobHeadFromPlayerHead(newItem, info.lmEntity, dropItem);

        info.newDrops.add(newItem);
    }

    private void executeCommand(@NotNull final CustomCommand customCommand, @NotNull final CustomDropProcessingInfo info){
        final boolean useCustomNameForNametags = ymlHelper.getBoolean(main.settingsCfg, "use-customname-for-mob-nametags");
        final String overridenName = main.rulesManager.getRule_EntityOverriddenName(info.lmEntity, useCustomNameForNametags);

        String displayName = overridenName == null ?
                Utils.capitalize(info.lmEntity.getTypeName().replaceAll("_", " ")) :
                MessageUtils.colorizeAll(overridenName);

        if (info.lmEntity.getLivingEntity().getCustomName() != null)
            displayName = info.lmEntity.getLivingEntity().getCustomName();

        String command = main.levelManager.replaceStringPlaceholders(customCommand.command, info.lmEntity, displayName);

        final String playerName = info.wasKilledByPlayer ?
                Objects.requireNonNull(info.lmEntity.getLivingEntity().getKiller()).getName() :
                "";

        command = Utils.replaceEx(command, "%player%", playerName);
        command = command.replace("%displayname%", displayName);
        command = processRangedCommand(command, customCommand);

        final int maxAllowedTimesToRun = ymlHelper.getInt(main.settingsCfg, "customcommand-amount-limit", 10);
        int timesToRun = customCommand.getAmount();

        if (customCommand.getHasAmountRange())
            timesToRun = main.random.nextInt(customCommand.getAmountRangeMax() - customCommand.amountRangeMin + 1) + customCommand.amountRangeMin;

        if (timesToRun > maxAllowedTimesToRun)
            timesToRun = maxAllowedTimesToRun;

        final String debugCommand = timesToRun > 1 ?
                String.format("Command (%sx): ", timesToRun) : "Command: ";

        Utils.debugLog(main, DebugType.CUSTOM_COMMANDS, debugCommand + command);

        for (int i = 0; i < timesToRun; i++)
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    @NotNull
    private String processRangedCommand(@NotNull String command, final CustomCommand cc){
        if (cc.rangedEntries.isEmpty()) return command;

        for (final String rangedKey : cc.rangedEntries.keySet()) {
            final String rangedValue = cc.rangedEntries.get(rangedKey);
            if (!rangedValue.contains("-")) {
                command = command.replace("%" + rangedKey + "%", rangedValue);
                continue;
            }

            final String[] nums = rangedValue.split("-");
            if (nums.length != 2) continue;

            if (!Utils.isInteger(nums[0].trim()) || !Utils.isInteger(nums[1].trim())) continue;
            int min = Integer.parseInt(nums[0].trim());
            final int max = Integer.parseInt(nums[1].trim());
            if (max < min) min = max;

            final int rangedNum = main.random.nextInt(max - min + 1) + min;
            command = command.replace("%" + rangedKey + "%", rangedNum + "");
        }

        return command;
    }

    private ItemStack getCookedVariantOfMeat(@NotNull final ItemStack itemStack){
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
}
