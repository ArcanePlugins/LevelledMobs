package me.lokka30.levelledmobs.nms;

import me.lokka30.levelledmobs.util.Utils;
import net.minecraft.nbt.CompoundTag;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

public class MiscUtils {
    public static @NotNull String getNBTDump(final String nmsVersion, final LivingEntity livingEntity){
        final CompoundTag compoundTag = new CompoundTag();
        final Class<?> clazz_CraftLivingEntity;
        try {
            clazz_CraftLivingEntity = Class.forName(
                    "org.bukkit.craftbukkit." + nmsVersion + ".entity.CraftLivingEntity");
        final Method method_getHandle = clazz_CraftLivingEntity.getDeclaredMethod("getHandle");
        final net.minecraft.world.entity.LivingEntity internalLivingEntity = (net.minecraft.world.entity.LivingEntity) method_getHandle.invoke(
                livingEntity);

        internalLivingEntity.saveWithoutId(compoundTag);
        } catch (Exception e) {
            Utils.logger.warning("Error getting nbt_dump: " + e.getMessage());
        }

        return compoundTag.toString();
    }
}
