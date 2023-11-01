/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.customdrops;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.misc.Addition;
import me.lokka30.levelledmobs.misc.DebugType;
import me.lokka30.levelledmobs.wrappers.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.YmlParsingHelper;
import me.lokka30.levelledmobs.result.PlayerLevelSourceResult;
import me.lokka30.levelledmobs.rules.LevelledMobSpawnReason;
import me.lokka30.levelledmobs.util.MessageUtils;
import me.lokka30.levelledmobs.util.PaperUtils;
import me.lokka30.levelledmobs.util.SpigotUtils;
import me.lokka30.levelledmobs.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The main CustomDropsclass that holds useful functions for parsing, instantizing and more of
 * custom drop items
 *
 * @author stumper66
 * @since 2.4.0
 */
public class CustomDropsHandler {

    public CustomDropsHandler(final LevelledMobs main) {
        this.main = main;
        this.customDropsitems = new TreeMap<>();
        this.customDropsitems_Babies = new TreeMap<>();
        this.customDropsitems_groups = new TreeMap<>();
        this.customDropIDs = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.groupIdToInstance = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.groupLimitsMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        customDropsParser = new CustomDropsParser(main, this);
        this.ymlHelper = customDropsParser.ymlHelper;
        this.customEquippedItems = new WeakHashMap<>();
        if (main.companion.externalCompatibilityManager.doesLMIMeetVersionRequirement()) {
            this.lmItemsParser = new LMItemsParser(main);
        }
        this.externalCustomDrops = new ExternalCustomDropsImpl();
    }

    final LevelledMobs main;
    // regular custom drops defined for a mob type
    private final Map<EntityType, CustomDropInstance> customDropsitems;
    // regular custom drops defined for a mob type that is a baby
    final Map<EntityType, CustomDropInstance> customDropsitems_Babies;
    // only used for the built-in universal groups
    private final Map<String, CustomDropInstance> customDropsitems_groups;
    // these are drops defined by a drop table
    final Map<String, CustomDropInstance> customDropIDs;
    // mappings of groupIds to drop instance
    final Map<String, CustomDropInstance> groupIdToInstance;
    // get a drop instance from it's groupid
    @Nullable Map<String, CustomDropInstance> customItemGroups;
    // groupid to grouplimits map
    final Map<String, GroupLimits> groupLimitsMap;
    public final CustomDropsParser customDropsParser;
    public final ExternalCustomDrops externalCustomDrops;
    LMItemsParser lmItemsParser;
    private final YmlParsingHelper ymlHelper;
    private final WeakHashMap<LivingEntity, EquippedItemsInfo> customEquippedItems;

    public @NotNull Map<EntityType, CustomDropInstance> getCustomDropsitems(){
        final Map<EntityType, CustomDropInstance> drops = new TreeMap<>(this.customDropsitems);
        for (final EntityType entityType : externalCustomDrops.getCustomDrops().keySet()){
            final CustomDropInstance dropInstance = externalCustomDrops.getCustomDrops().get(entityType);
            if (!drops.containsKey(entityType)){
                drops.put(entityType, dropInstance);
                continue;
            }

            // merge the 3rd party drops into the defined drops for the entity
            // 3rd party drop settings will override any conflicting
            final CustomDropInstance currentDropInstance = drops.get(entityType);
            currentDropInstance.combineDrop(dropInstance);

            if (dropInstance.overallChance != null)
                currentDropInstance.overallChance = dropInstance.overallChance;
            currentDropInstance.overallPermissions.addAll(dropInstance.overallPermissions);
        }

        return drops;
    }

    public @NotNull Map<String, CustomDropInstance> getCustomDropsitems_groups(){
        final Map<String, CustomDropInstance> drops = new TreeMap<>(
                this.customItemGroups != null ? this.customItemGroups : new HashMap<>());
        drops.putAll(this.customDropsitems_groups);

        for (final String groupName : externalCustomDrops.getCustomDropTables().keySet()){
            final CustomDropInstance dropInstance = externalCustomDrops.getCustomDropTables().get(groupName);
            if (!drops.containsKey(groupName)){
                drops.put(groupName, dropInstance);
                continue;
            }

            // merge the 3rd party drops into the defined drops for the entity
            // 3rd party drop settings will override any conflicting
            final CustomDropInstance currentDropInstance = drops.get(groupName);
            currentDropInstance.combineDrop(dropInstance);

            if (dropInstance.overallChance != null)
                currentDropInstance.overallChance = dropInstance.overallChance;
            currentDropInstance.overallPermissions.addAll(dropInstance.overallPermissions);
        }

        return drops;
    }

    void addCustomDropItem(final @NotNull EntityType entityType, final @NotNull CustomDropInstance customDropInstance){
        this.customDropsitems.put(entityType, customDropInstance);
    }

    void addCustomDropGroup(final @NotNull String groupName, final @NotNull CustomDropInstance customDropInstance){
        this.customDropsitems_groups.put(groupName, customDropInstance);
    }

