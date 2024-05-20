package io.github.arcaneplugins.levelledmobs.customdrops

import org.bukkit.inventory.ItemStack

/**
 * Used internally to determine if the mob's vanilla items should be removed or not
 *
 * @author stumper66
 * @since 2.6.0
 */
class CustomDropResult(
    val stackToItem: MutableList<Map.Entry<ItemStack, CustomDropItem>>,
    val hasOverride: Boolean,
    val didAnythingDrop: Boolean
)