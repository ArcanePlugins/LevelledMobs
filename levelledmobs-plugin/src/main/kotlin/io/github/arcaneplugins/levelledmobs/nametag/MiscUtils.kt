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
}