    public CustomDropResult getCustomItemDrops(final LivingEntityWrapper lmEntity,
        final List<ItemStack> drops, final boolean equippedOnly) {
        final CustomDropProcessingInfo processingInfo = new CustomDropProcessingInfo();
        processingInfo.lmEntity = lmEntity;
        processingInfo.equippedOnly = equippedOnly;
        processingInfo.newDrops = drops;

        processingInfo.dropRules = main.rulesManager.getRuleUseCustomDropsForMob(lmEntity);
        synchronized (lmEntity.getLivingEntity().getPersistentDataContainer()) {
            processingInfo.isSpawner = (lmEntity.getPDC()
                .has(main.namespacedKeys.spawnReasonKey, PersistentDataType.STRING) &&
                LevelledMobSpawnReason.SPAWNER.toString().equals(
                    lmEntity.getPDC()
                        .get(main.namespacedKeys.spawnReasonKey, PersistentDataType.STRING))
            );

            if (lmEntity.getPDC()
                .has(main.namespacedKeys.keySpawnerCustomDropId, PersistentDataType.STRING)) {
                processingInfo.customDropId = lmEntity.getPDC()
                    .get(main.namespacedKeys.keySpawnerCustomDropId, PersistentDataType.STRING);
                processingInfo.hasCustomDropId = !Utils.isNullOrEmpty(processingInfo.customDropId);
            }
        }

        if (lmEntity.getLivingEntity().getKiller() != null) {
            processingInfo.wasKilledByPlayer = true;
            processingInfo.mobKiller = lmEntity.getLivingEntity().getKiller();
        } else {
            processingInfo.wasKilledByPlayer = false;
        }

        if (lmEntity.getLivingEntity().getLastDamageCause() != null) {
            processingInfo.deathCause = DeathCause.valueOf(
                lmEntity.getLivingEntity().getLastDamageCause().getCause().toString()
                    .toUpperCase());
        }

        processingInfo.addition = BigDecimal.valueOf(
                main.mobDataManager.getAdditionsForLevel(lmEntity, Addition.CUSTOM_ITEM_DROP, 2))
            .setScale(0, RoundingMode.HALF_DOWN).intValueExact(); // truncate double to int

        processingInfo.doNotMultiplyDrops = main.rulesManager.getRuleCheckIfNoDropMultiplierEntitiy(
            lmEntity);

        if (lmEntity.getLivingEntity().getLastDamageCause() != null) {
            final EntityDamageEvent.DamageCause damageCause = lmEntity.getLivingEntity()
                .getLastDamageCause().getCause();
            processingInfo.deathByFire = (damageCause == EntityDamageEvent.DamageCause.FIRE ||
                damageCause == EntityDamageEvent.DamageCause.FIRE_TICK ||
                damageCause == EntityDamageEvent.DamageCause.LAVA);
        }

        if (!equippedOnly) {
            final String mobLevel =
                lmEntity.getMobLevel() > 0 ? "&r (level " + lmEntity.getMobLevel() + ")" : "";
            processingInfo.addDebugMessage(DebugType.CUSTOM_DROPS,
                    "&7Custom drops for &b" + lmEntity.getNameIfBaby() + mobLevel);

            processingInfo.addDebugMessage(DebugType.MOB_GROUPS,
                    "&8- &7Groups: &b" + String.join("&7, &b", lmEntity.getApplicableGroups()) + "&7.");
        }

        final List<String> groupsList = new LinkedList<>();
        for (final String group : lmEntity.getApplicableGroups()) {
            if (!getCustomDropsitems_groups().containsKey(group)) {
                continue;
            }

            groupsList.add(group);
        }

        final DropInstanceBuildResult buildResult = buildDropsListFromGroupsAndEntity(groupsList,
            lmEntity.getEntityType(), processingInfo);
        if (buildResult != DropInstanceBuildResult.SUCCESSFUL) {
            // didn't make overall chance
            if (buildResult == DropInstanceBuildResult.DID_NOT_MAKE_CHANCE) {
                processingInfo.addDebugMessage(DebugType.CUSTOM_DROPS,
                    String.format("&7%s (%s) - didn't make overall chance",
                        lmEntity.getTypeName(), lmEntity.getMobLevel()));
            } else {
                processingInfo.addDebugMessage(DebugType.CUSTOM_DROPS, String.format(
                    "&7%s (%s) - didn't make overall chance permission for player: &b%s &r",
                    lmEntity.getTypeName(), lmEntity.getMobLevel(),
                    processingInfo.mobKiller == null ? "(null)"
                        : processingInfo.mobKiller.getName()));
            }
            processingInfo.writeAnyDebugMessages();

            return new CustomDropResult(processingInfo.stackToItem, processingInfo.hasOverride, false);
        }

        getCustomItemsFromDropInstance(processingInfo); // payload

        final int postCount = drops.size();
        final boolean showCustomEquips = main.companion.debugsEnabled.contains(DebugType.CUSTOM_EQUIPS);
        final boolean showCustomDrops = main.companion.debugsEnabled.contains(DebugType.CUSTOM_DROPS);

        if (showCustomDrops || showCustomEquips) {
            if (equippedOnly && !drops.isEmpty() && showCustomEquips) {
                if (lmEntity.getMobLevel() > -1) {
                    processingInfo.addDebugMessage(
                            String.format("&7Custom equipment for &b%s &r(%s)", lmEntity.getTypeName(),
                                    lmEntity.getMobLevel()));

                } else {
                    processingInfo.addDebugMessage(
                        "&7Custom equipment for &b" + lmEntity.getTypeName() + "&r");
                }
                final StringBuilder sb = new StringBuilder();
                for (final ItemStack drop : drops) {
                    if (!sb.isEmpty()) {
                        sb.append(", ");
                    }
                    sb.append(drop.getType().name());
                }
                processingInfo.addDebugMessage("   " + sb);
            } else if (!equippedOnly && showCustomDrops) {
                processingInfo.addDebugMessage(
                    String.format("&8 --- &7Custom items added: &b%s&7.", postCount));
            }

            processingInfo.writeAnyDebugMessages();
        }

        return new CustomDropResult(processingInfo.stackToItem, processingInfo.hasOverride, postCount > 0);
    }

    private DropInstanceBuildResult buildDropsListFromGroupsAndEntity(final List<String> groups,
        final EntityType entityType, @NotNull final CustomDropProcessingInfo info) {
        info.prioritizedDrops = new HashMap<>();
        info.hasOverride = false;
        boolean usesGroupIds = false;

        final boolean overrideNonDropTableDrops = info.dropRules != null && info.dropRules.chunkKillOptions.getDisableVanillaDrops();

        for (final String id : getDropIds(info)) {
            if (this.customItemGroups == null || !this.customItemGroups.containsKey(id.trim())) {
                Utils.logger.warning("rule specified an invalid value for use-droptable-id: " + id);
                continue;
            }

            final CustomDropInstance dropInstance = this.customItemGroups.get(id.trim());
            info.allDropInstances.add(dropInstance);

            for (final CustomDropBase baseItem : dropInstance.customItems) {
                processDropPriorities(baseItem, info);
            }

            if (dropInstance.utilizesGroupIds) {
                usesGroupIds = true;
            }
            if (dropInstance.getOverrideStockDrops()) {
                info.hasOverride = true;
            }
        }

        if (!overrideNonDropTableDrops) {
            for (final String group : groups) {
                final CustomDropInstance dropInstance = getCustomDropsitems_groups().get(group);
                info.allDropInstances.add(dropInstance);

                for (final CustomDropBase baseItem : dropInstance.customItems) {
                    processDropPriorities(baseItem, info);
                }

                if (dropInstance.utilizesGroupIds) {
                    usesGroupIds = true;
                }
                if (dropInstance.getOverrideStockDrops()) {
                    info.hasOverride = true;
                }
            }

            final Map<EntityType, CustomDropInstance> dropMap =
                info.lmEntity.isBabyMob() && customDropsitems_Babies.containsKey(entityType) ?
                    customDropsitems_Babies : getCustomDropsitems();

            if (dropMap.containsKey(entityType)) {
                final CustomDropInstance dropInstance = dropMap.get(entityType);
                info.allDropInstances.add(dropInstance);

                for (final CustomDropBase baseItem : dropInstance.customItems) {
                    processDropPriorities(baseItem, info);
                }

                if (dropInstance.utilizesGroupIds) {
                    usesGroupIds = true;
                }
                if (dropInstance.getOverrideStockDrops()) {
                    info.hasOverride = true;
                }
            }
        }

        if (usesGroupIds) {
            for (final List<CustomDropBase> customDropBases : info.prioritizedDrops.values()) {
                Collections.shuffle(customDropBases);
            }
        }

        if (!checkOverallPermissions(info)) {
            return DropInstanceBuildResult.PERMISSION_DENIED;
        }

        if (info.equippedOnly && !info.hasEquippedItems) {
            return DropInstanceBuildResult.SUCCESSFUL;
        }

        return checkOverallChance(info) ?
            DropInstanceBuildResult.SUCCESSFUL : DropInstanceBuildResult.DID_NOT_MAKE_CHANCE;
    }

