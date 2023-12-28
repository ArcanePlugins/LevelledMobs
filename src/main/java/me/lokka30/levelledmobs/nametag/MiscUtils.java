package me.lokka30.levelledmobs.nametag;

import java.lang.reflect.Method;
import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/**
 * Various utilities that use NMS stuff
 *
 * @author stumper66
 * @since 3.11.0
 */
public class MiscUtils {

    public static @NotNull String getNBTDump(
        final @NotNull LivingEntity livingEntity
    ) {
        final LevelledMobs main = LevelledMobs.getInstance();
        final Definitions def = main.getDefinitions();
        final ServerVersionInfo ver = main.getVerInfo();

        if (ver.getMinecraftVersion() < 1.17){
            return getNBTDump_1_16(livingEntity);
        }

        try {
            //final Method method_getHandle = def.clazz_CraftLivingEntity.getDeclaredMethod("getHandle");
            final Object internalLivingEntity = def.method_getHandle.invoke(livingEntity);

            final Class<?> compoundTagClazz =
                Class.forName("net.minecraft.nbt.NBTTagCompound");

            final Object compoundTag = compoundTagClazz.getConstructor().newInstance();
            // net.minecraft.nbt.CompoundTag saveWithoutId(net.minecraft.nbt.CompoundTag) -> f

            final Method saveWithoutId =
                def.clazz_Entity.getDeclaredMethod("f", compoundTagClazz);

            saveWithoutId.invoke(internalLivingEntity, compoundTag);

            return compoundTag.toString();
        } catch (final Exception ex) {
            ex.printStackTrace();
        }

        return "";
    }

        private static @NotNull String getNBTDump_1_16(final @NotNull LivingEntity livingEntity){

        try {
            final Class<?> clazz_CraftLivingEntity;

            clazz_CraftLivingEntity = Class.forName(
                    "org.bukkit.craftbukkit.v1_16_R3.entity.CraftLivingEntity");
            final Method method_getHandle = clazz_CraftLivingEntity.getDeclaredMethod("getHandle");

            // net.minecraft.server.v1_16_R3.EntityLiving
            final Object internalLivingEntity = method_getHandle.invoke(livingEntity);

            final Class<?> compoundTagClazz = Class.forName("net.minecraft.server.v1_16_R3.NBTTagCompound");
            final Object compoundTag = compoundTagClazz.getConstructor().newInstance();

            final Class<?> clazz_Entity = Class.forName("net.minecraft.server.v1_16_R3.Entity");
            final Method saveWithoutId = clazz_Entity.getDeclaredMethod("save", compoundTagClazz);
            saveWithoutId.invoke(internalLivingEntity, compoundTag);

            return compoundTag.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }
}
