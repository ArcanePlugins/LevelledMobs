package me.lokka30.levelledmobs.nametag;

import java.lang.reflect.Method;
import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/**
 * @author PenalBuffalo (aka stumper66)
 */
public class MiscUtils {

    public static @NotNull String getNBTDump(
        final @NotNull LivingEntity livingEntity
    ) {
//        final ServerVersionInfo versionInfo = main.nametagQueueManager.nmsHandler.versionInfo;
//        if (versionInfo.getMinecraftVersion() <= 1.16){
//            return getNBTDump_1_16(livingEntity, versionInfo.getNMSVersion());
//        }
// TODO - Handle commented out code

        final Definitions def = LevelledMobs.getInstance().getDefinitions();

        try {
            //final Method method_getHandle = def.clazz_CraftLivingEntity.getDeclaredMethod("getHandle");
            // TODO - Handle commented out code
            final Object internalLivingEntity = def.method_getHandle.invoke(livingEntity);

            final Class<?> compoundTagClazz =
                Class.forName("net.minecraft.nbt.NBTTagCompound");

            final Object compoundTag = compoundTagClazz.getConstructor().newInstance();

            final Method saveWithoutId =
                def.clazz_Entity.getDeclaredMethod("e", compoundTagClazz);

            saveWithoutId.invoke(internalLivingEntity, compoundTag);

            return compoundTag.toString();
        } catch (final Exception ex) {
            ex.printStackTrace();
        }

        return "";
    }

//    private static @NotNull String getNBTDump_1_16(final @NotNull LivingEntity livingEntity, final String nmsVersion){
//        final String compoundTagName = "net.minecraft.server.v1_16_R3.NBTTagCompound";
//        final String methodName = "save";
//        final Definitions def = LevelledMobs.getInstance().getDefinitions();
//
//        try {
//            final Class<?> clazz_CraftLivingEntity;
//
//            clazz_CraftLivingEntity = Class.forName(
//                    "org.bukkit.craftbukkit." + nmsVersion + ".entity.CraftLivingEntity");
//            final Method method_getHandle = clazz_CraftLivingEntity.getDeclaredMethod("getHandle");
//
//            // net.minecraft.server.v1_16_R3.EntityLiving
//            final Object internalLivingEntity = method_getHandle.invoke(livingEntity);
//
//            final Class<?> compoundTagClazz = Class.forName(compoundTagName);
//            final Object compoundTag = compoundTagClazz.getConstructor().newInstance();
//
//            //final Class<?> clazz_Entity = Class.forName("net.minecraft.server." + nmsVersion + ".Entity");
//            final Method saveWithoutId = def.clazz_Entity.getDeclaredMethod(methodName, compoundTagClazz);
//            saveWithoutId.invoke(internalLivingEntity, compoundTag);
//
//            return compoundTag.toString();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return "";
//    }
// TODO - Handle commented out code
}
