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
        val def = LevelledMobs.instance.definitions
        val ver = LevelledMobs.instance.ver
        val useNewMethod = (ver.minecraftVersion >= 1.21 && ver.minorVersion >= 6
                || ver.minecraftVersion >= 1.22)

        try {
            val internalLivingEntity = def.methodGetHandle!!.invoke(livingEntity)

            if (useNewMethod) {
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
                return def.methodBuildResult!!.invoke(tagValueOutput).toString()
            }
            else{
                val compoundTag = def.clazzCompoundTag!!.getConstructor().newInstance()
                def.methodSaveWithoutId!!.invoke(internalLivingEntity, compoundTag)

                return compoundTag.toString()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return ""
    }

    @Suppress("UNCHECKED_CAST")
    fun getPDCKeys(
        livingEntity: LivingEntity
    ): MutableMap<String, String> {
        val def = LevelledMobs.instance.definitions
        val results = mutableMapOf<String, String>()

        try {
            //final Method method_getHandle = def.clazz_CraftLivingEntity.getDeclaredMethod("getHandle");
            val internalLivingEntity = def.methodGetHandle!!.invoke(livingEntity)

            val compoundTagClazz =
                Class.forName("net.minecraft.nbt.NBTTagCompound")

            val compoundTag = compoundTagClazz.getConstructor().newInstance()

            // net.minecraft.nbt.CompoundTag saveWithoutId(net.minecraft.nbt.CompoundTag) -> f
            val saveWithoutId =
                def.clazzEntity!!.getDeclaredMethod("f", compoundTagClazz)

            saveWithoutId.invoke(internalLivingEntity, compoundTag)

            // private final Map<String, NBTBase> tags;
            // # {"fileName":"CompoundTag.java","id":"sourceFile"}
            // net.minecraft.nbt.CompoundTag ->
            //    java.util.Map tags ->

            val ver = LevelledMobs.instance.ver
            var methodName = if (ver.majorVersion >= 21) "tags" else "x"

            val tagsField = compoundTagClazz.getDeclaredField(methodName)
            tagsField.trySetAccessible()
            val tagsMap = tagsField.get(compoundTag) as MutableMap<String, Any>

            // NBTTagCompound.java
            val bukkitValues = tagsMap["BukkitValues"] ?: return results

            // private final Map<String, NBTBase> tags; (again)
            val bukkitValuesMap = tagsField.get(bukkitValues) as MutableMap<String, Any>

            for (nbtBase in bukkitValuesMap.entries){
                // @Override
                // public NBTTagType<NBTTagCompound> getType()
                // net.minecraft.nbt.CompoundTag ->
                //     net.minecraft.nbt.TagType getType() ->
                methodName = if (ver.majorVersion >= 21) "getType" else "c"
                val getTypeMethod = nbtBase.value::javaClass.get().getMethod(methodName)
                val type = getTypeMethod.invoke(nbtBase.value)
                // @Override
                // public String getName()
                // net.minecraft.nbt.TagType ->
                //    java.lang.String getName() ->
                methodName = if (ver.majorVersion >= 21) "getName" else "a"
                val getNameMethod =  type::class.java.getDeclaredMethod(methodName)
                // all classes are public but for some reason acts like it is private
                getNameMethod.trySetAccessible()

                val valueType = getNameMethod.invoke(type).toString().lowercase()
                if ("byte[]" == valueType) {
                    // NBTTagByteArray.java: public int size()
                    val methodSize = nbtBase.value.javaClass.getDeclaredMethod("size")
                    val byteSize = (methodSize.invoke(nbtBase.value))
                    results[nbtBase.key] = "type: &b$valueType&r, size: &b$byteSize&r"
                    //Log.inf("key: '&b${nbtBase.key}&r', type: '&b$valueType&r', size: &b$byteSize&r")
                }
                else
                    results[nbtBase.key] = "type: &b$valueType&r, value: &b${nbtBase.value}&r"
                    //Log.inf("key: '&b${nbtBase.key}&r', type: '&b$valueType&r', value: '&b${nbtBase.value}&r'")
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return results.toSortedMap()
    }

    fun getNBTDebugMessage(
        results: MutableList<NBTApplyResult>
    ): String {
        val sb = StringBuilder()

        for (result in results) {
            if (result.objectsAdded == null) {
                continue
            }

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