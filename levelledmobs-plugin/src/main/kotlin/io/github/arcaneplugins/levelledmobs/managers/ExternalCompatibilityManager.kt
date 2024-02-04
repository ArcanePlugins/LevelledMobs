package io.github.arcaneplugins.levelledmobs.managers

import com.earth2me.essentials.Essentials
import java.io.InvalidObjectException
import java.lang.reflect.InvocationTargetException
import me.clip.placeholderapi.PlaceholderAPI
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.LivingEntityInterface
import io.github.arcaneplugins.levelledmobs.enums.LevellableState
import io.github.arcaneplugins.levelledmobs.misc.VersionInfo
import io.github.arcaneplugins.levelledmobs.result.PlayerHomeCheckResult
import io.github.arcaneplugins.levelledmobs.util.Utils
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType

/**
 * This class handles compatibility with other plugins such as EliteMobs and Citizens
 *
 * @author lokka30, stumper66
 * @since 2.4.0
 */
@Suppress("DEPRECATION")
class ExternalCompatibilityManager {
    private var lmiMeetsVersionRequirement: Boolean? = null
    private var lmiMeetsVersionRequirement2: Boolean? = null

    fun doesLMIMeetVersionRequirement(): Boolean {
        // must be 1.1.0 or newer
        if (lmiMeetsVersionRequirement != null)
            return lmiMeetsVersionRequirement!!

        val lmi = Bukkit.getPluginManager().getPlugin("LM_Items") ?: return false

        try {
            val requiredVersion = VersionInfo("1.1.0")
            val lmiVersion = VersionInfo(lmi.description.version)

            lmiMeetsVersionRequirement = requiredVersion <= lmiVersion
        } catch (e: InvalidObjectException) {
            e.printStackTrace()
            lmiMeetsVersionRequirement = false
        }

        return lmiMeetsVersionRequirement!!
    }

    fun doesLMIMeetVersionRequirement2(): Boolean {
        // must be 1.3.0 or newer
        if (lmiMeetsVersionRequirement2 != null) return lmiMeetsVersionRequirement2!!

        val lmi = Bukkit.getPluginManager().getPlugin("LM_Items") ?: return false

        try {
            val requiredVersion = VersionInfo("1.3.0")
            val lmiVersion = VersionInfo(lmi.description.version)

            lmiMeetsVersionRequirement2 = requiredVersion <= lmiVersion
        } catch (e: InvalidObjectException) {
            e.printStackTrace()
            lmiMeetsVersionRequirement2 = false
        }

        return lmiMeetsVersionRequirement2!!
    }

    enum class ExternalCompatibility {
        NOT_APPLICABLE,

        // DangerousCaves plugin
        DANGEROUS_CAVES,

        // EcoBosses plugin
        ECO_BOSSES,

        // MythicMobs plugin
        MYTHIC_MOBS,

        // EliteMobs plugin
        ELITE_MOBS, ELITE_MOBS_NPCS, ELITE_MOBS_SUPER_MOBS,

        // InfernalMobs plugin
        INFERNAL_MOBS,

        // Citizens plugin
        CITIZENS,

        // Shopkeepers plugin
        SHOPKEEPERS,

        // PlaceholderAPI plugin
        PLACEHOLDER_API,

        SIMPLE_PETS,  //SimplePets plugin

        ELITE_BOSSES,  //EliteBosses plugin

        BLOOD_NIGHT // Blood Night plugin
    }