    private boolean checkOverallPermissions(@NotNull final CustomDropProcessingInfo info) {
        boolean hadAnyPerms = false;
        for (final CustomDropInstance dropInstance : info.allDropInstances) {
            if (dropInstance.overallPermissions.isEmpty()) {
                continue;
            }

            hadAnyPerms = true;
            for (final String perm : dropInstance.overallPermissions) {
                if (info.mobKiller == null) {
                    continue;
                }
                final String checkPerm = "LevelledMobs.permission." + perm;
                if (info.mobKiller.hasPermission(checkPerm)) {
                    return true;
                }
            }
        }

        return !hadAnyPerms;
    }

    @NotNull private List<String> getDropIds(@NotNull final CustomDropProcessingInfo processingInfo) {
        final List<String> dropIds = new LinkedList<>();
        if (processingInfo.dropRules != null) {
            for (final String id : processingInfo.dropRules.useDropTableIds) {
                dropIds.addAll(List.of(id.split(",")));
            }
        }

        if (processingInfo.hasCustomDropId && !dropIds.contains(processingInfo.customDropId)) {
            dropIds.add(processingInfo.customDropId);
        }

        return dropIds;
    }

    private void processDropPriorities(@NotNull final CustomDropBase baseItem,
        @NotNull final CustomDropProcessingInfo processingInfo) {
        final int priority = -baseItem.priority;
        if (processingInfo.prioritizedDrops.containsKey(priority)) {
            processingInfo.prioritizedDrops.get(priority).add(baseItem);
        } else {
            final List<CustomDropBase> items = new LinkedList<>();
            items.add(baseItem);
            processingInfo.prioritizedDrops.put(priority, items);
        }

        if (baseItem instanceof CustomDropItem
            && ((CustomDropItem) baseItem).equippedSpawnChance > 0.0F) {
            processingInfo.hasEquippedItems = true;
        }
    }

    private void getCustomItemsFromDropInstance(final @NotNull CustomDropProcessingInfo info) {
        final List<UUID> dropLimitsReached = new LinkedList<>();
        final GroupLimits defaultLimits = this.groupLimitsMap.getOrDefault("default", null);

        for (final List<CustomDropBase> items : info.prioritizedDrops.values()) {
            // loop thru each drop list associated with any groupids

            final int retriesHardcodedMax = 10;
            int maxRetries = 1;

            for (int i = 0; i < maxRetries; i++) {
                info.retryNumber = i;

                for (final CustomDropBase drop : items) {
                    // loop thru all drops in this groupid

                    if (drop.groupId != null){
                        info.dropInstance = this.groupIdToInstance.get(drop.groupId);
                        info.groupLimits = this.groupLimitsMap.getOrDefault(drop.groupId, defaultLimits);
                        maxRetries = Math.min(info.groupLimits != null ? info.groupLimits.retries : 1, retriesHardcodedMax);
                    }
                    else{
                        info.dropInstance = null;
                        info.groupLimits = null;
                    }

                    if (info.groupLimits != null) {
                        final int itemDroppedCount = info.getItemsDropsById(drop);
                        if (info.groupLimits.hasReachedCapPerItem(itemDroppedCount)) {
                            if (!dropLimitsReached.contains(drop.uid)){
                                dropLimitsReached.add(drop.uid);
                                String itemDescription = (drop instanceof CustomDropItem dropItem) ?
                                        dropItem.getMaterial().name() : "CustomCommand";
                                Utils.debugLog(main, DebugType.GROUP_LIMITS,
                                        String.format("Reached cap-per-item limit of %s for %s",
                                                info.groupLimits.capPerItem, itemDescription));
                            }
                            continue;
                        }

                        final int groupDroppedCount = info.getDropItemsCountForGroup(drop);
                        if (info.groupLimits.hasReachedCapTotal(groupDroppedCount)){
                            Utils.debugLog(main, DebugType.GROUP_LIMITS,
                                    String.format("Reached cap-total of %s for group: %s",
                                            info.groupLimits.capTotal, drop.groupId));
                            return;
                        }
                    }

                    // payload:
                    getDropsFromCustomDropItem(info, drop);
                }

            } // next retry
        } // next group
    }

