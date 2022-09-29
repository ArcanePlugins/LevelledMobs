package me.lokka30.levelledmobs.nms;

import me.lokka30.levelledmobs.LevelledMobs;
import net.minecraft.nbt.CompoundTag;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

public class MiscUtils {
    public static @NotNull String getNBTDump(final @NotNull LivingEntity livingEntity, final @NotNull LevelledMobs main){
        final ServerVersionInfo versionInfo = main.nametagQueueManager.nmsHandler.versionInfo;
        if (versionInfo.getMinecraftVersion() <= 1.16){
            return getNBTDump_1_16(livingEntity, versionInfo.getNMSVersion());
        }

        try {
            final Class<?> clazz_CraftLivingEntity;

            clazz_CraftLivingEntity = Class.forName(
                    "org.bukkit.craftbukkit." + versionInfo.getNMSVersion() + ".entity.CraftLivingEntity");

            final Method method_getHandle = clazz_CraftLivingEntity.getDeclaredMethod("getHandle");
            final net.minecraft.world.entity.LivingEntity internalLivingEntity = (net.minecraft.world.entity.LivingEntity) method_getHandle.invoke(
                    livingEntity);

            final Class<?> compoundTagClazz = Class.forName("net.minecraft.nbt.NBTTagCompound");
            final Object compoundTag = compoundTagClazz.getConstructor().newInstance();

            if (versionInfo.getMinecraftVersion() >= 1.18){
                internalLivingEntity.saveWithoutId((CompoundTag) compoundTag);
            }
            else {
                final Class<?> entityClazz = Class.forName("net.minecraft.world.entity.Entity");
                final Method saveWithoutId = entityClazz.getDeclaredMethod("save", compoundTagClazz);
                saveWithoutId.invoke(internalLivingEntity, compoundTag);
            }
            return compoundTag.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    private static @NotNull String getNBTDump_1_16(final @NotNull LivingEntity livingEntity, final String nmsVersion){
        final String compoundTagName = "net.minecraft.server.v1_16_R3.NBTTagCompound";
        final String methodName = "save";

        try {
            final Class<?> clazz_CraftLivingEntity;

            clazz_CraftLivingEntity = Class.forName(
                    "org.bukkit.craftbukkit." + nmsVersion + ".entity.CraftLivingEntity");
            final Method method_getHandle = clazz_CraftLivingEntity.getDeclaredMethod("getHandle");

            // net.minecraft.server.v1_16_R3.EntityLiving
            final Object internalLivingEntity = method_getHandle.invoke(livingEntity);

            final Class<?> compoundTagClazz = Class.forName(compoundTagName);
            final Object compoundTag = compoundTagClazz.getConstructor().newInstance();

            final Class<?> clazz_Entity = Class.forName("net.minecraft.server." + nmsVersion + ".Entity");
            final Method saveWithoutId = clazz_Entity.getDeclaredMethod(methodName, compoundTagClazz);
            saveWithoutId.invoke(internalLivingEntity, compoundTag);

            return compoundTag.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }
}
