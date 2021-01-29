package io.github.lokka30.levelledmobs;

import io.github.lokka30.levelledmobs.utils.Utils;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

public class CustomItemDrop {
    final public EntityType associatedMob;
    public int minLevel;
    public int maxLevel;
    public double dropChance;
    public int amount;
    public int groupId;
    public boolean noMultiplier;
    public boolean noSpawner;
    public CustomDropsUniversalGroups entityGroup;
    private boolean hasAmountRange;
    private int amountRangeMin;
    private int amountRangeMax;
    private Material material;
    private ItemStack itemStack;

    public CustomItemDrop(final EntityType associatedMob){
        this.associatedMob = associatedMob;
        setDefaults();
    }

    public CustomItemDrop(final CustomDropsUniversalGroups entityGroup){
        this.associatedMob = null;
        this.entityGroup = entityGroup;
        setDefaults();
    }

    private void setDefaults(){
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

    public int getamountRangeMin(){
        return this.amountRangeMin;
    }

    public int getamountRangeMax(){
        return this.amountRangeMax;
    }

    public boolean getHasAmountRange(){
        return this.hasAmountRange;
    }

    public void setMaterial(final Material material){
        this.material = material;
        this.itemStack = new ItemStack(this.material, 1);
    }

    public boolean getIsGroup(){
        return this.entityGroup != null;
    }

    public String getAmountAsString(){
        if (this.hasAmountRange)
            return String.format("%s-%s", this.amountRangeMin, this.amountRangeMax);
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
