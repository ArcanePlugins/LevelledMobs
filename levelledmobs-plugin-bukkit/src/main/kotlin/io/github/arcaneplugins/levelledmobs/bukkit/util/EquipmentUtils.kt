package io.github.arcaneplugins.levelledmobs.bukkit.util

import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import java.util.function.Predicate

object EquipmentUtils {
    fun findSimilarItemStackInEntity(
        entity: LivingEntity,
        itemStack: ItemStack,
        predicate: Predicate<ItemStack>
    ): ItemStack?{
        val equip = findSimilarItemStackInEntityEquipment(entity, itemStack, predicate)
        return equip ?: findSimilarItemStackInEntityInventory(entity, itemStack, predicate)
    }

    private fun findSimilarItemStackInEntityEquipment(
        entity: LivingEntity,
        itemStack: ItemStack,
        predicate: Predicate<ItemStack>
    ): ItemStack?{
        val equipment = entity.equipment ?: return null

        for (slot in EquipmentSlot.entries) {
            val itemInSlot = equipment.getItem(slot)
            if (predicate.test(itemInSlot) && itemStack.isSimilar(itemInSlot)) return itemInSlot
        }

        return null
    }

    private fun findSimilarItemStackInEntityInventory(
        entity: LivingEntity,
        itemStack: ItemStack,
        predicate: Predicate<ItemStack>
    ): ItemStack?{
        if (entity !is InventoryHolder) return null
        val ih = entity as InventoryHolder

        for (otherStack in ih.inventory.contents) {
            if (otherStack == null) continue
            if (predicate.test(itemStack) && itemStack.isSimilar(otherStack)) return otherStack
        }

        return null
    }
}