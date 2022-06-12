/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.customdrops;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.rules.CustomDropsRuleSet;
import me.lokka30.levelledmobs.util.Utils;
import me.lokka30.microlib.messaging.MessageUtils;
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
        this.allDropInstances = new LinkedList<>();
        this.playerLevelVariableCache = new TreeMap<>();
        this.stackToItem = new LinkedList<>();
    }

    public LivingEntityWrapper lmEntity;
    int addition;
    Player mobKiller;
    @NotNull
    final Map<String, Integer> playerLevelVariableCache;
    boolean isSpawner;
    boolean equippedOnly;
    boolean deathByFire;
    boolean wasKilledByPlayer;
    DeathCause deathCause;
    boolean doNotMultiplyDrops;
    boolean hasOverride;
    boolean hasCustomDropId;
    boolean hasEquippedItems;
    public String customDropId;
    List<ItemStack> newDrops;
    @NotNull
    final Map<String, Integer> groupIDsDroppedAlready;
    Map<Integer, List<CustomDropBase>> prioritizedDrops;
    @Nullable CustomDropsRuleSet dropRules;
    @NotNull
    final List<CustomDropInstance> allDropInstances;
    private StringBuilder debugMessages;
    public final List<Map.Entry<ItemStack, CustomDropItem>> stackToItem;

    void addDebugMessage(final String message) {
        if (this.debugMessages == null) {
            this.debugMessages = new StringBuilder();
        }

        if (this.debugMessages.length() > 0) {
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
