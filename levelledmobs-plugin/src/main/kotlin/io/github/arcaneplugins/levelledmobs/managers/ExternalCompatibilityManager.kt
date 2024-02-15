package io.github.arcaneplugins.levelledmobs.managers

import com.earth2me.essentials.Essentials
import java.io.InvalidObjectException
import me.clip.placeholderapi.PlaceholderAPI
import io.github.arcaneplugins.levelledmobs.LivingEntityInterface
import io.github.arcaneplugins.levelledmobs.enums.ExternalCompatibility
import io.github.arcaneplugins.levelledmobs.enums.LevellableState
import io.github.arcaneplugins.levelledmobs.misc.ExternalPluginDetection
import io.github.arcaneplugins.levelledmobs.misc.VersionInfo
import io.github.arcaneplugins.levelledmobs.result.PlayerHomeCheckResult
import io.github.arcaneplugins.levelledmobs.rules.RulesManager
import io.github.arcaneplugins.levelledmobs.util.Log
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import java.util.TreeMap
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
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
    val externalPluginDefinitions: MutableMap<String, ExternalPluginDetection> = TreeMap(String.CASE_INSENSITIVE_ORDER)
    val externalPluginPlaceholders: MutableMap<String, ExternalPluginDetection> = TreeMap(String.CASE_INSENSITIVE_ORDER)

    init {
        instance = this
    }

    fun parseMobPluginDetection(
        cs: YamlConfiguration?
    ){
        externalPluginDefinitions.clear()
        externalPluginPlaceholders.clear()
        buildBuiltInCompatibilities()
        if (cs == null) return

        for (key in cs.getKeys(false)){
            val csKey = cs.get(key) ?: continue
            if (csKey !is ConfigurationSection) continue

            var keyType = ExternalPluginDetection.KeyTypes.PDC
            var friendlyName = csKey.getString("friendly-name")
            val pluginName = csKey.getString("plugin-name")
            val keyName = csKey.getString("key-name")
            if ("metadata".equals(csKey.getString("key-type"), ignoreCase = true))
                keyType = ExternalPluginDetection.KeyTypes.METADATA
            val requirementStr = csKey.getString("requirement")
            if (friendlyName.isNullOrEmpty())
                friendlyName = pluginName

            if (pluginName.isNullOrEmpty()) continue
            if (keyName.isNullOrEmpty()){
                Log.war("no key-name was supplied for $pluginName")
                continue
            }
            var requirement = ExternalPluginDetection.RequirementTypes.EXISTS
            if (!requirementStr.isNullOrEmpty()){
                try{
                    requirement = ExternalPluginDetection.RequirementTypes.valueOf(requirementStr.uppercase())
                }
                catch (ignored: Exception){
                    Log.war("Invalid value: $requirementStr")
                }
            }

            val mpd = ExternalPluginDetection(
                pluginName,
                friendlyName!!,
                keyName,
                requirement,
                keyType
            )
            mpd.requirementValue = csKey.getString("requirement-value")
            mpd.placeholderName = csKey.getString("placeholder-name")
            this.externalPluginDefinitions[mpd.friendlyName] = mpd
            if (!mpd.placeholderName.isNullOrEmpty())
                this.externalPluginPlaceholders[mpd.placeholderName!!] = mpd
        }
    }

    fun buildBuiltInCompatibilities(){
        val compats = mutableListOf(
            ExternalCompatibility.MYTHIC_MOBS,
            ExternalCompatibility.SIMPLE_PETS,
            ExternalCompatibility.ELITE_BOSSES,
            ExternalCompatibility.ELITE_MOBS,
            ExternalCompatibility.CITIZENS
        )

        for (compat in compats){
            val mobPluginDetection = ExternalPluginDetection(
                compat.toString(),
                compat.toString().replace("_", "-"),
                "(built-in)",
                ExternalPluginDetection.RequirementTypes.EXISTS,
                ExternalPluginDetection.KeyTypes.PDC
            )
            mobPluginDetection.externalCompatibility = compat
            this.externalPluginDefinitions[mobPluginDetection.friendlyName] = mobPluginDetection
        }
    }

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

    companion object{
        @JvmStatic
        lateinit var instance: ExternalCompatibilityManager
            private set

        private var useNewerEliteMobsKey: Boolean? = null

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

        val hasLMItemsInstalled: Boolean
            get() = checkIfPluginIsInstalledAndEnabled("LM_Items")

        val hasPapiInstalled: Boolean
            get() = checkIfPluginIsInstalledAndEnabled("PlaceholderAPI")

        val hasNbtApiInstalled: Boolean
            get() = checkIfPluginIsInstalledAndEnabled("NBTAPI")

        fun getPapiPlaceholder(player: Player?, placeholder: String?): String {
            return PlaceholderAPI.setPlaceholders(player, placeholder!!)
        }

        val hasMythicMobsInstalled: Boolean
            get() = checkIfPluginIsInstalledAndEnabled("MythicMobs")

        val hasLibsDisguisesInstalled: Boolean
            get() = checkIfPluginIsInstalledAndEnabled("LibsDisguises")

        val hasWorldGuardInstalled: Boolean
            get() = checkIfPluginIsInstalledAndEnabled("WorldGuard")

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
            val externalPlugins = RulesManager.instance.getRuleExternalPlugins(
                    lmEntity
            )
            if (externalPlugins == null) return LevellableState.ALLOWED

            for (pluginName in instance.externalPluginDefinitions.keys){
                if (!externalPlugins.isEnabledInList(pluginName, lmEntity)){
                    val mobPlugin = instance.externalPluginDefinitions[pluginName]!!
                    val result = evaluateExternalPluginMob(mobPlugin, lmEntity)
                    if (result != LevellableState.ALLOWED) return result
                }
            }

            return LevellableState.ALLOWED
        }

        private fun evaluateExternalPluginMob(
            mobPlugin: ExternalPluginDetection,
            lmEntity: LivingEntityWrapper
        ): LevellableState{
            if (!mobPlugin.isBuiltIn){
                if (!mobPlugin.isMobOfType(lmEntity))
                    return LevellableState.DENIED_EXTERNAL_PLUGIN

                return LevellableState.ALLOWED
            }

            when (mobPlugin.externalCompatibility!!){
                ExternalCompatibility.MYTHIC_MOBS -> {
                    if (isMobOfMythicMobs(lmEntity)) return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_MYTHIC_MOBS
                }
                ExternalCompatibility.SIMPLE_PETS -> {
                    if (isMobOfSimplePets(lmEntity)) return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_SIMPLEPETS
                }
                ExternalCompatibility.ELITE_BOSSES -> {
                    if (isMobOfEliteBosses(lmEntity)) return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_ELITE_BOSSES
                }
                ExternalCompatibility.ELITE_MOBS -> {
                    if (isMobOfMythicMobs(lmEntity)) return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_ELITE_MOBS
                }
                else -> {
                    return LevellableState.ALLOWED
                }
            }

            return LevellableState.ALLOWED
        }

        fun updateAllExternalCompats(lmEntity: LivingEntityWrapper) {
            for (mobPlugin in instance.externalPluginDefinitions.values){
                if (mobPlugin.isBuiltIn) continue

                mobPlugin.isMobOfType(lmEntity)
            }

            isMobOfMythicMobs(lmEntity)
            isMobOfEliteMobs(lmEntity)
            isMobOfSimplePets(lmEntity)
            isMobOfCitizens(lmEntity)
            isMobOfEliteBosses(lmEntity)
        }

        /**
         * @param lmEntity mob to check
         * @return if MythicMobs compatibility enabled and entity is from MythicMobs
         */
        private fun isMobOfMythicMobs(lmEntity: LivingEntityWrapper): Boolean {
            if (!hasMythicMobsInstalled) {
                return false
            }
            if (lmEntity.isMobOfExternalType) {
                return true
            }

            val isExternalType = isMythicMob(lmEntity)
            if (isExternalType) {
                lmEntity.setMobExternalType("MYTHIC-MOBS")
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
                        Log.war("Got error comparing EliteMob versions: ${e.message}")
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
                    lmEntity.setMobExternalType("ELITE-MOBS")
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
                lmEntity.setMobExternalType(ExternalCompatibility.CITIZENS.toString())
            }

            return isExternalType
        }

        fun isMobOfCitizens(livingEntity: LivingEntity): Boolean {
            return livingEntity.hasMetadata("NPC")
        }

        fun getWGRegionsAtLocation(
            lmInterface: LivingEntityInterface
        ): MutableList<String> {
            if (!hasWorldGuardInstalled) {
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
            } catch (e: Exception) {
                Log.sev("Error checking if ${lmEntity.nameIfBaby} is a SimplePet")
                e.printStackTrace()
            }

            return false
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
    }
}