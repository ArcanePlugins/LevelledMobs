/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.customdrops;

import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.levelledmobs.rules.CustomDropsRuleSet;
import me.lokka30.microlib.messaging.MessageUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Used to store information when a custom drop item
 * is being requested either during mob spawn in for
 * equipped items or after mob death to get the items
 * the mob will potentially drop
 *
 * @author stumper66
 * @since 2.4.1
 */
public class CustomDropProcessingInfo {
    public CustomDropProcessingInfo() {
        this.groupIDsDroppedAlready = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.allDropInstances = new LinkedList<>();
        this.playerLevelVariableCache = new TreeMap<>();
    }

    public LivingEntityWrapper lmEntity;
    public int addition;
    public Player mobKiller;
    @NotNull
    final public Map<String, Integer> playerLevelVariableCache;
    public boolean isSpawner;
    public boolean equippedOnly;
    public boolean deathByFire;
    public boolean wasKilledByPlayer;
    public EntityDamageEvent.DamageCause deathCause;
    public boolean doNotMultiplyDrops;
    public boolean hasOverride;
    public boolean hasCustomDropId;
    public boolean madeOverallChance;
    public boolean hasEquippedItems;
    public String customDropId;
    public String playerLevelVariable;
    public List<ItemStack> newDrops;
    @Nonnull
    final public Map<String, Integer> groupIDsDroppedAlready;
    public Map<Integer, List<CustomDropBase>> prioritizedDrops;
    @Nullable
    public CustomDropsRuleSet dropRules;
    @NotNull
    final public List<CustomDropInstance> allDropInstances;
    public StringBuilder debugMessages;

    public void addDebugMessage(final String message){
        if (this.debugMessages == null)
            this.debugMessages = new StringBuilder();

        if (this.debugMessages.length() > 0)
            this.debugMessages.append(System.lineSeparator());

        this.debugMessages.append(message);
    }

    public void writeAnyDebugMessages(){
        if (this.debugMessages == null) return;

        Utils.logger.info(MessageUtils.colorizeAll(this.debugMessages.toString()));
        this.debugMessages.setLength(0);
    }
}