    private void getDropsFromCustomDropItem(@NotNull final CustomDropProcessingInfo info,
        final CustomDropBase dropBase) {
        if (dropBase instanceof CustomCommand && info.lmEntity.getLivingEntity()
            .hasMetadata("noCommands") ||
            info.lmEntity.deathCause == EntityDamageEvent.DamageCause.VOID) {
            return;
        }

        if (info.equippedOnly && dropBase instanceof CustomCommand
            && !((CustomCommand) dropBase).runOnSpawn) {
            return;
        }
        if (info.equippedOnly && dropBase instanceof CustomDropItem
            && ((CustomDropItem) dropBase).equippedSpawnChance <= 0.0F) {
            return;
        }
        if (!info.equippedOnly && dropBase.playerCausedOnly && (dropBase.causeOfDeathReqs == null
            || dropBase.causeOfDeathReqs.isEmpty()) && !info.wasKilledByPlayer) {
            return;
        }
        if (dropBase.noSpawner && info.isSpawner) {
            return;
        }

        if (shouldDenyDeathCause(dropBase, info)) {
            return;
        }

        if (!madePlayerLevelRequirement(info, dropBase)) {
            return;
        }

        if (dropBase.excludedMobs.contains(info.lmEntity.getTypeName())) {
            if (dropBase instanceof final CustomDropItem dropItem && !info.equippedOnly) {

                info.addDebugMessage(DebugType.CUSTOM_DROPS, String.format(
                    "&8 - &7Mob: &b%s&7, item: %s, mob was excluded", info.lmEntity.getTypeName(),
                    dropItem.getMaterial().name()));
            }
            return;
        }

        boolean doDrop =
            dropBase.maxLevel <= -1 || info.lmEntity.getMobLevel() <= dropBase.maxLevel;
        if (dropBase.minLevel > -1 && info.lmEntity.getMobLevel() < dropBase.minLevel) {
            doDrop = false;
        }
        if (!doDrop) {
            if (dropBase instanceof final CustomDropItem dropItem) {
                if (!info.equippedOnly && isCustomDropsDebuggingEnabled()) {
                    final ItemStack itemStack =
                        info.deathByFire ? getCookedVariantOfMeat(dropItem.getItemStack())
                            : dropItem.getItemStack();

                    info.addDebugMessage(String.format(
                        "&8- &7level: &b%s&7, fromSpawner: &b%s&7, item: &b%s&7, minL: &b%s&7, maxL: &b%s&7, nospawner: &b%s&7, dropped: &bfalse",
                        info.lmEntity.getMobLevel(), info.isSpawner, itemStack.getType().name(),
                        dropBase.minLevel, dropBase.maxLevel, dropBase.noSpawner));
                }
            } else if (dropBase instanceof CustomCommand) {
                info.addDebugMessage(DebugType.CUSTOM_DROPS, String.format(
                    "&8- custom-cmd: &7level: &b%s&7, fromSpawner: &b%s&7, minL: &b%s&7, maxL: &b%s&7, nospawner: &b%s&7, executed: &bfalse",
                    info.lmEntity.getMobLevel(), info.isSpawner, dropBase.minLevel,
                    dropBase.maxLevel, dropBase.noSpawner));
            }
            return;
        }

        // equip-chance and equip-drop-chance:
        if (!info.equippedOnly && dropBase instanceof final CustomDropItem item) {
            if (!checkIfMadeEquippedDropChance(info, item)) {
                if (isCustomDropsDebuggingEnabled()) {
                    info.addDebugMessage(String.format(
                        "&8 - &7item: &b%s&7, was not equipped on mob, dropped: &bfalse&7.",
                        item.getItemStack().getType().name())
                    );
                }
                return;
            }
        }

        if (!info.equippedOnly && !checkDropPermissions(info, dropBase)) {
            return;
        }

        final boolean runOnSpawn = dropBase instanceof CustomCommand cc && cc.runOnSpawn;
        boolean didNotMakeChance = false;
        float chanceRole = 0.0F;

        if (!info.equippedOnly && dropBase.useChunkKillMax && info.wasKilledByPlayer
            && hasReachedChunkKillLimit(info.lmEntity)) {
            if (dropBase instanceof CustomDropItem) {
                info.addDebugMessage(DebugType.CUSTOM_DROPS, String.format(
                    "&8- &7level: &b%s&7, item: &b%s&7, gId: &b%s&7, chunk kill count reached",
                    info.lmEntity.getMobLevel(),
                    ((CustomDropItem) dropBase).getMaterial().name(), dropBase.groupId));
            } else {
                info.addDebugMessage(DebugType.CUSTOM_DROPS, String.format(
                    "&8- &7level: &b%s&7, item: custom command, gId: &b%s&7, chunk kill count reached",
                    info.lmEntity.getMobLevel(), dropBase.groupId));
            }

            return;
        }

        if ((!info.equippedOnly || runOnSpawn) && dropBase.chance < 1.0) {
            chanceRole = (float) ThreadLocalRandom.current().nextInt(0, 100001) * 0.00001F;
            if (1.0F - chanceRole >= dropBase.chance) {
                didNotMakeChance = true;
            }
        }

        if (didNotMakeChance && (!info.equippedOnly || runOnSpawn) && isCustomDropsDebuggingEnabled()) {
            if (dropBase instanceof final CustomDropItem dropItem) {
                final ItemStack itemStack =
                    info.deathByFire ? getCookedVariantOfMeat(dropItem.getItemStack())
                        : dropItem.getItemStack();

                info.addDebugMessage(DebugType.CUSTOM_DROPS, String.format(
                    "&8 - &7item: &b%s&7, amount: &b%s&7, chance: &b%s&7, chanceRole: &b%s&7, dropped: &bfalse&7.",
                    itemStack.getType().name(), dropItem.getAmountAsString(), dropBase.chance,
                    Utils.round(chanceRole, 4))
                );
            }
            else{
                info.addDebugMessage(DebugType.CUSTOM_DROPS, String.format(
                        "&8 - &7Custom command&7, chance: &b%s&7, chanceRole: &b%s&7, executed: &bfalse&7.",
                        dropBase.chance, Utils.round(chanceRole, 4))
                );
            }
        }
        if ((!info.equippedOnly || runOnSpawn) && didNotMakeChance) {
            return;
        }

        final boolean hasGroupId = !Utils.isNullOrEmpty(dropBase.groupId);
        int maxDropGroup = 0;
        if (info.groupLimits != null && info.groupLimits.hasCapSelect()){
            maxDropGroup = Math.max(info.groupLimits.capSelect, 0);
        }
        else if (info.groupLimits == null){
            maxDropGroup = dropBase.maxDropGroup;
        }

        if (!info.equippedOnly && hasGroupId) {
            // legacy section, only executed if the old 'maxdropgroup' was used
            // instead of 'group-limits.capSelect'
            final int groupDroppedCount = info.getItemsDropsByGroup(dropBase);

            if (maxDropGroup > 0 && groupDroppedCount >= maxDropGroup
                || info.groupLimits == null && maxDropGroup == 0 && groupDroppedCount > 0) {
                if (isCustomDropsDebuggingEnabled()) {
                    if (dropBase instanceof final CustomDropItem item) {
                        info.addDebugMessage(String.format(
                            "&8- &7level: &b%s&7, item: &b%s&7, gId: &b%s&7, maxDropGroup: &b%s&7, groupDropCount: &b%s&7, dropped: &bfalse",
                            info.lmEntity.getMobLevel(),
                            item.getMaterial().name(), dropBase.groupId,
                            info.getItemsDropsByGroup(dropBase), groupDroppedCount));
                    } else {
                        info.addDebugMessage(String.format(
                            "&8- &7level: &b%s&7, item: custom command, gId: &b%s&7, maxDropGroup: &b%s&7, groupDropCount: &b%s&7, executed: &bfalse",
                            info.lmEntity.getMobLevel(), info.getItemsDropsByGroup(dropBase), dropBase.maxDropGroup,
                            groupDroppedCount));
                    }
                }
                return;
            }
        }

        if (dropBase instanceof CustomCommand) {
            // ------------------------------------------ commands get executed here then function returns ---------------------------------------------------
            executeCommand((CustomCommand) dropBase, info);

            if (hasGroupId) {
                if (isCustomDropsDebuggingEnabled()) {
                    final int count = info.getItemsDropsByGroup(dropBase);
                    String msg = String.format(
                        "&8- &7level: &b%s&7, item: command, gId: &b%s&7, maxDropGroup: &b%s&7, groupDropCount: &b%s&7, executed: &btrue",
                        info.lmEntity.getMobLevel(), dropBase.groupId, dropBase.maxDropGroup, count);
                    if (info.retryNumber > 0){
                        msg += ", retry: " + info.retryNumber;
                    }
                    info.addDebugMessage(msg);
                }
            } else if (isCustomDropsDebuggingEnabled()) {
                String msg = String.format(
                    "&8- &7level: &b%s&7, item: custom command, gId: &b%s&7, maxDropGroup: &b%s&7, executed: &btrue",
                    info.lmEntity.getMobLevel(), dropBase.groupId, dropBase.maxDropGroup);

                if (info.retryNumber > 0){
                    msg += ", retry: " + info.retryNumber;
                }
                info.addDebugMessage(msg);
            }

            return;
            // -----------------------------------------------------------------------------------------------------------------------------------------------
        }
        if (!(dropBase instanceof final CustomDropItem dropItem)) {
            Utils.logger.warning("Unsupported drop type: " + dropBase.getClass().getName());
            return;
        }

        if (info.equippedOnly && dropItem.equippedSpawnChance < 1.0F) {
            chanceRole = (float) ThreadLocalRandom.current().nextInt(0, 100001) * 0.00001F;
            if (1.0F - chanceRole >= dropItem.equippedSpawnChance) {
                if (isCustomDropsDebuggingEnabled()) {
                    info.addDebugMessage(String.format(
                        "&8- Mob: &b%s&7, &7level: &b%s&7, item: &b%s&7, spawnchance: &b%s&7, chancerole: &b%s&7, did not make spawn chance",
                        info.lmEntity.getTypeName(), info.lmEntity.getMobLevel(),
                        dropItem.getMaterial().name(), dropItem.equippedSpawnChance,
                        Utils.round(chanceRole, 4)));
                }
                return;
            }
        }

        int newDropAmount = dropItem.getAmount();
        if (dropItem.getHasAmountRange()) {
            final int change = ThreadLocalRandom.current()
                .nextInt(0, dropItem.getAmountRangeMax() - dropItem.getAmountRangeMin() + 1);
            newDropAmount = dropItem.getAmountRangeMin() + change;
        }

        if (hasGroupId && info.groupLimits != null){
            final GroupLimits gl = info.groupLimits;

            if (gl.hasCapPerItem()){
                newDropAmount = Math.min(newDropAmount, gl.capPerItem);
            }

            if (gl.hasCapTotal() && dropBase.groupId != null){
                final int hasDroppedSoFar = info.getDropItemsCountForGroup(dropBase);
                if (gl.capTotal - hasDroppedSoFar > gl.capTotal){
                    newDropAmount = gl.capTotal;
                }
            }
        }

        // if we made it this far then the item will be dropped

        if (dropItem.isExternalItem && isCustomDropsDebuggingEnabled()
            && !main.companion.externalCompatibilityManager.doesLMIMeetVersionRequirement()) {
            Utils.debugLog(main, DebugType.CUSTOM_DROPS,
                "Could not get external custom item - LM_Items is not installed");
        }

        processEnchantmentChances(dropItem);

        ItemStack newItem;
        if (dropItem.isExternalItem && main.companion.externalCompatibilityManager.doesLMIMeetVersionRequirement()
            && lmItemsParser.getExternalItem(dropItem, info)) {
            newItem = dropItem.getItemStack();
        } else if (info.deathByFire) {
            newItem = getCookedVariantOfMeat(dropItem.getItemStack().clone());
        } else {
            newItem = dropItem.getItemStack().clone();
        }

        newItem.setAmount(newDropAmount);

        if (!dropItem.noMultiplier && !info.doNotMultiplyDrops) {
            main.levelManager.multiplyDrop(info.lmEntity, newItem, info.addition, true);
            newDropAmount = newItem.getAmount();
        } else if (newDropAmount > newItem.getMaxStackSize()) {
            newDropAmount = newItem.getMaxStackSize();
        }

        if (newItem.getAmount() != newDropAmount) {
            newItem.setAmount(newDropAmount);
        }

        if (info.equippedOnly && main.companion.debugsEnabled.contains(DebugType.CUSTOM_EQUIPS)) {
            info.addDebugMessage(String.format(
                "&8 - &7item: &b%s&7, equipChance: &b%s&7, chanceRole: &b%s&7, equipped: &btrue&7.",
                newItem.getType().name(), dropItem.equippedSpawnChance,
                Utils.round(chanceRole, 4)));
        } else if (!info.equippedOnly && main.companion.debugsEnabled.contains(DebugType.CUSTOM_DROPS)) {
            final String retryMsg = info.retryNumber > 0 ? ", retry: " + info.retryNumber : "";

            info.addDebugMessage(String.format(
                "&8 - &7item: &b%s&7, amount: &b%s&7, newAmount: &b%s&7, chance: &b%s&7, chanceRole: &b%s&7, dropped: &btrue&7%s.",
                newItem.getType().name(), dropItem.getAmountAsString(), newDropAmount,
                dropItem.chance, Utils.round(chanceRole, 4), retryMsg));
        }

        int damage = dropItem.getDamage();
        if (dropItem.getHasDamageRange()) {
            damage = ThreadLocalRandom.current()
                .nextInt(dropItem.getDamageRangeMin(), dropItem.getDamageRangeMax() + 1);
        }

        if (damage > 0 || dropItem.lore != null || dropItem.customName != null) {
            final ItemMeta meta = newItem.getItemMeta();

            if (damage > 0 && meta instanceof Damageable) {
                ((Damageable) meta).setDamage(damage);
            }

            if (meta != null && dropItem.lore != null && !dropItem.lore.isEmpty()) {
                final List<String> newLore = new ArrayList<>(dropItem.lore.size());
                for (String lore : dropItem.lore) {
                    if (lore.contains("%")) {
                        lore = lore.replace("%player%", info.mobKiller == null ? "" : info.mobKiller.getName());
                        lore = main.levelManager.replaceStringPlaceholders(lore, info.lmEntity, true, info.mobKiller);
                    }

                    newLore.add(lore);

                    if (main.getVerInfo().getIsRunningPaper() && main.companion.useAdventure) {
                        PaperUtils.updateItemMetaLore(meta, newLore);
                    } else {
                        SpigotUtils.updateItemMetaLore(meta, newLore);
                    }
                }
            }

            if (meta != null && dropItem.customName != null && !dropItem.customName.isEmpty()) {
                String customName = dropItem.customName.replace("%player%", info.mobKiller == null ? "" : info.mobKiller.getName());
                customName = main.levelManager.replaceStringPlaceholders(customName, info.lmEntity, true, info.mobKiller);

                if (main.getVerInfo().getIsRunningPaper() && main.companion.useAdventure) {
                    PaperUtils.updateItemDisplayName(meta, customName);
                } else {
                    SpigotUtils.updateItemDisplayName(meta, MessageUtils.colorizeAll(customName));
                }
            }

            newItem.setItemMeta(meta);
        }

        if (!info.equippedOnly) info.itemGotDropped(dropBase, newDropAmount);

        if (newItem.getType() == Material.PLAYER_HEAD && !"none".equalsIgnoreCase(dropItem.mobHeadTexture)) {
            main.mobHeadManager.updateMobHeadFromPlayerHead(newItem, info.lmEntity, dropItem);
        }

        info.newDrops.add(newItem);
        info.stackToItem.add(Utils.getPair(newItem, dropItem));
    }

