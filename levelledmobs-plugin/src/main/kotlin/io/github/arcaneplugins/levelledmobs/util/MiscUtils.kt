package io.github.arcaneplugins.levelledmobs.util

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import org.bukkit.entity.LivingEntity

/**
 * Various utilities that use NMS stuff
 *
 * @author stumper66
 * @since 3.11.0
 */
object MiscUtils {
    fun getNBTDump(
        livingEntity: LivingEntity
    ): String {
        val def = LevelledMobs.instance.definitions

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

            return compoundTag.toString()
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
}