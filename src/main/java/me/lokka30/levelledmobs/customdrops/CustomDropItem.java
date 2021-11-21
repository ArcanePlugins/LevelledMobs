/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.customdrops;

import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

/**
 * This holds all the attributes set for a
 * custom drop item
 *
 * @author stumper66
 * @since 2.5.0
 */
public class CustomDropItem extends CustomDropBase {
    int customModelDataId;
    float equippedSpawnChance;
    boolean noMultiplier;
    boolean onlyDropIfEquipped;
    public String customName;
    public String mobHeadTexture;
    String nbtData;
    public List<String> lore;
    List<ItemFlag> itemFlags;
    private boolean hasDamageRange;
    private int damage;
    private int damageRangeMin;
    private int damageRangeMax;
    public UUID customPlayerHeadId;
    private Material material;
    private ItemStack itemStack;

    CustomDropItem(@NotNull final CustomDropsDefaults defaults) {
        super(defaults);
        if (!Utils.isNullOrEmpty(defaults.damage)) this.setDamageRangeFromString(defaults.damage);
        this.customModelDataId = defaults.customModelData;
        this.chance = defaults.chance;
        this.maxLevel = defaults.maxLevel;
        this.minLevel = defaults.minLevel;
        this.groupId = defaults.groupId;
        this.maxDropGroup = defaults.maxDropGroup;
        this.equippedSpawnChance = defaults.equippedSpawnChance;
        this.noMultiplier = defaults.noMultiplier;
        this.onlyDropIfEquipped = defaults.onlyDropIfEquipped;
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

    private static String getContent(@NotNull final Object aClass, final String name) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        final Field declaredField = aClass.getClass().getDeclaredField(name);
        declaredField.setAccessible(true);
        return (String) declaredField.get(aClass);
    }

    boolean setDamageRangeFromString(final String numberOrNumberRange){
        if (numberOrNumberRange == null || numberOrNumberRange.isEmpty()) return false;

        if (!numberOrNumberRange.contains("-")){
            if (!Utils.isInteger(numberOrNumberRange)) return false;

            this.damage = Integer.parseInt(numberOrNumberRange);
            this.hasDamageRange = false;
            this.damageRangeMax = 0;
            this.damageRangeMin = 0;
            return true;
        }

        final String[] nums = numberOrNumberRange.split("-");
        if (nums.length != 2) return false;

        if (!Utils.isInteger(nums[0].trim()) || !Utils.isInteger(nums[1].trim())) return false;
        this.damageRangeMin = Integer.parseInt(nums[0].trim());
        this.damageRangeMax = Integer.parseInt(nums[1].trim());
        this.hasDamageRange = true;

        return true;
    }

    public int getDamage() { return this.damage; }

    public void setDamage(final int damage){
        this.damage = damage;
        this.hasDamageRange = false;
    }

    int getDamageRangeMin(){
        return this.damageRangeMin;
    }

    int getDamageRangeMax(){
        return this.damageRangeMax;
    }

    boolean getHasDamageRange(){
        return this.hasDamageRange;
    }

    public void setMaterial(final Material material){
        this.material = material;
        this.itemStack = new ItemStack(this.material, 1);
    }

    String getDamageAsString(){
        if (this.hasDamageRange)
            return String.format("%s-%s", this.damageRangeMin, this.damageRangeMax);
        else
            return String.valueOf(this.damage);
    }

    public Material getMaterial(){
        return this.material;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public void setItemStack(final ItemStack itemStack){
        this.itemStack = itemStack;
    }

    public String toString(){
        return String.format("%s, amount: %s, chance: %s, equipped: %s",
                this.material.name(), this.getAmountAsString(), this.chance, this.equippedSpawnChance);
    }
}
