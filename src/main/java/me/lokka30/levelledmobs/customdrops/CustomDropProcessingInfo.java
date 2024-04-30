/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.customdrops;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.managers.DebugManager;
import me.lokka30.levelledmobs.misc.DebugType;
import me.lokka30.levelledmobs.wrappers.LivingEntityWrapper;
import me.lokka30.levelledmobs.rules.CustomDropsRuleSet;
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
public class CustomDropProcessingInfo {

    CustomDropProcessingInfo() {
        this.groupIDsDroppedAlready = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.itemsDroppedById = new TreeMap<>();
        this.allDropInstances = new LinkedList<>();
        this.playerLevelVariableCache = new TreeMap<>();
        this.stackToItem = new LinkedList<>();
        this.debugMessages = new TreeMap<>();
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
    float equippedChanceRole;
    public @Nullable GroupLimits groupLimits;
    public String customDropId;
    List<ItemStack> newDrops;
    public CustomDropInstance dropInstance;
    final @NotNull Map<String, Integer> groupIDsDroppedAlready;
    final @NotNull Map<UUID, Integer> itemsDroppedById;
    Map<Integer, List<CustomDropBase>> prioritizedDrops;
    @Nullable CustomDropsRuleSet dropRules;
    final @NotNull List<CustomDropInstance> allDropInstances;
    private final Map<DebugType, StringBuilder> debugMessages;
    public final List<Map.Entry<ItemStack, CustomDropItem>> stackToItem;

    public void itemGotDropped(final @NotNull CustomDropBase dropBase, final int amountDropped){

        if (dropBase.hasGroupId()) {
            final int count = groupIDsDroppedAlready.getOrDefault(
                    dropBase.groupId, 0) + amountDropped;

            groupIDsDroppedAlready.put(dropBase.groupId, count);
        }

        final int count = itemsDroppedById.getOrDefault(dropBase.uid, 0) + amountDropped;
        itemsDroppedById.put(dropBase.uid, count);
    }

    public int getDropItemsCountForGroup(final @NotNull CustomDropBase dropBase){
        final String useGroupId = dropBase.hasGroupId() ?
                dropBase.groupId : "default";

        return groupIDsDroppedAlready.getOrDefault(useGroupId, 0);
    }

    public int getItemsDropsById(final @NotNull CustomDropBase dropBase){
        return itemsDroppedById.getOrDefault(dropBase.uid, 0);
    }

    public int getItemsDropsByGroup(final @NotNull CustomDropBase dropBase){
        final String useGroupId = dropBase.hasGroupId() ?
                dropBase.groupId : "default";

        return groupIDsDroppedAlready.getOrDefault(useGroupId, 0);
    }

    void addDebugMessage(final String message) {
        addDebugMessage(DebugType.CUSTOM_DROPS, message);
    }

    void addDebugMessage(final DebugType debugType, final String message) {
        if (!LevelledMobs.getInstance().debugManager.isDebugTypeEnabled(debugType)){
            return;
        }

        final StringBuilder sb = this.debugMessages.computeIfAbsent(
                debugType, k -> new StringBuilder());

        if (!sb.isEmpty())
            sb.append(System.lineSeparator());

        sb.append(message);
    }

    void writeAnyDebugMessages() {
        if (this.debugMessages.isEmpty()) return;

        for (DebugType debugType : this.debugMessages.keySet()){
            DebugManager.log(debugType, lmEntity, () -> this.debugMessages.get(debugType).toString());
        }

        this.debugMessages.clear();
    }
}
