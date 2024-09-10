package io.github.arcaneplugins.levelledmobs.customdrops

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.enums.EquipmentClass
import io.github.arcaneplugins.levelledmobs.misc.NamespacedKeys
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import java.util.WeakHashMap
import org.bukkit.Material
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
        if (isItemAllowedForSerialization(mainHand)){
            val serialized = mainHand!!.serializeAsBytes()
            lmEntity.pdc.set(NamespacedKeys.equipment0, PersistentDataType.BYTE_ARRAY, serialized)
        }
        if (isItemAllowedForSerialization(offhand)){
            val serialized = offhand!!.serializeAsBytes()
            lmEntity.pdc.set(NamespacedKeys.equipment1, PersistentDataType.BYTE_ARRAY, serialized)
        }
        if (isItemAllowedForSerialization(helmet)){
            val serialized = helmet!!.serializeAsBytes()
            lmEntity.pdc.set(NamespacedKeys.equipment2, PersistentDataType.BYTE_ARRAY, serialized)
        }
        if (isItemAllowedForSerialization(chestplate)){
            val serialized = chestplate!!.serializeAsBytes()
            lmEntity.pdc.set(NamespacedKeys.equipment3, PersistentDataType.BYTE_ARRAY, serialized)
        }
        if (isItemAllowedForSerialization(leggings)){
            val serialized = leggings!!.serializeAsBytes()
            lmEntity.pdc.set(NamespacedKeys.equipment4, PersistentDataType.BYTE_ARRAY, serialized)
        }
        if (isItemAllowedForSerialization(boots)){
            val serialized = boots!!.serializeAsBytes()
            lmEntity.pdc.set(NamespacedKeys.equipment5, PersistentDataType.BYTE_ARRAY, serialized)
        }
    }

    private fun isItemAllowedForSerialization(itemStack: ItemStack?): Boolean{
        if (itemStack == null) return false
        // maybe there will be more types later
        if (itemStack.type == Material.AIR) return false

        return true
    }
}