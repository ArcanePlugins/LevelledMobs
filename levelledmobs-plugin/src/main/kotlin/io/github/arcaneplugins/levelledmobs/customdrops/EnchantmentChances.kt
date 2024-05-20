package io.github.arcaneplugins.levelledmobs.customdrops

import org.bukkit.enchantments.Enchantment

/**
 * Used in conjunction with custom drops for handling
 * enchantment level chances
 *
 * @author stumper66
 * @since 3.7.0
 */
class EnchantmentChances {
    val items = mutableMapOf<Enchantment, MutableMap<Int, Float>>()
    val options = mutableMapOf<Enchantment, ChanceOptions>()

    val isEmpty: Boolean
        get() = items.isEmpty()

    class ChanceOptions {
        var defaultLevel: Int? = null
        var doShuffle: Boolean = true
    }

    override fun toString(): String {
        return "EnchantmentChances, ${items.size} items"
    }
}
