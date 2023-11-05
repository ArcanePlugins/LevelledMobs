/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.customdrops;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.util.Utils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This holds all the attributes set for a custom drop item
 *
 * @author stumper66
 * @since 2.5.0
 */
public class CustomDropItem extends CustomDropBase {

    public int customModelDataId;
    public float equippedSpawnChance;
    public boolean noMultiplier;
    public boolean onlyDropIfEquipped;
    public boolean equipOnHelmet;
    public String customName;
    public String mobHeadTexture;
    public List<String> lore;
    public @Nullable List<ItemFlag> itemFlags;
    public @Nullable List<String> itemFlagsStrings;
    private boolean hasDamageRange;
    private int damage;
    private int damageRangeMin;
    private int damageRangeMax;
    public boolean equipOffhand;
    public UUID customPlayerHeadId;
    private Material material;
    private ItemStack itemStack;
    public boolean isExternalItem;
    public String externalPluginName;
    public String externalType;
    public String externalItemId;
    public String nbtData;
    public Double externalAmount;
    public Map<String, Object> externalExtras;
    public EnchantmentChances enchantmentChances;

    @SuppressWarnings("unused")
    public CustomDropItem(@NotNull final LevelledMobs levelledMobs) {
        super(levelledMobs.customDropsHandler.customDropsParser.getDefaults());
        setDefaults(levelledMobs.customDropsHandler.customDropsParser.getDefaults());
    }

    public CustomDropItem(@NotNull final CustomDropsDefaults defaults) {
        super(defaults);
        setDefaults(defaults);
    }

    private void setDefaults(@NotNull final CustomDropsDefaults defaults){
        this.customModelDataId = defaults.customModelData;
        this.chance = defaults.chance;
        this.maxLevel = defaults.maxLevel;
        this.minLevel = defaults.minLevel;
        this.groupId = defaults.groupId;
        this.maxDropGroup = defaults.maxDropGroup;
        this.equippedSpawnChance = defaults.equippedSpawnChance;
        this.noMultiplier = defaults.noMultiplier;
        this.onlyDropIfEquipped = defaults.onlyDropIfEquipped;
        this.equipOffhand = defaults.equipOffhand;
        this.equipOnHelmet = defaults.equipOnHelmet;
    }

    public CustomDropItem cloneItem() {
        CustomDropItem copy = null;
        try {
            copy = (CustomDropItem) super.clone();
            copy.itemStack = this.itemStack.clone();
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return copy;
    }

    public boolean setDamageRangeFromString(final String numberOrNumberRange) {
        if (numberOrNumberRange == null || numberOrNumberRange.isEmpty()) {
            return false;
        }

        if (!numberOrNumberRange.contains("-")) {
            if (!Utils.isInteger(numberOrNumberRange)) {
                return false;
            }

            this.damage = Integer.parseInt(numberOrNumberRange);
            this.hasDamageRange = false;
            this.damageRangeMax = 0;
            this.damageRangeMin = 0;
            return true;
        }

        final String[] nums = numberOrNumberRange.split("-");
        if (nums.length != 2) {
            return false;
        }

        if (!Utils.isInteger(nums[0].trim()) || !Utils.isInteger(nums[1].trim())) {
            return false;
        }
        this.damageRangeMin = Integer.parseInt(nums[0].trim());
        this.damageRangeMax = Integer.parseInt(nums[1].trim());
        this.hasDamageRange = true;

        return true;
    }

    public int getDamage() {
        return this.damage;
    }

    public void setDamage(final int damage) {
        this.damage = damage;
        this.hasDamageRange = false;
    }

    public int getDamageRangeMin() {
        return this.damageRangeMin;
    }

    public int getDamageRangeMax() {
        return this.damageRangeMax;
    }

    public boolean getHasDamageRange() {
        return this.hasDamageRange;
    }

    public void setMaterial(final Material material) {
        this.material = material;
        this.itemStack = new ItemStack(this.material, 1);
    }

    public @NotNull String getDamageAsString() {
        if (this.hasDamageRange) {
            return String.format("%s-%s", this.damageRangeMin, this.damageRangeMax);
        } else {
            return String.valueOf(this.damage);
        }
    }

    public Material getMaterial() {
        return this.material;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public void setItemStack(final @NotNull ItemStack itemStack) {
        this.itemStack = itemStack;
        this.material = itemStack.getType();
    }

    public String toString() {
        return String.format("%s, amount: %s, chance: %s, equipped: %s",
            this.material.name(), this.getAmountAsString(), this.chance, this.equippedSpawnChance);
    }
}
