package me.lokka30.levelledmobs.customdrops;

import org.bukkit.enchantments.Enchantment;

import java.util.HashMap;
import java.util.Map;

/**
 * Used in conjunction with custom drops for handling
 * enchantment level chances
 *
 * @author stumper66
 * @since 3.7.0
 */
public class EnchantmentChances {
    public EnchantmentChances(){
        this.items = new HashMap<>();
        this.options = new HashMap<>();
    }

    public final Map<Enchantment, Map<Integer, Float>> items;
    public final Map<Enchantment, ChanceOptions> options;

    public boolean isEmpty(){
        return this.items.isEmpty();
    }

    public static class ChanceOptions{
        public Integer defaultLevel;
        public boolean doShuffle = true;
    }

    public String toString(){
        return String.format("EnchantmentChances, %s items", this.items.size());
    }
}
