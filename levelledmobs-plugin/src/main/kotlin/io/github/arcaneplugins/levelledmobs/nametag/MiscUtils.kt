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
            return getNBTDump116(livingEntity)
        }

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

    private fun getNBTDump116(livingEntity: LivingEntity): String {
        try {
            val clazzCraftLivingEntity = Class.forName(
            "org.bukkit.craftbukkit.v1_16_R3.entity.CraftLivingEntity")
            val methodGetHandle = clazzCraftLivingEntity.getDeclaredMethod("getHandle")

            // net.minecraft.server.v1_16_R3.EntityLiving
            val internalLivingEntity = methodGetHandle.invoke(livingEntity)

            val compoundTagClazz = Class.forName("net.minecraft.server.v1_16_R3.NBTTagCompound")
            val compoundTag = compoundTagClazz.getConstructor().newInstance()

            val clazzEntity = Class.forName("net.minecraft.server.v1_16_R3.Entity")
            val saveWithoutId = clazzEntity.getDeclaredMethod("save", compoundTagClazz)
            saveWithoutId.invoke(internalLivingEntity, compoundTag)

            return compoundTag.toString()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        return ""
    }
}