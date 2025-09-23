package io.github.arcaneplugins.levelledmobs.nametag

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.bukkit.Bukkit

/**
 * Holds various parsed data on the server verion
 * that the server is running
 *
 * @author stumper66
 * @since 3.10.3
 */
class ServerVersionInfo {
    fun load(){
        parseServerVersion()
        parseNMSVersion()

        if (isRunningPaper && "unknown" == nmsVersion) {
            nmsVersion = Bukkit.getServer().minecraftVersion
            useSimpleName = true
        }

        // 1.21.6+ paper servers
        useMojangMappings = isRunningPaper && minorVersion >= 21 && revision >= 6
    }

    var majorVersion = 0
        private set
    var majorVersionEnum: MinecraftMajorVersion? = null
        private set
    var minorVersion = 0
        private set
    var revision = 0
        private set
    var minecraftVersion = 0.0
        private set
    private var isOneTwentyFiveOrNewer = false
    var useOldEnums = false
        private set
    var useMojangMappings = true
        private set
    var useNewHorseJumpAttrib = false
        private set
    var allowStructureConditions = false
        private set
    var useSimpleName = true
        private set

    // preliminary fabric support. not entirely there yet
    private var _isRunningFabric: Boolean? = null
    private var _isRunningSpigot: Boolean? = null
    private var _isRunningPaper: Boolean? = null
    private var _isRunningFolia: Boolean? = null
    var nmsVersion = "unknown"
        private set
    private val versionPattern: Pattern = Pattern.compile(".*\\.(v\\d+_\\d+_R\\d+)(?:.+)?")
    private val versionShortPattern: Pattern = Pattern.compile(".*\\.(v\\d+_\\d+)(?:.+)?")

    private fun parseServerVersion(){
        if (isRunningPaper)
            parsePaperVersion()

        if (!isRunningPaper || !isOneTwentyFiveOrNewer)
            parseBukkitVersion()

        isOneTwentyFiveOrNewer =
            minorVersion == 20 && revision >= 5 || minorVersion >= 21

        allowStructureConditions =
            minorVersion == 20 && revision >= 4 || minorVersion >= 21

        // 1.21.3 changed various enums to interfaces
        useOldEnums = minorVersion < 21 || minorVersion == 21 && revision < 3

        useNewHorseJumpAttrib = minorVersion >= 21 || (minorVersion == 20 && revision >= 6)

        useSimpleName = (isRunningPaper && isOneTwentyFiveOrNewer && !isRunningFolia
                || isRunningFabric)
                && (!isRunningFolia || minorVersion >= 22)
    }

    private fun parsePaperVersion(){
        val minecraftVersion = Bukkit.getServer().minecraftVersion
        // 1.20.4
        val versions = minecraftVersion.split(".")
        for (i in versions.indices) {
            when (i) {
                0 -> this.majorVersion = versions[i].toInt()
                1 -> this.minorVersion = versions[i].toInt()
                2 -> this.revision = versions[i].toInt()
            }
        }

        this.majorVersionEnum =
            MinecraftMajorVersion.valueOf("V${majorVersion}_$minorVersion")
        this.minecraftVersion = "$majorVersion.$minorVersion".toDouble()
    }

    private fun parseBukkitVersion() {
        val bukkitVersion = Bukkit.getBukkitVersion()
        // 1.19.2-R0.1-SNAPSHOT --> 1.19.2
        val firstDash = bukkitVersion.indexOf("-")
        val versions = bukkitVersion.take(firstDash).split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        for (i in versions.indices) {
            when (i) {
                0 -> this.majorVersion = versions[i].toInt()
                1 -> this.minorVersion = versions[i].toInt()
                2 -> this.revision = versions[i].toInt()
            }
        }
    }

    private fun parseNMSVersion() {
        if (isRunningFabric) return
        // example: org.bukkit.craftbukkit.v1_18_R2.CraftServer
        val nmsRegex: Matcher = versionPattern.matcher(
            Bukkit.getServer().javaClass.canonicalName
        )
        val nmsShortRegex: Matcher = versionShortPattern.matcher(
            Bukkit.getServer().javaClass.canonicalName
        )

        if (nmsShortRegex.find()) {
            // example: 1.18
            var versionStr = nmsShortRegex
                .group(1).uppercase(Locale.getDefault())

            try {
                this.majorVersionEnum =
                    MinecraftMajorVersion.valueOf(versionStr.uppercase(Locale.getDefault()))
                versionStr = versionStr.replace("_", ".").replace("V", "")
                this.minecraftVersion = versionStr.toDouble()
            } catch (e: Exception) {
                LevelledMobs.instance.logger.warning(
                    "LevelledMobs: Could not extract the minecraft version from '${Bukkit.getServer().javaClass.canonicalName}'. ${e.message}"
                )
            }
        }

        // example: v1_18_R2
        if (nmsRegex.find()) {
            this.nmsVersion = nmsRegex.group(1)
        } else if (!isRunningPaper) {
            LevelledMobs.instance.logger.warning(
                "LevelledMobs: NMSHandler, Could not match regex for bukkit version: " + Bukkit.getServer()
                    .javaClass.canonicalName
            )
        }
    }

    val isRunningFabric: Boolean
        get() {
            if (this._isRunningFabric == null) {
                try {
                    Class.forName("net.fabricmc.loader.api.FabricLoader")
                    this._isRunningFabric = true
                } catch (_: ClassNotFoundException) {
                    this._isRunningFabric = false
                }
            }

            return this._isRunningFabric!!
        }

    val isRunningSpigot: Boolean
        get() {
            if (this._isRunningSpigot == null) {
                try {
                    Class.forName("net.md_5.bungee.api.ChatColor")
                    this._isRunningSpigot = true
                } catch (_: ClassNotFoundException) {
                    this._isRunningSpigot = false
                }
            }

            return this._isRunningSpigot!!
        }

    val isRunningPaper: Boolean
        get() {
            if (this._isRunningPaper == null) {
                try {
                    Class.forName("com.destroystokyo.paper.ParticleBuilder")
                    this._isRunningPaper = true
                } catch (_: ClassNotFoundException) {
                    this._isRunningPaper = false
                }
            }

            return this._isRunningPaper!!
        }

    val isRunningFolia: Boolean
        get() {
            if (this._isRunningFolia == null) {
                try {
                    Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
                    this._isRunningFolia = true
                } catch (_: ClassNotFoundException) {
                    this._isRunningFolia = false
                }
            }

            return this._isRunningFolia!!
        }

    val isNMSVersionValid: Boolean
        get() { return isOneTwentyFiveOrNewer && isRunningPaper || "unknown" != this.nmsVersion }

    override fun toString(): String {
        return "$majorVersion.$minorVersion.$revision - $nmsVersion"
    }

    enum class MinecraftMajorVersion {
        V1_19, V1_20, V1_21
    }
}