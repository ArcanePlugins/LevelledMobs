/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.customdrops;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import me.lokka30.levelledmobs.wrappers.LivingEntityWrapper;
import me.lokka30.levelledmobs.rules.CustomDropsRuleSet;
import me.lokka30.levelledmobs.util.MessageUtils;
import me.lokka30.levelledmobs.util.Utils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Used to store information when a custom drop item is being requested either during mob spawn in
 * for equipped items or after mob death to get the items the mob will potentially drop
 *
 * @author stumper66
 * @since 2.4.1
 */
class CustomDropProcessingInfo {

    CustomDropProcessingInfo() {
        this.groupIDsDroppedAlready = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.itemsDroppedById = new TreeMap<>();
        this.allDropInstances = new LinkedList<>();
        this.playerLevelVariableCache = new TreeMap<>();
        this.stackToItem = new LinkedList<>();
    }

    public LivingEntityWrapper lmEntity;
    Player mobKiller;
    final @NotNull Map<String, Integer> playerLevelVariableCache;
    DeathCause deathCause;
    double addition;
    boolean isSpawner;
    boolean equippedOnly;
    boolean deathByFire;
    boolean wasKilledByPlayer;
    boolean doNotMultiplyDrops;
    boolean hasOverride;
    boolean hasCustomDropId;
    boolean hasEquippedItems;
    int retryNumber;
    public String customDropId;
    List<ItemStack> newDrops;
    public CustomDropInstance dropInstance;
    final @NotNull Map<String, Integer> groupIDsDroppedAlready;
    final @NotNull Map<UUID, Integer> itemsDroppedById;
    Map<Integer, List<CustomDropBase>> prioritizedDrops;
    @Nullable CustomDropsRuleSet dropRules;
    final @NotNull List<CustomDropInstance> allDropInstances;
    private StringBuilder debugMessages;
    public final List<Map.Entry<ItemStack, CustomDropItem>> stackToItem;

    public void itemGotDropped(final @NotNull CustomDropBase dropBase){
        final String useGroupId = dropBase.groupId != null ?
                dropBase.groupId : "default";

        int count = groupIDsDroppedAlready.containsKey(useGroupId) ?
                groupIDsDroppedAlready.get(useGroupId) + 1 :
                1;

        groupIDsDroppedAlready.put(useGroupId, count);

        count = itemsDroppedById.containsKey(dropBase.uid) ?
                itemsDroppedById.get(dropBase.uid) + 1 :
                1;

        itemsDroppedById.put(dropBase.uid, count);
    }

    public int getDropItemsCountForGroup(final @NotNull CustomDropBase dropBase){
        final String useGroupId = dropBase.groupId != null ?
                dropBase.groupId : "default";

        return groupIDsDroppedAlready.getOrDefault(useGroupId, 0);
    }

    public int getItemsDropsById(final @NotNull CustomDropBase dropBase){
        return itemsDroppedById.getOrDefault(dropBase.uid, 0);
    }

    public int getItemsDropsByGroup(final @NotNull CustomDropBase dropBase){
        final String useGroupId = dropBase.groupId != null ?
                dropBase.groupId : "default";

        return groupIDsDroppedAlready.getOrDefault(useGroupId, 0);
    }

    void addDebugMessage(final String message) {
        if (this.debugMessages == null) {
            this.debugMessages = new StringBuilder();
        }

        if (!this.debugMessages.isEmpty()) {
            this.debugMessages.append(System.lineSeparator());
        }

        this.debugMessages.append(message);
    }

    void writeAnyDebugMessages() {
        if (this.debugMessages == null) {
            return;
        }

        Utils.logger.info(MessageUtils.colorizeAll(this.debugMessages.toString()));
        this.debugMessages.setLength(0);
    }
}
