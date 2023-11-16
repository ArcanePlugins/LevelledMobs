package io.github.arcaneplugins.levelledmobs.bukkit.integration.impl

import de.tr7zw.nbtapi.NBTContainer
import de.tr7zw.nbtapi.NBTEntity
import de.tr7zw.nbtapi.NBTItem
import io.github.arcaneplugins.levelledmobs.bukkit.integration.Integration
import io.github.arcaneplugins.levelledmobs.bukkit.integration.IntegrationPriority
import io.github.arcaneplugins.levelledmobs.bukkit.integration.type.entity.EntityOwner
import io.github.arcaneplugins.levelledmobs.bukkit.integration.type.nbt.NbtModificationResult
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack
import java.lang.Exception
import java.lang.RuntimeException

class NbtApiIntegration : EntityOwner, Integration(
    "NBTAPI",
    "Allows LevelledMobs to modify the NBT data of items and entities.",
    true,
    true,
    IntegrationPriority.NORMAL){
    init {
        enabled = Bukkit.getPluginManager().isPluginEnabled("NBTAPI")
    }

    fun addNbtTag(
        itemStack: ItemStack,
        tag: String
    ): NbtModificationResult{
        var result = NbtModificationResult(itemStack)

        try{
            val nbtItem = NBTItem(itemStack)

            val jsonBefore = nbtItem.toString()
            nbtItem.mergeCompound(NBTContainer(tag))
            val jsonAfter = nbtItem.toString()

            if (jsonBefore == jsonAfter){
                result.withException(RuntimeException(
                    "No NBT modification detected: ensure you are using a correct tag."))
            }
        }
        catch (ex: Exception){
            result.withException(ex)
        }

        return result
    }

    fun addNbtTag(entity: LivingEntity, tag: String): NbtModificationResult{
        val result = NbtModificationResult(entity)

        try{
            val nbtEntity = NBTEntity(entity)

            val jsonBefore = nbtEntity.toString()
            nbtEntity.mergeCompound(NBTContainer(tag))
            val jsonAfter = nbtEntity.toString()

            if (jsonBefore == jsonAfter){
                result.withException(RuntimeException(
                    "No NBT modification detected: ensure you are using a correct tag."))
            }
        }
        catch (ex: Exception){
            result.withException(ex)
        }

        return result
    }

    fun getNbtDump(itemStack: ItemStack): String{
        return NBTItem(itemStack).toString()
    }

    fun getNbtDump(entity: LivingEntity): String{
        return NBTEntity(entity).toString()
    }
}