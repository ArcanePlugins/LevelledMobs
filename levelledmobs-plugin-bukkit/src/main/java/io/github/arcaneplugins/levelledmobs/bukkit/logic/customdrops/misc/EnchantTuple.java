package io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.misc;

import javax.annotation.Nonnull;
import org.bukkit.enchantments.Enchantment;

public class EnchantTuple {

    private final Enchantment enchantment;
    private final int chance;
    private final int strength;

    public EnchantTuple(
        final @Nonnull Enchantment enchantment,
        final int chance,
        final int strength
    ) {
        this.enchantment = enchantment;
        this.chance = chance;
        this.strength = strength;
    }

    public @Nonnull Enchantment getEnchantment() {
        return enchantment;
    }

    public int getChance() {
        return chance;
    }

    public int getStrength() {
        return strength;
    }
}
