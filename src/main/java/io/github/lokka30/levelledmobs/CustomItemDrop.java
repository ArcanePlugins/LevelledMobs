package io.github.lokka30.levelledmobs;

import io.github.lokka30.levelledmobs.utils.Utils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CustomItemDrop {
    public int minLevel;
    public int maxLevel;
    public double dropChance;
    private int amount;
    public int groupId;
    private int damage;
    public boolean noMultiplier;
    public boolean noSpawner;
    public boolean isEquipped;
    public String customName;
    public List<String> lore;
    private boolean hasAmountRange;
    private boolean hasDamageRange;
    private int amountRangeMin;
    private int amountRangeMax;
    private int damageRangeMin;
    private int damageRangeMax;
    private Material material;
    private ItemStack itemStack;

    public CustomItemDrop(){
        this.minLevel = -1;
        this.maxLevel = -1;
        this.groupId = -1;
        this.amount = 1;
        this.dropChance = 0.2; // default drop chance if not specified
    }

    public boolean setAmountRangeFromString(final String numberRange){
        if (numberRange == null || numberRange.isEmpty()) return false;

        String[] nums = numberRange.split("-");
        if (nums.length != 2) return false;

        if (!Utils.isInteger(nums[0].trim()) || !Utils.isInteger(nums[1].trim())) return false;
        this.amountRangeMin = Integer.parseInt(nums[0].trim());
        this.amountRangeMax = Integer.parseInt(nums[1].trim());
        this.hasAmountRange = true;

        return true;
    }

    public boolean setDamageRangeFromString(final String numberRange){
        if (numberRange == null || numberRange.isEmpty()) return false;

        String[] nums = numberRange.split("-");
        if (nums.length != 2) return false;

        if (!Utils.isInteger(nums[0].trim()) || !Utils.isInteger(nums[1].trim())) return false;
        this.damageRangeMin = Integer.parseInt(nums[0].trim());
        this.damageRangeMax = Integer.parseInt(nums[1].trim());
        this.hasDamageRange = true;

        return true;
    }

    public int getAmount(){
        return this.amount;
    }

    public int getDamage() { return this.damage; }

    public void setAmount(int amount){
        this.amount = amount;
        if (this.amount > 64) this.amount = 64;
        if (this.amount < 1) this.amount = 1;
    }

    public void setDamage(int damage){
        this.damage = damage;
    }

    public int getAmountRangeMin(){
        return this.amountRangeMin;
    }

    public int getAmountRangeMax(){
        return this.amountRangeMax;
    }

    public boolean getHasAmountRange(){
        return this.hasAmountRange;
    }

    public int getDamageRangeMin(){
        return this.damageRangeMin;
    }

    public int getDamageRangeMax(){
        return this.damageRangeMax;
    }

    public boolean getHasDamageRange(){
        return this.hasDamageRange;
    }

    public void setMaterial(final Material material){
        this.material = material;
        this.itemStack = new ItemStack(this.material, 1);
    }

    public String getAmountAsString(){
        if (this.hasAmountRange)
            return String.format("%s-%s", this.amountRangeMin, this.amountRangeMax);
        else
            return String.valueOf(this.amount);
    }

    public String getDamageAsString(){
        if (this.hasDamageRange)
            return String.format("%s-%s", this.damageRangeMin, this.damageRangeMax);
        else
            return String.valueOf(this.amount);
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
}