    companion object{
        private var useNewerEliteMobsKey: Boolean? = null
        private var dangerousCavesMobTypeKey: NamespacedKey? = null
        private var ecoBossesKey: NamespacedKey? = null

        private fun isExternalCompatibilityEnabled(
            externalCompatibility: ExternalCompatibility,
            list: Map<ExternalCompatibility, Boolean?>
        ): Boolean {
            // if not defined default to true
            return (!list.containsKey(externalCompatibility)
                    || list[externalCompatibility] != null && list[externalCompatibility]!!)
        }

        private fun checkIfPluginIsInstalledAndEnabled(pluginName: String): Boolean {
            val plugin = Bukkit.getPluginManager().getPlugin(pluginName)
            return plugin != null && plugin.isEnabled
        }

        fun hasLMItemsInstalled(): Boolean {
            return checkIfPluginIsInstalledAndEnabled("LM_Items")
        }

        fun hasPapiInstalled(): Boolean {
            return checkIfPluginIsInstalledAndEnabled("PlaceholderAPI")
        }

        fun hasNbtApiInstalled(): Boolean {
            return checkIfPluginIsInstalledAndEnabled("NBTAPI")
        }

        fun getPapiPlaceholder(player: Player?, placeholder: String?): String {
            return PlaceholderAPI.setPlaceholders(player, placeholder!!)
        }

        fun hasMythicMobsInstalled(): Boolean {
            return checkIfPluginIsInstalledAndEnabled("MythicMobs")
        }

        fun hasLibsDisguisesInstalled(): Boolean {
            return checkIfPluginIsInstalledAndEnabled("LibsDisguises")
        }

        fun hasWorldGuardInstalled(): Boolean {
            return checkIfPluginIsInstalledAndEnabled("WorldGuard")
        }

        private fun isMobOfSimplePets(lmEntity: LivingEntityWrapper): Boolean {
            val plugin = Bukkit.getPluginManager().getPlugin("SimplePets")
            if (plugin == null || !plugin.isEnabled) {
                return false
            }

            // version 5 uses the API, older versions we'll check for metadata
            if (plugin.description.version.startsWith("4")) {
                for (meta in lmEntity.livingEntity.getMetadata("pet")) {
                    if (meta.asString().isNotEmpty()) {
                        return true
                    }
                }

                return false
            } else {
                return isSimplePets(lmEntity)
            }
        }

        private fun isMobOfEliteBosses(lmEntity: LivingEntityWrapper): Boolean {
            val plugin = Bukkit.getPluginManager().getPlugin("EliteBosses")
            if (plugin == null || !plugin.isEnabled) {
                return false
            }

            for (meta in lmEntity.livingEntity.getMetadata("EliteBosses")) {
                if (meta.asInt() > 0) {
                    return true
                }
            }

            return false
        }

        fun isMobOfBloodNight(lmEntity: LivingEntityWrapper): Boolean {
            val plugin = Bukkit.getPluginManager().getPlugin("BloodNight")
            if (plugin == null || !plugin.isEnabled) {
                return false
            }

            val isBloodNightMob: Boolean = lmEntity.pdc
                .has(NamespacedKey(plugin, "mobtype"), PersistentDataType.STRING)

            if (isBloodNightMob) lmEntity.setMobExternalType(ExternalCompatibility.BLOOD_NIGHT)

            return isBloodNightMob
        }

        fun isMythicMob(lmEntity: LivingEntityWrapper): Boolean {
            val p = Bukkit.getPluginManager().getPlugin("MythicMobs")
            if (p == null || !p.isEnabled) {
                return false
            }

            val mmKey = NamespacedKey(p, "type")
            synchronized(lmEntity.livingEntity.persistentDataContainer) {
                return lmEntity.pdc.has(mmKey, PersistentDataType.STRING)
            }
        }

        fun getMythicMobInternalName(lmEntity: LivingEntityWrapper): String {
            if (!isMythicMob(lmEntity)) {
                return ""
            }

            val p = Bukkit.getPluginManager().getPlugin("MythicMobs")
            if (p == null || !p.isEnabled) {
                return ""
            }

            val mmKey = NamespacedKey(p, "type")
            synchronized(lmEntity.livingEntity.persistentDataContainer) {
                if (lmEntity.pdc.has(mmKey, PersistentDataType.STRING)) {
                    val type = lmEntity.pdc.get(mmKey, PersistentDataType.STRING)
                    return type ?: ""
                }
            }

            return ""
        }

        fun checkAllExternalCompats(lmEntity: LivingEntityWrapper): LevellableState {
            val compatRules: Map<ExternalCompatibility, Boolean?> =
                LevelledMobs.instance.rulesManager.getRuleExternalCompatibility(
                    lmEntity
                )

            if (!isExternalCompatibilityEnabled(ExternalCompatibility.DANGEROUS_CAVES, compatRules)) {
                if (isMobOfDangerousCaves(lmEntity)) {
                    return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_DANGEROUS_CAVES
                }
            }

            if (!isExternalCompatibilityEnabled(ExternalCompatibility.ECO_BOSSES, compatRules)) {
                if (isMobOfEcoBosses(lmEntity)) {
                    return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_ECO_BOSSES
                }
            }

            if (!isExternalCompatibilityEnabled(ExternalCompatibility.MYTHIC_MOBS, compatRules)) {
                if (isMobOfMythicMobs(lmEntity)) {
                    return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_MYTHIC_MOBS
                }
            }

            if (!isExternalCompatibilityEnabled(ExternalCompatibility.ELITE_MOBS, compatRules)) {
                if (isMobOfEliteMobs(lmEntity)) {
                    return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_ELITE_MOBS
                }
            }

            if (!isExternalCompatibilityEnabled(ExternalCompatibility.INFERNAL_MOBS, compatRules)) {
                if (isMobOfInfernalMobs(lmEntity)) {
                    return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_INFERNAL_MOBS
                }
            }

            if (!isExternalCompatibilityEnabled(ExternalCompatibility.CITIZENS, compatRules)) {
                if (isMobOfCitizens(lmEntity)) {
                    return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_CITIZENS
                }
            }

            if (!isExternalCompatibilityEnabled(ExternalCompatibility.SHOPKEEPERS, compatRules)) {
                if (isMobOfShopkeepers(lmEntity)) {
                    return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_SHOPKEEPERS
                }
            }

            if (!isExternalCompatibilityEnabled(ExternalCompatibility.SIMPLE_PETS, compatRules)) {
                if (isMobOfSimplePets(lmEntity)) {
                    return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_SIMPLEPETS
                }
            }

            if (!isExternalCompatibilityEnabled(ExternalCompatibility.ELITE_BOSSES, compatRules)) {
                if (isMobOfEliteBosses(lmEntity)) {
                    return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_ELITE_BOSSES
                }
            }

            if (!isExternalCompatibilityEnabled(ExternalCompatibility.BLOOD_NIGHT, compatRules)) {
                if (isMobOfBloodNight(lmEntity)) {
                    return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_BLOOD_NIGHT
                }
            }

            return LevellableState.ALLOWED
        }

        fun updateAllExternalCompats(lmEntity: LivingEntityWrapper) {
            isMobOfDangerousCaves(lmEntity)
            isMobOfEcoBosses(lmEntity)
            isMobOfMythicMobs(lmEntity)
            isMobOfEliteMobs(lmEntity)
            isMobOfInfernalMobs(lmEntity)
            isMobOfCitizens(lmEntity)
            isMobOfShopkeepers(lmEntity)
            isMobOfSimplePets(lmEntity)
            isMobOfEliteMobs(lmEntity)
            isMobOfBloodNight(lmEntity)
        }

        /**
         * @param lmEntity mob to check
         * @return if Dangerous Caves compatibility enabled and entity is from DangerousCaves
         * @author lokka30, stumper66, imDaniX (author of DC2 - provided part of this method)
         */
        private fun isMobOfDangerousCaves(lmEntity: LivingEntityWrapper): Boolean {
            val plugin = Bukkit.getPluginManager().getPlugin("DangerousCaves") ?: return false

            if (dangerousCavesMobTypeKey == null) {
                dangerousCavesMobTypeKey = NamespacedKey(plugin, "mob-type")
            }

            synchronized(lmEntity.livingEntity.persistentDataContainer) {
                if (!lmEntity.pdc.has(
                        dangerousCavesMobTypeKey!!,
                        PersistentDataType.STRING
                    )
                ) {
                    return false
                }
            }

            lmEntity.setMobExternalType(ExternalCompatibility.DANGEROUS_CAVES)
            return true
        }

        /**
         * @param lmEntity mob to check
         * @return if the compat is enabled and if the mob belongs to EcoBosses
         * @author lokka30, Auxilor (author of EcoBosses - provided part of this method)
         */
        private fun isMobOfEcoBosses(lmEntity: LivingEntityWrapper): Boolean {
            val plugin = Bukkit.getPluginManager().getPlugin("EcoBosses") ?: return false

            if (ecoBossesKey == null) {
                ecoBossesKey = NamespacedKey(plugin, "boss")
            }

            synchronized(lmEntity.livingEntity.persistentDataContainer) {
                if (!lmEntity.pdc
                        .has(ecoBossesKey!!, PersistentDataType.STRING)
                ) {
                    return false
                }
            }

            lmEntity.setMobExternalType(ExternalCompatibility.ECO_BOSSES)
            return true
        }

        /**
         * @param lmEntity mob to check
         * @return if MythicMobs compatibility enabled and entity is from MythicMobs
         */
        private fun isMobOfMythicMobs(lmEntity: LivingEntityWrapper): Boolean {
            if (!hasMythicMobsInstalled()) {
                return false
            }
            if (lmEntity.isMobOfExternalType) {
                return true
            }

            val isExternalType = isMythicMob(lmEntity)
            if (isExternalType) {
                lmEntity.setMobExternalType(ExternalCompatibility.MYTHIC_MOBS)
            }

            return isExternalType
        }

        /**
         * @param lmEntity mob to check
         * @return if EliteMobs compatibility enabled and entity is from EliteMobs
         */
        private fun isMobOfEliteMobs(lmEntity: LivingEntityWrapper): Boolean {
            val p = Bukkit.getPluginManager().getPlugin("EliteMobs")
            if (p != null) {
                // 7.3.12 and newer uses a different namespaced key
                if (useNewerEliteMobsKey == null) {
                    val theDash = p.description.version.indexOf('-')
                    val version = if (theDash > 3) p.description.version.substring(0, theDash)
                    else p.description.version
                    try {
                        val pluginVer = VersionInfo(version)
                        val cutoverVersion = VersionInfo("7.3.12")
                        useNewerEliteMobsKey = pluginVer >= cutoverVersion
                    } catch (e: InvalidObjectException) {
                        Utils.logger.warning(
                            "Got error comparing EliteMob versions: " + e.message
                        )
                        // default to newer version on error
                        useNewerEliteMobsKey = true
                    }
                }

                val checkKey =
                    if (useNewerEliteMobsKey!!) "eliteentity" else "EliteMobsCullable"
                val isEliteMob: Boolean
                synchronized(lmEntity.livingEntity.persistentDataContainer) {
                    isEliteMob = lmEntity.pdc
                        .has(NamespacedKey(p, checkKey), PersistentDataType.STRING)
                }

                if (isEliteMob) {
                    lmEntity.setMobExternalType(ExternalCompatibility.ELITE_MOBS)
                    return true
                }
            }

            return false
        }

        /**
         * @param lmEntity mob to check
         * @return if Citizens compatibility enabled and entity is from Citizens
         */
        private fun isMobOfCitizens(lmEntity: LivingEntityWrapper): Boolean {
            val isExternalType = isMobOfCitizens(lmEntity.livingEntity)

            if (isExternalType) {
                lmEntity.setMobExternalType(ExternalCompatibility.CITIZENS)
            }

            return isExternalType
        }

        /**
         * @param lmEntity mob to check
         * @return if InfernalMobs compatibility enabled and entity is from InfernalMobs
         */
        private fun isMobOfInfernalMobs(lmEntity: LivingEntityWrapper): Boolean {
            val isExternalType = lmEntity.livingEntity.hasMetadata("infernalMetadata")

            if (isExternalType) {
                lmEntity.setMobExternalType(ExternalCompatibility.INFERNAL_MOBS)
            }

            return isExternalType
        }

        fun isMobOfCitizens(livingEntity: LivingEntity): Boolean {
            return livingEntity.hasMetadata("NPC")
        }

        /**
         * @param lmEntity mob to check
         * @return if Shopkeepers compatibility enabled and entity is from Shopkeepers
         */
        private fun isMobOfShopkeepers(lmEntity: LivingEntityWrapper): Boolean {
            val isExternalType = lmEntity.livingEntity.hasMetadata("shopkeeper")

            if (isExternalType) {
                lmEntity.setMobExternalType(ExternalCompatibility.SHOPKEEPERS)
            }

            return isExternalType
        }

        fun getWGRegionsAtLocation(
            lmInterface: LivingEntityInterface
        ): MutableList<String> {
            if (!hasWorldGuardInstalled()) {
                return mutableListOf()
            }

            return WorldGuardIntegration.getWorldGuardRegionsForLocation(lmInterface)
        }

        fun getPlayerHomeLocation(
            player: Player,
            allowBed: Boolean
        ): PlayerHomeCheckResult {
            val plugin = Bukkit.getPluginManager().getPlugin("essentials")
                ?: return PlayerHomeCheckResult(
                    "Unable to get player home, Essentials is not installed", null
                )

            if (allowBed && player.world.environment != World.Environment.NETHER) {
                val bedLocation = player.bedSpawnLocation
                if (bedLocation != null) {
                    return PlayerHomeCheckResult(null, bedLocation, "bed")
                }
            }

            val essentials = plugin as Essentials
            val user = essentials.getUser(player)
                ?: return PlayerHomeCheckResult("Unable to locate player information in essentials")

            if (user.homes == null || user.homes.isEmpty()) {
                return PlayerHomeCheckResult(null, null)
            }

            return PlayerHomeCheckResult(
                null, user.getHome(user.homes[0]),
                user.homes[0]
            )
        }

        private fun isSimplePets(lmEntity: LivingEntityWrapper): Boolean {
            try {
                val clazzPetCore = Class.forName(
                    "simplepets.brainsynder.api.plugin.SimplePets"
                )
                val clazzIPetsPlugin = Class.forName(
                    "simplepets.brainsynder.api.plugin.IPetsPlugin"
                )

                val methodGetPlugin = clazzPetCore.getDeclaredMethod("getPlugin")
                // returns public class PetCore extends JavaPlugin implements IPetsPlugin
                val objIPetsPlugin = methodGetPlugin.invoke(null)

                val methodIsPetEntity = clazzIPetsPlugin.getDeclaredMethod("isPetEntity", Entity::class.java)
                return methodIsPetEntity.invoke(objIPetsPlugin, lmEntity.livingEntity) as Boolean
            } catch (e: NoSuchMethodException) {
                Utils.logger.error("Error checking if " + lmEntity.nameIfBaby + " is a SimplePet")
                e.printStackTrace()
            } catch (e: InvocationTargetException) {
                Utils.logger.error("Error checking if " + lmEntity.nameIfBaby + " is a SimplePet")
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                Utils.logger.error("Error checking if " + lmEntity.nameIfBaby + " is a SimplePet")
                e.printStackTrace()
            } catch (e: ClassNotFoundException) {
                Utils.logger.error("Error checking if " + lmEntity.nameIfBaby + " is a SimplePet")
                e.printStackTrace()
            }

            return false
        }
    }
}