package io.github.lokka30.levelledmobs;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

public class CustomItemDrop {
    public LivingEntity associatedMob;
    public int minLevel;
    public int maxLevel;
    public double dropChance;
    public int amount;
    public Material material;
    public List<Enchantment> enchantments;

    public CustomItemDrop(){

    }

    /*
        ItemStack testEnchant = new ItemStack (Material.BOW, 1);
        ItemMeta testEnchantMeta = testEnchant.getItemMeta();
        testEnchantMeta.addEnchant(Enchantment.ARROW_FIRE, 10, true);
        testEnchant.setItemMeta(testEnchantMeta);
    */
}
