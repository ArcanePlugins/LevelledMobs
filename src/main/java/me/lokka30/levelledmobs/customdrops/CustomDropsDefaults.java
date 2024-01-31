/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.customdrops;

import java.util.LinkedList;
import java.util.List;
import me.lokka30.levelledmobs.misc.CachedModalList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds all default values for either all custom drop items
 *
 * @author stumper66
 * @since 2.4.0
 */
public class CustomDropsDefaults {
    boolean noMultiplier;
    boolean noSpawner;
    public boolean override;
    boolean playerCausedOnly;
    boolean onlyDropIfEquipped;
    public int amount;
    public int priority;
    public int minLevel;
    public int maxLevel;
    public boolean equipOffhand;
    public boolean equipOnHelmet;
    int customModelData;
    int maxDropGroup;
    int minPlayerLevel;
    int maxPlayerLevel;
    public SlidingChance chance;
    public boolean useChunkKillMax;
    @Nullable SlidingChance equippedChance;
    @Nullable SlidingChance overallChance;
    String groupId;
    String playerLevelVariable;
    String nbtData;
    List<String> itemFlagsStrings;
    public final List<String> permissions;
    final List<String> overallPermissions;
    CachedModalList<DeathCause> causeOfDeathReqs;
    public String externalType;
    public String externalItemId;
    public Double externalAmount;
    public boolean runOnSpawn;
    public boolean runOnDeath;

    CustomDropsDefaults() {
        // these are the defaults of the defaults
        this.amount = 1;
        this.minLevel = -1;
        this.maxLevel = -1;
        this.minPlayerLevel = -1;
        this.maxPlayerLevel = -1;
        this.customModelData = -1;
        this.priority = 0;
        this.maxDropGroup = 0;
        this.noMultiplier = false;
        this.noSpawner = false;
        this.override = false;
        this.playerCausedOnly = false;
        this.permissions = new LinkedList<>();
        this.overallPermissions = new LinkedList<>();
        this.runOnSpawn = false;
        this.runOnDeath = true;
        this.equipOnHelmet = false;
    }

    void setDefaultsFromDropItem(@NotNull final CustomDropBase dropBase) {
        if (this.chance != null)
            this.chance.setFromInstance(dropBase.chance);
        else
            this.chance = dropBase.chance;
        this.useChunkKillMax = dropBase.useChunkKillMax;
        this.amount = dropBase.getAmount();
        this.minLevel = dropBase.minLevel;
        this.maxLevel = dropBase.maxLevel;
        this.priority = dropBase.priority;
        this.maxDropGroup = dropBase.maxDropGroup;
        this.noSpawner = dropBase.noSpawner;
        this.playerCausedOnly = dropBase.playerCausedOnly;
        if (!"default".equals(dropBase.groupId)){
            this.groupId = dropBase.groupId;
        }
        this.minPlayerLevel = dropBase.minPlayerLevel;
        this.maxPlayerLevel = dropBase.maxPlayerLevel;
        this.playerLevelVariable = dropBase.playerLevelVariable;
        this.permissions.addAll(dropBase.permissions);
        this.causeOfDeathReqs = dropBase.causeOfDeathReqs;

        if (dropBase instanceof final CustomDropItem dropItem) {
            this.customModelData = dropItem.customModelDataId;
            if (this.equippedChance != null)
                this.equippedChance.setFromInstance(dropItem.equippedChance);
            else
                this.equippedChance = dropItem.equippedChance;
            this.noMultiplier = dropItem.noMultiplier;
            this.onlyDropIfEquipped = dropItem.onlyDropIfEquipped;
            this.externalType = dropItem.externalType;
            this.externalItemId = dropItem.externalItemId;
            this.externalAmount = dropItem.externalAmount;
            this.equipOffhand = dropItem.equipOffhand;
            this.nbtData = dropItem.nbtData;
            this.itemFlagsStrings = dropItem.itemFlagsStrings;
            this.equipOnHelmet = dropItem.equipOnHelmet;
        } else if (dropBase instanceof final CustomCommand command) {
            this.runOnSpawn = command.runOnSpawn;
            this.runOnDeath = command.runOnDeath;
        }
    }
}
