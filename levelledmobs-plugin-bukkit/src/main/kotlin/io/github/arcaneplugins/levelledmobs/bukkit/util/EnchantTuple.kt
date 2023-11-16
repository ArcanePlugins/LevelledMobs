package io.github.arcaneplugins.levelledmobs.bukkit.util

import org.bukkit.enchantments.Enchantment

class EnchantTuple(
    val enchantment: Enchantment,
    val chance: Float,
    val strength: Int
) {
    override fun toString(): String {
        return "type=$enchantment, chance=$chance, strength=$strength"
    }
}