package io.github.arcaneplugins.levelledmobs.bukkit.util

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.function.Predicate

object PlayerUtils {
    fun findItemStackInEitherHand(
        player: Player,
        predicate: Predicate<ItemStack?>
    ): FoundItemInHandResult?{
        val inventory = player.inventory

        if (predicate.test(inventory.itemInMainHand)) {
            return FoundItemInHandResult(inventory.itemInMainHand, true)
        } else if (predicate.test(inventory.itemInOffHand)) {
            return FoundItemInHandResult(inventory.itemInOffHand, false)
        }

        return null
    }

    class FoundItemInHandResult(
        val itemStack: ItemStack,
        val inMainHand: Boolean
    )
}