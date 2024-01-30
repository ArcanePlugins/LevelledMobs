package io.github.arcaneplugins.levelledmobs.util

import java.lang.reflect.InvocationTargetException
import java.util.Optional
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.result.MythicMobsMobInfo
import io.github.arcaneplugins.levelledmobs.nametag.Definitions
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import org.bukkit.Bukkit

/**
 * Used for detecting mobs using Mythic Mobs
 *
 * @author stumper66
 * @since 3.6.0
 */
object MythicMobUtils {
    fun getMythicMobInfo(lmEntity: LivingEntityWrapper): MythicMobsMobInfo? {
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

        val mmMain = Bukkit.getPluginManager().getPlugin("MythicMobs")
        if (mmMain == null || !mmMain.isEnabled) {
            return null
        }

        val def: Definitions = LevelledMobs.instance.definitions

        if (def.field_MM_mobManager == null) {
            Utils.logger.warning("Mythic Mobs is installed but field_MM_mobManager is null")
            return null
        }

        try {
            val mobExecutorObj = def.field_MM_mobManager!![mmMain]

            //     public Optional<ActiveMob> getActiveMob(UUID uuid) {
            //       return ((MobRegistry)this.mobRegistry.get()).getActiveMob(uuid); }

            // Optional<io.lumine.mythic.core.mobs.ActiveMob>
            val activeMobObj = def.method_MM_getActiveMob!!.invoke(
                mobExecutorObj, lmEntity.livingEntity.uniqueId
            ) as Optional<*>

            if (activeMobObj.isEmpty) {
                return null
            }

            val mobTypeObj = def.field_MM_type!![activeMobObj.get()]
            val result = MythicMobsMobInfo()
            result.preventOtherDrops = def.field_MM_preventOtherDrops!![mobTypeObj] as Boolean
            result.preventRandomEquipment = def.field_MM_preventRandomEquipment!![mobTypeObj] as Boolean
            result.internalName = def.field_MM_internalName!![mobTypeObj] as String

            return result
        } catch (e: InvocationTargetException) {
            Utils.logger.warning("Error getting MythicMob info: " + e.message)
        } catch (e: IllegalAccessException) {
            Utils.logger.warning("Error getting MythicMob info: " + e.message)
        }
        return null
    }
}