package io.github.arcaneplugins.levelledmobs.util

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.result.NBTApplyResult
import org.bukkit.entity.LivingEntity

/**
 * Various utilities that use NMS stuff
 *
 * @author stumper66
 * @since 3.11.0
 */
object MiscUtils {

     fun retrieveLoadedChunkRadius(location: org.bukkit.Location, expect: Double): Double {
        val world = location.world
        val centerX:Int = (location.x / 16).toInt()
        val centerZ:Int = (location.z / 16).toInt()
        var maxChunkRadius = 0

        outer@ while (true) {
            val currentRadius = maxChunkRadius + 1
            if (currentRadius * 16 > expect) break
            for (dx in -currentRadius..currentRadius) {
                if (!world.isChunkLoaded(centerX + dx, centerZ - currentRadius))
                    break@outer
                if (!world.isChunkLoaded(centerX + dx, centerZ + currentRadius))
                    break@outer
            }

            for (dz in (-currentRadius + 1) until currentRadius) {
                if (!world.isChunkLoaded(centerX - currentRadius, centerZ + dz))
                    break@outer
                if (!world.isChunkLoaded(centerX + currentRadius, centerZ + dz))
                    break@outer
            }
            maxChunkRadius = currentRadius
        }

        return (maxChunkRadius * 16.0).coerceAtMost(expect)
    }

    fun getNBTDump(
        livingEntity: LivingEntity
    ): String {
        return getNBTDumpObj(livingEntity)?.toString() ?: return ""
    }

    fun getNBTDumpObj(
        livingEntity: LivingEntity
    ): Any? {
        val def = LevelledMobs.instance.definitions
        val ver = LevelledMobs.instance.ver
        val useNewerMethod = (ver.minecraftVersion >= 1.21 && ver.revision >= 6
                || ver.minecraftVersion >= 1.22)

        try {
            val internalLivingEntity = def.methodGetHandle!!.invoke(livingEntity)

            if (useNewerMethod) {
                /*
                    net.minecraft.util.ProblemReporter p = net.minecraft.util.ProblemReporter.DISCARDING;
                    net.minecraft.world.level.storage.TagValueOutput tvo = net.minecraft.world.level.storage.TagValueOutput.createWithoutContext(p);
                    entity.saveWithoutId(tvo);
                    net.minecraft.nbt.CompoundTag tag = tvo.buildResult();
                    return tag.toString()
                */
                val problemReporter = def.fieldDISCARDING!!.get(null)
                val tagValueOutput = def.methodWithoutContext!!.invoke(def.clazzTagValueOutput, problemReporter)
                def.methodSaveWithoutId!!.invoke(internalLivingEntity, tagValueOutput)
                return def.methodBuildResult!!.invoke(tagValueOutput)
            }
            else{
                val compoundTag = def.clazzCompoundTag!!.getConstructor().newInstance()
                def.methodSaveWithoutId!!.invoke(internalLivingEntity, compoundTag)

                return compoundTag
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return null
    }

    @Suppress("UNCHECKED_CAST")
    fun getPDCKeys(
        livingEntity: LivingEntity
    ): MutableMap<String, String> {
        val def = LevelledMobs.instance.definitions
        val results = mutableMapOf<String, String>()
        val nbtCompountTag = getNBTDumpObj(livingEntity) ?: ""

        val value = def.fieldTags!!.get(nbtCompountTag) as MutableMap<String, Any>
        val compoundBukkitValues = value["BukkitValues"]
        val bukkitValues = def.fieldTags!!.get(compoundBukkitValues) as MutableMap<String, Any>

        for (entry in bukkitValues.entries) {
            var byteArraySize = -1
            val classType = def.methodGetType!!.invoke(entry.value)
            val className = (def.methodGetName!!.invoke(classType) as String).lowercase()
            if (entry.value.javaClass.isAssignableFrom(def.classByteArrayTag!!))
                byteArraySize = def.methodByteArrayTagSize!!.invoke(entry.value) as Int

            results[entry.key] = if (byteArraySize >= 0)
                "(byte array size $byteArraySize)"
            else
                "${entry.value}  ($className)"
        }

        return results.toSortedMap()
    }

    fun getNBTDebugMessage(
        results: MutableList<NBTApplyResult>
    ): String {
        val sb = StringBuilder()

        for (result in results) {
            if (result.objectsAdded == null)
                continue

            for (i in 0 until result.objectsAdded!!.size) {
                if (i > 0)
                    sb.append(", ")
                else
                    sb.append("added: ")

                sb.append(result.objectsAdded!![i])
            }
        }

        for (result in results) {
            if (result.objectsUpdated == null)
                continue

            for (i in 0 until result.objectsUpdated!!.size) {
                if (i > 0 || sb.isNotEmpty())
                    sb.append(", ")

                if (i == 0) sb.append("updated: ")

                sb.append(result.objectsUpdated!![i])
            }
        }

        for (result in results) {
            if (result.objectsRemoved == null)
                continue

            for (i in 0 until result.objectsRemoved!!.size) {
                if (i > 0 || sb.isNotEmpty())
                    sb.append(", ")

                if (i == 0)
                    sb.append("removed: ")

                sb.append(result.objectsRemoved!![i])
            }
        }

        return if (sb.isEmpty()) "" else sb.toString()
    }
}