package io.github.lokka30.levelledmobs;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CustomItemDrop {
    final public EntityType associatedMob;
    public int minLevel;
    public int maxLevel;
    public double dropChance;
    public int amount;
    public boolean noMultiplier;
    public List<Enchantment> enchantments;
    private Material material;
    private ItemStack itemStack;
    private static final double defaultCustomItemDropChance = 0.2;

    public CustomItemDrop(EntityType associatedMob){
        this.associatedMob = associatedMob;
        this.minLevel = -1;
        this.maxLevel = -1;
        this.amount = 1;
        this.dropChance = defaultCustomItemDropChance;
    }

    public void setMaterial(Material material){
        this.material = material;
        this.itemStack = new ItemStack(this.material, 1);
    }

    public Material getMaterial(){
        return this.material;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public void setItemStack(ItemStack itemStack){
        this.itemStack = itemStack;
    }
}
