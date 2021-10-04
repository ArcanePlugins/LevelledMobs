/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.customdrops;

import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

/**
 * Holds all default values for either all custom
 * drop items
 *
 * @author stumper66
 * @since 2.4.0
 */
public class CustomDropsDefaults {
    public boolean noMultiplier;
    public boolean noSpawner;
    public boolean override;
    public boolean playerCausedOnly;
    public boolean onlyDropIfEquipped;
    public int amount;
    public int priority;
    public int minLevel;
    public int maxLevel;
    public int customModelData;
    public int maxDropGroup;
    public Integer minPlayerLevel;
    public Integer maxPlayerLevel;
    public double chance;
    public double equippedSpawnChance;
    public Double overallChance;
    public String groupId;
    public String damage;
    public String nbtData;
    public String playerLevelVariable;
    public List<ItemFlag> itemFlags;
    final public List<String> permissions;
    final public List<String> overallPermissions;

    public CustomDropsDefaults() {
        // these are the defaults of the defaults
        this.chance = 0.2;
        this.amount = 1;
        this.minLevel = -1;
        this.maxLevel = -1;
        this.customModelData = -1;
        this.priority = 0;
        this.equippedSpawnChance = 0.0;
        this.maxDropGroup = 0;
        this.noMultiplier = false;
        this.noSpawner = false;
        this.override = false;
        this.playerCausedOnly = false;
        this.permissions = new LinkedList<>();
        this.overallPermissions = new LinkedList<>();
    }

    public void setDefaultsFromDropItem(@NotNull final CustomDropItem drop) {
        this.chance = drop.chance;
        this.amount = drop.getAmount();
        this.minLevel = drop.minLevel;
        this.maxLevel = drop.maxLevel;
        this.customModelData = drop.customModelDataId;
        this.priority = drop.priority;
        this.equippedSpawnChance = drop.equippedSpawnChance;
        this.maxDropGroup = drop.maxDropGroup;
        this.noMultiplier = drop.noMultiplier;
        this.noSpawner = drop.noSpawner;
        this.playerCausedOnly = drop.playerCausedOnly;
        this.groupId = drop.groupId;
        this.nbtData = drop.nbtData;
        this.itemFlags = drop.itemFlags;
        this.minPlayerLevel = drop.minPlayerLevel;
        this.maxPlayerLevel = drop.maxPlayerLevel;
        this.playerLevelVariable = drop.playerLevelVariable;
        this.onlyDropIfEquipped = drop.onlyDropIfEquipped;
        this.permissions.addAll(drop.permissions);
    }
}
