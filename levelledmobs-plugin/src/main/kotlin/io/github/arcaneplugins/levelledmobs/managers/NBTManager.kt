package io.github.arcaneplugins.levelledmobs.managers

import com.google.gson.JsonParser
import de.tr7zw.nbtapi.NBTContainer
import de.tr7zw.nbtapi.NBTEntity
import de.tr7zw.nbtapi.NBTItem
import java.util.TreeMap
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.customdrops.CustomDropItem
import io.github.arcaneplugins.levelledmobs.misc.DebugType
import io.github.arcaneplugins.levelledmobs.result.NBTApplyResult
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper

/**
 * @author stumper66
 * @since 3.1.0
 */
object NBTManager {
    fun applyNBT_Data_Item(
        item: CustomDropItem,
        nbtStuff: String
    ): NBTApplyResult {
        val result = NBTApplyResult()
        val nbtent = NBTItem(item.itemStack)

        try {
            nbtent.mergeCompound(NBTContainer(nbtStuff))
            result.itemStack = nbtent.item
        } catch (e: Exception) {
            result.exceptionMessage = e.message
        }

        return result
    }

    fun applyNBT_Data_Mob(
        lmEntity: LivingEntityWrapper,
        nbtStuff: String
    ): NBTApplyResult {
        val result = NBTApplyResult()

        try {
            val nbtent = NBTEntity(lmEntity.livingEntity)
            val jsonBefore = nbtent.toString()
            nbtent.mergeCompound(NBTContainer(nbtStuff))
            val jsonAfter = nbtent.toString()

            if (LevelledMobs.instance.debugManager.isDebugTypeEnabled(
                    DebugType.NBT_APPLICATION
                )
            ) {
                showChangedJson(jsonBefore, jsonAfter, result)
            }

            if (LevelledMobs.instance.debugManager.isDebugTypeEnabled(DebugType.NBT_APPLICATION) && jsonBefore == jsonAfter) {
                result.exceptionMessage = "No NBT data changed.  Make sure you have used proper NBT strings"
            }
        } catch (e: Exception) {
            result.exceptionMessage = e.message
        }

        return result
    }

    private fun showChangedJson(
        jsonBefore: String, jsonAfter: String,
        applyResult: NBTApplyResult
    ) {
        val objectsBefore: MutableMap<String, String> = TreeMap()
        val objectsAfter: MutableMap<String, String> = TreeMap()
        val jsonObjectBefore = JsonParser.parseString(jsonBefore).asJsonObject
        val jsonObjectAfter = JsonParser.parseString(jsonAfter).asJsonObject

        try {
            for (key in jsonObjectBefore.keySet()) {
                objectsBefore[key] = jsonObjectBefore[key].toString()
            }
            for (key in jsonObjectAfter.keySet()) {
                objectsAfter[key] = jsonObjectAfter[key].toString()
            }
        } catch (e: java.lang.Exception) {
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