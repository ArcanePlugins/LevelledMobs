package me.lokka30.levelledmobs.util;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.nametag.Definitions;
import me.lokka30.levelledmobs.wrappers.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.MythicMobsMobInfo;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Used for detecting mobs using Mythic Mobs
 *
 * @author stumper66
 * @since 3.6.0
 */
public class MythicMobUtils {
    public @Nullable static MythicMobsMobInfo getMythicMobInfo(final @NotNull LivingEntityWrapper lmEntity) {
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

        final Definitions def = LevelledMobs.getInstance().getDefinitions();

        try {
            final Object mobExecutorObj = def.field_MM_mobManager.get(mmMain);

            //     public Optional<ActiveMob> getActiveMob(UUID uuid) {
            //       return ((MobRegistry)this.mobRegistry.get()).getActiveMob(uuid); }

            // Optional<io.lumine.mythic.core.mobs.ActiveMob>
            final Optional<?> activeMobObj = (Optional<?>) def.method_MM_getActiveMob.invoke(
                    mobExecutorObj, lmEntity.getLivingEntity().getUniqueId());

            if (activeMobObj.isEmpty()) {
                return null;
            }

            final Object mobTypeObj = def.field_MM_type.get(activeMobObj.get());
            final MythicMobsMobInfo result = new MythicMobsMobInfo();
            result.preventOtherDrops = (boolean) def.field_MM_preventOtherDrops.get(mobTypeObj);
            result.preventRandomEquipment = (boolean) def.field_MM_preventRandomEquipment.get(mobTypeObj);
            result.internalName = (String) def.field_MM_internalName.get(mobTypeObj);

            return result;
        } catch (InvocationTargetException | IllegalAccessException e) {
            Utils.logger.warning("Error getting MythicMob info: " + e.getMessage());
        }
        return null;
    }
}