    private boolean checkOverallChance(@NotNull final CustomDropProcessingInfo info) {
        for (final CustomDropInstance dropInstance : info.allDropInstances) {
            if (dropInstance.overallChance == null || dropInstance.overallChance >= 1.0
                    || dropInstance.overallChance <= 0.0) {
                continue;
            }

            synchronized (info.lmEntity.getLivingEntity().getPersistentDataContainer()) {
                if (info.lmEntity.getPDC()
                        .has(main.namespacedKeys.overallChanceKey, PersistentDataType.INTEGER)) {
                    final int value = Objects.requireNonNull(info.lmEntity.getPDC()
                            .get(main.namespacedKeys.overallChanceKey, PersistentDataType.INTEGER));
                    return value == 1;
                }
            }

            // we'll roll the dice to see if we get any drops at all and store it in the PDC
            final float chanceRole =
                    (float) ThreadLocalRandom.current().nextInt(0, 100001) * 0.00001F;
            final boolean madeChance = 1.0F - chanceRole < dropInstance.overallChance;
            if (info.equippedOnly) {
                synchronized (info.lmEntity.getLivingEntity().getPersistentDataContainer()) {
                    info.lmEntity.getPDC()
                            .set(main.namespacedKeys.overallChanceKey, PersistentDataType.INTEGER,
                                    madeChance ? 1 : 0);
                }
            }

            return madeChance;
        }

        return true;
    }

