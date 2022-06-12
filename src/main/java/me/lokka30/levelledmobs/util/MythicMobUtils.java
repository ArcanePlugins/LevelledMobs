package me.lokka30.levelledmobs.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.MythicMobsMobInfo;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MythicMobUtils {

    @Nullable
    public static MythicMobsMobInfo getMythicMobInfo(final @NotNull LivingEntityWrapper lmEntity) {
        // the below code was written against MythicMobs v5.0.4-f1007ca3

        /*
           1. The main class of the plugin, MythicBukkit - get field: private MobExecutor mobManager;
           2. from MobExecutor class, call this function while passing the UUID from the mob: public Optional<ActiveMob> getActiveMob(UUID uuid);
           3. in the ActiveMob class, get this field: private transient MythicMob type;
           4. MythicMob is an interface, the class implementation is MobType
           5. the MobType class has the properties we want including:
              private Boolean preventRandomEquipment = Boolean.valueOf(false);
              private Boolean preventOtherDrops = Boolean.valueOf(false);
              private String internalName;
        */

        // io.lumine.mythic.bukkit.MythicBukkit
        final Plugin mmMain = Bukkit.getPluginManager().getPlugin("MythicMobs");
        if (mmMain == null) {
            return null;
        }

        try {
            final Field field_mobManager = mmMain.getClass().getDeclaredField("mobManager");
            field_mobManager.setAccessible(true);

            final Object mobExecutorObj = field_mobManager.get(mmMain);
            // io.lumine.mythic.core.mobs.MobExecutor
            final Class<?> clazz_MobExecutor = mobExecutorObj.getClass();

            //     public Optional<ActiveMob> getActiveMob(UUID uuid) {
            //       return ((MobRegistry)this.mobRegistry.get()).getActiveMob(uuid); }
            final Method method_getActiveMob = clazz_MobExecutor.getMethod("getActiveMob",
                UUID.class);

            // Optional<io.lumine.mythic.core.mobs.ActiveMob>
            final Optional<?> activeMobObj = (Optional<?>) method_getActiveMob.invoke(
                mobExecutorObj, lmEntity.getLivingEntity().getUniqueId());

            if (activeMobObj.isEmpty()) {
                return null;
            }

            // io.lumine.mythic.core.mobs.ActiveMob
            final Class<?> clazz_ActiveMob = activeMobObj.get().getClass();

            // io.lumine.mythic.api.mobs.MythicMob (interface)
            final Field field_type = clazz_ActiveMob.getDeclaredField("type");
            field_type.setAccessible(true);
            final Object mobTypeObj = field_type.get(activeMobObj.get());
            // io.lumine.mythic.core.mobs.MobType
            final Class<?> clazz_MobType = mobTypeObj.getClass();

            final Field field_preventOtherDrops = clazz_MobType.getDeclaredField(
                "preventOtherDrops"); // boolean
            field_preventOtherDrops.setAccessible(true);
            final Field field_preventRandomEquipment = clazz_MobType.getDeclaredField(
                "preventRandomEquipment"); // boolean
            field_preventRandomEquipment.setAccessible(true);
            final Field field_internalName = clazz_MobType.getDeclaredField(
                "internalName"); // string
            field_internalName.setAccessible(true);

            final MythicMobsMobInfo result = new MythicMobsMobInfo();
            result.preventOtherDrops = (boolean) field_preventOtherDrops.get(mobTypeObj);
            result.preventRandomEquipment = (boolean) field_preventRandomEquipment.get(mobTypeObj);
            result.internalName = (String) field_internalName.get(mobTypeObj);

            return result;
        } catch (NoSuchFieldException | InvocationTargetException | IllegalAccessException |
                 NoSuchMethodException e) {
            Utils.logger.warning("Error getting MythicMob info: " + e.getMessage());
        }
        return null;
    }
}
