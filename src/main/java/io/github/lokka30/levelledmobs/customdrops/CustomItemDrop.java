package io.github.lokka30.levelledmobs.customdrops;

import io.github.lokka30.levelledmobs.misc.Utils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class CustomItemDrop implements Cloneable {
    public int minLevel;
    public int maxLevel;
    public int groupId;
    public int customModelDataId;
    public double dropChance;
    public boolean noMultiplier;
    public boolean noSpawner;
    public boolean isEquipped;
    public String customName;
    public List<String> lore;
    public Set<String> excludedMobs;
    private int damage;
    private int amount;
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
        this.customModelDataId = -1;
        this.amount = 1;
        this.dropChance = 0.2; // default drop chance if not specified
        this.excludedMobs = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    }

    public CustomItemDrop cloneItem(){
        CustomItemDrop copy = null;
        try {
            copy = (CustomItemDrop) super.clone();
            copy.itemStack = this.itemStack.clone();
        }
        catch (Exception ignored){ }

        return copy;
    }

    private static String getContent(Object aClass, String name) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field declaredField = aClass.getClass().getDeclaredField(name);
        declaredField.setAccessible(true);
        return (String) declaredField.get(aClass);
    }

    public boolean setAmountRangeFromString(final String numberOrNumberRange){
        if (numberOrNumberRange == null || numberOrNumberRange.isEmpty()) return false;

        if (!numberOrNumberRange.contains("-")){
            if (!Utils.isInteger(numberOrNumberRange)) return false;

            this.amount = Integer.parseInt(numberOrNumberRange);
            this.hasAmountRange = false;
            return true;
        }

        final String[] nums = numberOrNumberRange.split("-");
        if (nums.length != 2) return false;

        if (!Utils.isInteger(nums[0].trim()) || !Utils.isInteger(nums[1].trim())) return false;
        this.amountRangeMin = Integer.parseInt(nums[0].trim());
        this.amountRangeMax = Integer.parseInt(nums[1].trim());
        this.hasAmountRange = true;

        return true;
    }

    public boolean setDamageRangeFromString(final String numberOrNumberRange){
        if (numberOrNumberRange == null || numberOrNumberRange.isEmpty()) return false;

        if (!numberOrNumberRange.contains("-")){
            if (!Utils.isInteger(numberOrNumberRange)) return false;

            this.damage = Integer.parseInt(numberOrNumberRange);
            this.hasDamageRange = false;
            this.damageRangeMax = 0;
            this.damageRangeMin = 0;
            return true;
        }

        String[] nums = numberOrNumberRange.split("-");
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
        this.hasDamageRange = false;
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
}
