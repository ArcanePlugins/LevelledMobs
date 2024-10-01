package io.github.arcaneplugins.levelledmobs.managers

import com.google.gson.JsonParser
import de.tr7zw.nbtapi.NBT
import de.tr7zw.nbtapi.iface.ReadWriteNBT
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.customdrops.CustomDropItem
import io.github.arcaneplugins.levelledmobs.debug.DebugType
import io.github.arcaneplugins.levelledmobs.result.NBTApplyResult
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper

/**
 * @author stumper66
 * @since 3.1.0
 */
object NBTManager {
    fun applyNBTDataItem(
        item: CustomDropItem,
        nbtStuff: String
    ): NBTApplyResult {
        val result = NBTApplyResult()
        var itemNbt: ReadWriteNBT? = null
        var jsonBefore: String? = null

        if (LevelledMobs.instance.debugManager.isDebugTypeEnabled(DebugType.NBT_APPLICATION)){
            itemNbt = NBT.createNBTObject()
            NBT.get(item.itemStack, itemNbt::mergeCompound)
            jsonBefore = itemNbt.toString()
        }

        try {
            NBT.modify(item.itemStack){ nbt -> nbt.mergeCompound(NBT.parseNBT(nbtStuff)) }

            if (LevelledMobs.instance.debugManager.isDebugTypeEnabled(DebugType.NBT_APPLICATION)){
                NBT.get(item.itemStack, itemNbt!!::mergeCompound)
                result.itemStack = item.itemStack

                val jsonAfter = itemNbt.toString()
                formulateChangedJson(jsonBefore!!, jsonAfter, result)
            }
        } catch (e: Exception) {
            result.exceptionMessage = e.message
        }

        return result
    }

    fun applyNBTDataMob(
        lmEntity: LivingEntityWrapper,
        nbtStuff: String
    ): NBTApplyResult {
        val result = NBTApplyResult()
        var jsonBefore: String? = null
        var entityNbt: ReadWriteNBT? = null

        try {
            if (LevelledMobs.instance.debugManager.isDebugTypeEnabled(DebugType.NBT_APPLICATION)){
                entityNbt = NBT.createNBTObject()
                NBT.get(lmEntity.livingEntity, entityNbt::mergeCompound)
                jsonBefore = entityNbt.toString()
            }

            NBT.modify(lmEntity.livingEntity){ nbt -> nbt.mergeCompound(NBT.parseNBT(nbtStuff)) }

            if (LevelledMobs.instance.debugManager.isDebugTypeEnabled(DebugType.NBT_APPLICATION)){
                NBT.get(lmEntity.livingEntity, entityNbt!!::mergeCompound)
                val jsonAfter = entityNbt.toString()
                formulateChangedJson(jsonBefore!!, jsonAfter, result)

                if (jsonBefore == jsonAfter)
                    result.exceptionMessage = "No NBT data changed.  Make sure you have used proper NBT strings"
            }
        } catch (e: Exception) {
            result.exceptionMessage = e.message
        }

        return result
    }

    private fun formulateChangedJson(
        jsonBefore: String,
        jsonAfter: String,
        applyResult: NBTApplyResult
    ) {
        val objectsBefore = mutableMapOf<String, String>()
        val objectsAfter = mutableMapOf<String, String>()
        val jsonObjectBefore = JsonParser.parseString(jsonBefore).asJsonObject
        val jsonObjectAfter = JsonParser.parseString(jsonAfter).asJsonObject

        try {
            for (key in jsonObjectBefore.keySet()) {
                objectsBefore[key] = jsonObjectBefore[key].toString()
            }
            for (key in jsonObjectAfter.keySet()) {
                objectsAfter[key] = jsonObjectAfter[key].toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        for (key in jsonObjectAfter.keySet()) {
            val value = jsonObjectAfter[key].toString()

            if (objectsBefore.containsKey(key) && objectsAfter.containsKey(key)
                && objectsBefore[key] != value
            ) {
                if (applyResult.objectsUpdated == null) {
                    applyResult.objectsUpdated = mutableListOf()
                }
                applyResult.objectsUpdated!!.add("$key:$value")
            } else if (!objectsBefore.containsKey(key) && objectsAfter.containsKey(key)) {
                if (applyResult.objectsAdded == null) {
                    applyResult.objectsAdded = mutableListOf()
                }
                applyResult.objectsAdded!!.add("$key:$value")
            } else if (objectsBefore.containsKey(key) && !objectsAfter.containsKey(key)) {
                if (applyResult.objectsRemoved == null) {
                    applyResult.objectsRemoved = mutableListOf()
                }
                applyResult.objectsRemoved!!.add("$key:$value")
            }
        }
    }
}