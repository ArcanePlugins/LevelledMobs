package io.github.arcaneplugins.levelledmobs.customdrops

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.enums.EquipmentClass
import io.github.arcaneplugins.levelledmobs.misc.NamespacedKeys
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import java.util.WeakHashMap
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * Holds info on which custom drops are equipped on a mob
 *
 * @author stumper66
 * @since 3.3.3
 */
class EquippedItemsInfo {
    var helmet: ItemStack? = null
    var chestplate: ItemStack? = null
    var leggings: ItemStack? = null
    var boots: ItemStack? = null
    var mainHand: ItemStack? = null
    var offhand: ItemStack? = null

    companion object{
        // only used on spigot servers
        private val customEquippedItems = WeakHashMap<LivingEntity, EquippedItemsInfo>()
        val droppedEquipmentByClass = mutableSetOf<EquipmentClass>()

        fun getEntityEquippedItems(
            lmEntity: LivingEntityWrapper
        ): EquippedItemsInfo? {
            return if (LevelledMobs.instance.ver.isRunningPaper)
                loadFromPDC(lmEntity)
            else
                customEquippedItems[lmEntity.livingEntity]
        }

        private fun loadFromPDC(
            lmEntity: LivingEntityWrapper
        ): EquippedItemsInfo{
            val equipment = EquippedItemsInfo()

            var temp = lmEntity.pdc.get(NamespacedKeys.equipment0, PersistentDataType.BYTE_ARRAY)
            if (temp != null){
                equipment.mainHand = ItemStack.deserializeBytes(temp)
            }

            temp = lmEntity.pdc.get(NamespacedKeys.equipment1, PersistentDataType.BYTE_ARRAY)
            if (temp != null){
                equipment.offhand = ItemStack.deserializeBytes(temp)
            }

            temp = lmEntity.pdc.get(NamespacedKeys.equipment2, PersistentDataType.BYTE_ARRAY)
            if (temp != null){
                equipment.helmet = ItemStack.deserializeBytes(temp)
            }

            temp = lmEntity.pdc.get(NamespacedKeys.equipment3, PersistentDataType.BYTE_ARRAY)
            if (temp != null){
                equipment.chestplate = ItemStack.deserializeBytes(temp)
            }

            temp = lmEntity.pdc.get(NamespacedKeys.equipment4, PersistentDataType.BYTE_ARRAY)
            if (temp != null){
                equipment.leggings = ItemStack.deserializeBytes(temp)
            }

            temp = lmEntity.pdc.get(NamespacedKeys.equipment5, PersistentDataType.BYTE_ARRAY)
            if (temp != null){
                equipment.boots = ItemStack.deserializeBytes(temp)
            }

            return equipment
        }
    }

    fun saveEquipment(
        lmEntity: LivingEntityWrapper
    ){
        if (LevelledMobs.instance.ver.isRunningPaper)
            saveEquipmentToPDC(lmEntity)
        else
            addEntityEquippedItems(lmEntity)
    }

    private fun addEntityEquippedItems(
        lmEntity: LivingEntityWrapper
    ){
        customEquippedItems[lmEntity.livingEntity] = this
    }

    private fun saveEquipmentToPDC(
        lmEntity: LivingEntityWrapper
    ){
        if (mainHand != null){
            val serialized = mainHand!!.serializeAsBytes()
            lmEntity.pdc.set(NamespacedKeys.equipment0, PersistentDataType.BYTE_ARRAY, serialized)
        }
        if (offhand != null){
            val serialized = offhand!!.serializeAsBytes()
            lmEntity.pdc.set(NamespacedKeys.equipment1, PersistentDataType.BYTE_ARRAY, serialized)
        }
        if (helmet != null){
            val serialized = helmet!!.serializeAsBytes()
            lmEntity.pdc.set(NamespacedKeys.equipment2, PersistentDataType.BYTE_ARRAY, serialized)
        }
        if (chestplate != null){
            val serialized = chestplate!!.serializeAsBytes()
            lmEntity.pdc.set(NamespacedKeys.equipment3, PersistentDataType.BYTE_ARRAY, serialized)
        }
        if (leggings != null){
            val serialized = leggings!!.serializeAsBytes()
            lmEntity.pdc.set(NamespacedKeys.equipment4, PersistentDataType.BYTE_ARRAY, serialized)
        }
        if (boots != null){
            val serialized = boots!!.serializeAsBytes()
            lmEntity.pdc.set(NamespacedKeys.equipment5, PersistentDataType.BYTE_ARRAY, serialized)
        }
    }
}