package io.github.arcaneplugins.levelledmobs.nametag

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
        val ver: ServerVersionInfo = LevelledMobs.instance.ver

        if (ver.minecraftVersion < 1.17) {
            return getNBTDump_1_16(livingEntity)
        }

        try {
            //final Method method_getHandle = def.clazz_CraftLivingEntity.getDeclaredMethod("getHandle");
            val internalLivingEntity = def.method_getHandle!!.invoke(livingEntity)

            val compoundTagClazz =
                Class.forName("net.minecraft.nbt.NBTTagCompound")

            val compoundTag = compoundTagClazz.getConstructor().newInstance()

            // net.minecraft.nbt.CompoundTag saveWithoutId(net.minecraft.nbt.CompoundTag) -> f
            val saveWithoutId =
                def.clazz_Entity!!.getDeclaredMethod("f", compoundTagClazz)

            saveWithoutId.invoke(internalLivingEntity, compoundTag)

            return compoundTag.toString()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return ""
    }

    private fun getNBTDump_1_16(livingEntity: LivingEntity): String {
        try {
            val clazz_CraftLivingEntity = Class.forName(
            "org.bukkit.craftbukkit.v1_16_R3.entity.CraftLivingEntity")
            val method_getHandle = clazz_CraftLivingEntity.getDeclaredMethod("getHandle")

            // net.minecraft.server.v1_16_R3.EntityLiving
            val internalLivingEntity = method_getHandle.invoke(livingEntity)

            val compoundTagClazz = Class.forName("net.minecraft.server.v1_16_R3.NBTTagCompound")
            val compoundTag = compoundTagClazz.getConstructor().newInstance()

            val clazz_Entity = Class.forName("net.minecraft.server.v1_16_R3.Entity")
            val saveWithoutId = clazz_Entity.getDeclaredMethod("save", compoundTagClazz)
            saveWithoutId.invoke(internalLivingEntity, compoundTag)

            return compoundTag.toString()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        return ""
    }
}