    private void processEnchantmentChances(final @NotNull CustomDropItem dropItem){
        if (dropItem.enchantmentChances == null || dropItem.enchantmentChances.isEmpty()) return;

        final StringBuilder debug = new StringBuilder();
        boolean isFirstEnchantment = true;
        for (final Enchantment enchantment : dropItem.enchantmentChances.items.keySet()){
            final EnchantmentChances.ChanceOptions opts = dropItem.enchantmentChances.options.get(enchantment);
            boolean madeAnyChance = false;
            if (isCustomDropsDebuggingEnabled()) {
                if (!isFirstEnchantment) debug.append("; ");
                debug.append(enchantment.getKey().value()).append(": ");
            }

            if (isFirstEnchantment)
                isFirstEnchantment = false;

            int enchantmentNumber = 0;
            final List<Integer> levelsList = new ArrayList<>(dropItem.enchantmentChances.items.get(enchantment).keySet());
            if (opts == null || opts.doShuffle)
                Collections.shuffle(levelsList);

            for (final int enchantLevel : levelsList){
                final float chanceValue = dropItem.enchantmentChances.items.get(enchantment).get(enchantLevel);
                if (chanceValue <= 0.0f) continue;
                enchantmentNumber++;

                final float chanceRole =
                        (float) ThreadLocalRandom.current().nextInt(0, 100001) * 0.00001F;
                final boolean madeChance = 1.0F - chanceRole < chanceValue;
                if (!madeChance){
                    if (isCustomDropsDebuggingEnabled()){
                        if (enchantmentNumber > 1) debug.append(", ");
                        debug.append(String.format("%s: &4%s&r &b(%s)&r", enchantLevel, chanceRole, chanceValue));
                    }
                    continue;
                }

                if (isCustomDropsDebuggingEnabled()){
                    if (enchantmentNumber > 1) debug.append(", ");
                    debug.append(String.format("%s: &2%s&r &b(%s)&r", enchantLevel, chanceRole, chanceValue));
                }

                if (dropItem.getMaterial() == Material.ENCHANTED_BOOK){
                    final EnchantmentStorageMeta meta = (EnchantmentStorageMeta) dropItem.getItemStack().getItemMeta();
                    if (meta != null) {
                        meta.addStoredEnchant(enchantment, enchantLevel, true);
                        dropItem.getItemStack().setItemMeta(meta);
                    }
                }
                else{
                    dropItem.getItemStack().addUnsafeEnchantment(enchantment, enchantLevel);
                }
                madeAnyChance = true;
                break;
            }

            if (!madeAnyChance && opts != null && opts.defaultLevel != null){
                dropItem.getItemStack().addUnsafeEnchantment(enchantment, opts.defaultLevel);
                if (isCustomDropsDebuggingEnabled())
                    debug.append(", used dflt: &2").append(opts.defaultLevel).append("&r");
            }
        }

        if (isCustomDropsDebuggingEnabled())
            Utils.logger.info(debug.toString());
    }

    private boolean hasReachedChunkKillLimit(final @NotNull LivingEntityWrapper lmEntity) {
        final int maximumDeathInChunkThreshold = main.rulesManager.getMaximumDeathInChunkThreshold(
            lmEntity);
        if (maximumDeathInChunkThreshold <= 0) {
            return false;
        }

        return lmEntity.chunkKillcount >= maximumDeathInChunkThreshold;
    }

    private boolean shouldDenyDeathCause(final @NotNull CustomDropBase dropBase,
        final @NotNull CustomDropProcessingInfo info) {
        if (dropBase.causeOfDeathReqs == null || info.deathCause == null) {
            return false;
        }

        if (info.wasKilledByPlayer && Utils.isDamageCauseInModalList(dropBase.causeOfDeathReqs,
            DeathCause.PLAYER_CAUSED)) {
            return false;
        }

        if (!Utils.isDamageCauseInModalList(dropBase.causeOfDeathReqs, info.deathCause)) {
            if (isCustomDropsDebuggingEnabled()) {
                final String itemName = dropBase instanceof CustomDropItem ?
                    ((CustomDropItem) dropBase).getMaterial().name() : "(command)";
                info.addDebugMessage(String.format(
                    "&8 - &7item: &b%s&7, death-cause: &b%s&7, death-cause-req: &b%s&7, dropped: &bfalse&7.",
                    itemName, info.deathCause, dropBase.causeOfDeathReqs)
                );
            }

            return true;
        }

        return false;
    }

