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
 */
public class CustomDropItem extends CustomDropBase {
    public int customModelDataId;
    public double equippedSpawnChance;
    public boolean noMultiplier;
    public String customName;
    public String mobHeadTexture;
    public List<String> lore;
    public List<ItemFlag> itemFlags;
    private boolean hasDamageRange;
    private int damage;
    private int damageRangeMin;
    private int damageRangeMax;
    public UUID customPlayerHeadId;
    private Material material;
    private ItemStack itemStack;

    public CustomDropItem(@NotNull final CustomDropsDefaults defaults) {
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
    }

    public CustomDropItem cloneItem() {
        CustomDropItem copy = null;
        try {
            copy = (CustomDropItem) super.clone();
            copy.itemStack = this.itemStack.clone();
        } catch (Exception ignored) {}

        return copy;
    }

    private static String getContent(@NotNull Object aClass, String name) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field declaredField = aClass.getClass().getDeclaredField(name);
        declaredField.setAccessible(true);
        return (String) declaredField.get(aClass);
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

    public int getDamage() { return this.damage; }

    public void setDamage(int damage){
        this.damage = damage;
        this.hasDamageRange = false;
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

    public String toString(){
        return String.format("%s, amount: %s, chance: %s, equipped: %s",
                this.material.name(), this.getAmountAsString(), this.chance, this.equippedSpawnChance);
    }
}
