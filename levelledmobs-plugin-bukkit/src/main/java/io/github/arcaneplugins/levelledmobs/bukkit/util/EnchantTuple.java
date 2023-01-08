package io.github.arcaneplugins.levelledmobs.bukkit.util;

import javax.annotation.Nonnull;
import org.bukkit.enchantments.Enchantment;

public class EnchantTuple {

    private final Enchantment enchantment;
    private final float chance;
    private final int strength;

    public EnchantTuple(
        final @Nonnull Enchantment enchantment,
        final float chance,
        final int strength
    ) {
        this.enchantment = enchantment;
        this.chance = chance;
        this.strength = strength;
    }

    @Override
    public String toString() {
        return """
            {"type"="%s", "chance"=%s, "strength"=%s}"""
            .formatted(getEnchantment(), getChance(),getStrength());
    }

    public @Nonnull Enchantment getEnchantment() {
        return enchantment;
    }

    public float getChance() {
        return chance;
    }

    public int getStrength() {
        return strength;
    }
}