    private boolean checkDropPermissions(final @NotNull CustomDropProcessingInfo info,
        final @NotNull CustomDropBase dropBase) {
        if (info.equippedOnly || dropBase.permissions.isEmpty()) {
            return true;
        }

        if (info.mobKiller == null) {
            if (isCustomDropsDebuggingEnabled()) {
                info.addDebugMessage(String.format(
                    "&8 - &7item: &b%s&7, not player was provided for item permissions",
                    (dropBase instanceof CustomDropItem) ?
                        ((CustomDropItem) dropBase).getItemStack().getType().name()
                        : "custom command"));
            }
            return false;
        }

        boolean hadPermission = false;
        for (final String perm : dropBase.permissions) {
            final String permCheck = "levelledmobs.permission." + perm;
            if (info.mobKiller.hasPermission(permCheck)) {
                hadPermission = true;
                break;
            }
        }

        if (!hadPermission) {
            if (isCustomDropsDebuggingEnabled()) {
                info.addDebugMessage(String.format(
                    "&8 - &7item: &b%s&7, player: &b%s&7 didn't have permission: &b%s&7",
                    (dropBase instanceof CustomDropItem) ?
                        ((CustomDropItem) dropBase).getItemStack().getType().name()
                        : "custom command",
                    info.mobKiller.getName(), dropBase.permissions));
            }
            return false;
        }

        return true;
    }

    private boolean checkIfMadeEquippedDropChance(final CustomDropProcessingInfo info,
        final @NotNull CustomDropItem item) {
        if (item.equippedSpawnChance >= 1.0F || !item.onlyDropIfEquipped) {
            return true;
        }
        if (item.equippedSpawnChance <= 0.0F) {
            return false;
        }

        return isMobWearingItem(item.getItemStack(), info.lmEntity.getLivingEntity(), item);
    }

    private boolean isMobWearingItem(final ItemStack item, final @NotNull LivingEntity mob,
        final CustomDropItem customDropItem) {
        final EntityEquipment equipment = mob.getEquipment();
        if (equipment == null) {
            return false;
        }

        final EquippedItemsInfo equippedItemsInfo = this.customEquippedItems.get(mob);
        if (equippedItemsInfo == null) {
            return false;
        }

        if (customDropItem.equipOnHelmet && item.isSimilar(equipment.getHelmet())){
            return true;
        }

        switch (item.getType()) {
            case LEATHER_HELMET:
            case CHAINMAIL_HELMET:
            case IRON_HELMET:
            case DIAMOND_HELMET:
            case NETHERITE_HELMET:
                if (equippedItemsInfo.helmet != null
                    && customDropItem == equippedItemsInfo.helmet) {
                    return true;
                }
            case LEATHER_CHESTPLATE:
            case CHAINMAIL_CHESTPLATE:
            case IRON_CHESTPLATE:
            case DIAMOND_CHESTPLATE:
            case NETHERITE_CHESTPLATE:
                if (equippedItemsInfo.chestplate != null
                    && customDropItem == equippedItemsInfo.chestplate) {
                    return true;
                }
            case LEATHER_LEGGINGS:
            case CHAINMAIL_LEGGINGS:
            case IRON_LEGGINGS:
            case DIAMOND_LEGGINGS:
            case NETHERITE_LEGGINGS:
                if (equippedItemsInfo.leggings != null
                    && customDropItem == equippedItemsInfo.leggings) {
                    return true;
                }
            case LEATHER_BOOTS:
            case CHAINMAIL_BOOTS:
            case IRON_BOOTS:
            case DIAMOND_BOOTS:
            case NETHERITE_BOOTS:
                if (equippedItemsInfo.boots != null && customDropItem == equippedItemsInfo.boots) {
                    return true;
                }
        }

        if (equippedItemsInfo.mainHand != null && customDropItem == equippedItemsInfo.mainHand) {
            return true;
        }

        return equippedItemsInfo.offhand != null && customDropItem == equippedItemsInfo.offhand;
    }

    private boolean madePlayerLevelRequirement(final @NotNull CustomDropProcessingInfo info,
        final @NotNull CustomDropBase dropBase) {

        if (dropBase.playerLevelVariable != null && !info.equippedOnly && !dropBase.playeerVariableMatches.isEmpty()) {
            final String papiResult = Utils.removeColorCodes(ExternalCompatibilityManager.getPapiPlaceholder(
                    info.mobKiller, dropBase.playerLevelVariable));

            boolean foundMatch = false;
            for (final String resultStr : dropBase.playeerVariableMatches){
                if (Utils.matchWildcardString(papiResult, resultStr)){
                    foundMatch = true;
                    if (isCustomDropsDebuggingEnabled()) {
                        if (dropBase instanceof CustomDropItem) {
                            info.addDebugMessage(String.format(
                                    "&8 - &7Mob: &b%s&7, item: %s, PAPI val: %s, matched: %s",
                                    info.lmEntity.getTypeName(), ((CustomDropItem) dropBase).getMaterial(),
                                    papiResult, resultStr));
                        } else {
                            info.addDebugMessage(String.format(
                                    "&8 - &7Mob: &b%s&7, (customCommand), PAPI val: %s, matched: %s",
                                    info.lmEntity.getTypeName(), papiResult, resultStr));
                        }
                    }
                    break;
                }
            }

            if (!foundMatch) {
                if (isCustomDropsDebuggingEnabled()) {
                    if (dropBase instanceof CustomDropItem) {
                        info.addDebugMessage(String.format(
                                "&8 - &7Mob: &b%s&7, item: %s, PAPI val: %s, no matches found",
                                info.lmEntity.getTypeName(), ((CustomDropItem) dropBase).getMaterial(),
                                papiResult));
                    } else {
                        info.addDebugMessage(String.format(
                                "&8 - &7Mob: &b%s&7, (customCommand), PAPI val: %s, no matches found",
                                info.lmEntity.getTypeName(), papiResult));
                    }
                }
                return false;
            }
        }

        if (!info.equippedOnly && (dropBase.minPlayerLevel > -1 || dropBase.maxPlayerLevel > -1)) {
            // check if the variable result has been cached already and use it if so
            final String variableToUse = Utils.isNullOrEmpty(dropBase.playerLevelVariable) ?
                "%level%" : dropBase.playerLevelVariable;
            final int levelToUse;
            if (info.playerLevelVariableCache.containsKey(variableToUse)) {
                levelToUse = info.playerLevelVariableCache.get(variableToUse);
            } else {
                //levelToUse = main.levelManager.getPlayerLevelSourceNumber(info.mobKiller, variableToUse);
                final PlayerLevelSourceResult result = main.levelManager.getPlayerLevelSourceNumber(
                    info.mobKiller, info.lmEntity, variableToUse);
                levelToUse = result.isNumericResult ? result.numericResult : 1;
                info.playerLevelVariableCache.put(variableToUse, levelToUse);
            }

            if (dropBase.minPlayerLevel > 0 && levelToUse < dropBase.minPlayerLevel ||
                dropBase.maxPlayerLevel > 0 && levelToUse > dropBase.maxPlayerLevel) {
                if (isCustomDropsDebuggingEnabled()) {
                    if (dropBase instanceof CustomDropItem) {
                        info.addDebugMessage(String.format(
                            "&8 - &7Mob: &b%s&7, item: %s, lvl-src: %s, minlvl: %s, maxlvl: %s player level criteria not met",
                            info.lmEntity.getTypeName(), ((CustomDropItem) dropBase).getMaterial(),
                            levelToUse, dropBase.minPlayerLevel, dropBase.maxPlayerLevel));
                    } else {
                        info.addDebugMessage(String.format(
                            "&8 - &7Mob: &b%s&7, (customCommand), lvl-src: %s, minlvl: %s, maxlvl: %s player level criteria not met",
                            info.lmEntity.getTypeName(), levelToUse, dropBase.minPlayerLevel,
                            dropBase.maxPlayerLevel));
                    }
                }
                return false;
            }
        }

        return true;
    }

