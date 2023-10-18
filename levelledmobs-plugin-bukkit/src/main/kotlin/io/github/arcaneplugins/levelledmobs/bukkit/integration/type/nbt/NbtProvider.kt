package io.github.arcaneplugins.levelledmobs.bukkit.integration.type.nbt

import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack

interface NbtProvider {
    fun addNbtTag(itemStack: ItemStack, tag: String): NbtModificationResult

    fun addNbtTag(entity: LivingEntity, tag: String): NbtModificationResult

    fun getNbtDump(itemStack: ItemStack): String

    fun getNbtDump(entity: LivingEntity): String
}