    private void executeCommand(@NotNull final CustomCommand customCommand,
        @NotNull final CustomDropProcessingInfo info) {
        if (info.equippedOnly && !customCommand.runOnSpawn) {
            return;
        }
        if (!info.equippedOnly && !customCommand.runOnDeath) {
            return;
        }

        for (String command : customCommand.commands) {
            command = processRangedCommand(command, customCommand);
            command = main.levelManager.replaceStringPlaceholders(command, info.lmEntity,false,
                    info.lmEntity.getLivingEntity().getKiller());
            if (command.contains("%") && ExternalCompatibilityManager.hasPapiInstalled()) {
                command = ExternalCompatibilityManager.getPapiPlaceholder(info.mobKiller, command);
            }

            final int maxAllowedTimesToRun = ymlHelper.getInt(main.settingsCfg,
                "customcommand-amount-limit", 10);
            int timesToRun = customCommand.getAmount();

            if (customCommand.getHasAmountRange()) {
                timesToRun = main.random.nextInt(
                    customCommand.getAmountRangeMax() - customCommand.amountRangeMin + 1)
                    + customCommand.amountRangeMin;
            }

            if (timesToRun > maxAllowedTimesToRun) {
                timesToRun = maxAllowedTimesToRun;
            }

            final String debugCommand = timesToRun > 1 ?
                String.format("Command (%sx): ", timesToRun) : "Command: ";

            Utils.debugLog(main, DebugType.CUSTOM_COMMANDS, debugCommand + command);

            if (customCommand.delay > 0) {
                final String commandToRun = command;
                final int finalTimesToRun = timesToRun;
                final BukkitRunnable runnable = new BukkitRunnable() {
                    @Override
                    public void run() {
                        executeTheCommand(commandToRun, finalTimesToRun);
                    }
                };
                runnable.runTaskLater(main, customCommand.delay);
            } else {
                executeTheCommand(command, timesToRun);
            }
        }
    }

    private void executeTheCommand(final String command, final int timesToRun) {
        for (int i = 0; i < timesToRun; i++) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }

    @NotNull private String processRangedCommand(final @NotNull String command,
        final @NotNull CustomCommand cc) {
        if (cc.rangedEntries.isEmpty()) {
            return command;
        }

        String newCommand = command;

        for (final Map.Entry<String, String> rangeds : cc.rangedEntries.entrySet()) {
            final String rangedKey = rangeds.getKey();
            final String rangedValue = rangeds.getValue();
            if (!rangedValue.contains("-")) {
                newCommand = newCommand.replace("%" + rangedKey + "%", rangedValue);
                continue;
            }

            final String[] nums = rangedValue.split("-");
            if (nums.length != 2) {
                continue;
            }

            if (!Utils.isInteger(nums[0].trim()) || !Utils.isInteger(nums[1].trim())) {
                continue;
            }
            int min = Integer.parseInt(nums[0].trim());
            final int max = Integer.parseInt(nums[1].trim());
            if (max < min) {
                min = max;
            }

            final int rangedNum = main.random.nextInt(max - min + 1) + min;
            newCommand = newCommand.replace("%" + rangedKey + "%", String.valueOf(rangedNum));
        }

        return newCommand;
    }

    private ItemStack getCookedVariantOfMeat(@NotNull final ItemStack itemStack) {
        return switch (itemStack.getType()) {
            case BEEF -> new ItemStack(Material.COOKED_BEEF);
            case CHICKEN -> new ItemStack(Material.COOKED_CHICKEN);
            case COD -> new ItemStack(Material.COOKED_COD);
            case MUTTON -> new ItemStack(Material.COOKED_MUTTON);
            case PORKCHOP -> new ItemStack(Material.COOKED_PORKCHOP);
            case RABBIT -> new ItemStack(Material.COOKED_RABBIT);
            case SALMON -> new ItemStack(Material.COOKED_SALMON);
            default -> itemStack;
        };
    }

    public void addEntityEquippedItems(final @NotNull LivingEntity livingEntity,
        final @NotNull EquippedItemsInfo equippedItemsInfo) {
        this.customEquippedItems.put(livingEntity, equippedItemsInfo);
    }

    private boolean isCustomDropsDebuggingEnabled() {
        return main.companion.debugsEnabled.contains(DebugType.CUSTOM_DROPS);
    }

    public @Nullable CustomDropInstance getDropInstanceFromGroupId(final @Nullable String groupId){
        if (groupId == null) return null;
        return this.groupIdToInstance.get(groupId);
    }

    public void setDropInstanceFromId(final @NotNull String groupId, final @NotNull CustomDropInstance dropInstance){
        this.groupIdToInstance.put(groupId, dropInstance);
    }

    public @Nullable GroupLimits getGroupLimits(final @NotNull CustomDropBase dropBase){
        final GroupLimits limitsDefault = this.groupLimitsMap.get("default");

        if (dropBase.groupId == null || !this.groupLimitsMap.containsKey(dropBase.groupId)) {
            return limitsDefault;
        }

        return this.groupLimitsMap.get(dropBase.groupId);
    }

    public void clearGroupIdMappings(){
        this.groupIdToInstance.clear();
    }
}
