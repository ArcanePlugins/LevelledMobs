package io.github.arcaneplugins.levelledmobs.nametag

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.misc.VersionInfo
import io.github.arcaneplugins.levelledmobs.util.Log
import java.util.regex.Pattern
import org.bukkit.Bukkit
import kotlin.text.split

/**
 * Holds various parsed data on the server verion
 * that the server is running
 *
 * @author stumper66
 * @since 3.10.3
 */
class ServerVersionInfo {
    fun load(){
        parseNMSVersion()
        parseServerVersion()

        if (isRunningPaper && "unknown" == nmsVersion) {
            nmsVersion = Bukkit.getServer().minecraftVersion
            useSimpleName = true
        }

        // 1.21.6+ paper servers
        useMojangMappings = minecraftVersion.isGreaterThanOrEqual("26.1") ||
                isRunningPaper && minecraftVersion.isGreaterThanOrEqual("21.6")
    }

    var majorVersion = 0
        private set
    var minorVersion = 0
        private set
    var revision = 0
        private set
    var minecraftVersion = VersionInfo("0.0")
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
        useOldEnums = minecraftVersion.isLessThanOrEquals("1.21.3")

        useSimpleName = minecraftVersion.isGreaterThanOrEqual("26.1") ||
                (isRunningPaper && isOneTwentyFiveOrNewer && !isRunningFolia
                || isRunningFabric)
                && (!isRunningFolia || minecraftVersion.isGreaterThanOrEqual("1.22.0"))

    }

    private fun parsePaperVersion(){
        var minecraftVersion = Bukkit.getServer().minecraftVersion
        val dash = minecraftVersion.indexOf("-")
        if (dash > 0) // '1.21.9-rc1'
            minecraftVersion = minecraftVersion.take(dash)
        val space = minecraftVersion.indexOf(" ")
        if (space > 0) // '1.21.9 Release Candidate 1'
            minecraftVersion = minecraftVersion.take(space)

        // 1.20.4
        parseVersions(minecraftVersion)
    }

    private fun parseBukkitVersion() {
        // 1.19.2-R0.1-SNAPSHOT --> 1.19.2
        val bukkitVersion = Bukkit.getBukkitVersion().split('-')[0]

        parseVersions(bukkitVersion)
    }

    private fun parseNMSVersion() {
        if (isRunningFabric) return
        if (minecraftVersion.isGreaterThanOrEqual("26.1")) return

        // example: 26.1-R0.1-SNAPSHOT
        Log.infTemp("version: ${Bukkit.getServer().javaClass.canonicalName}")
        val nmsRegex = versionPattern.matcher(
            Bukkit.getServer().javaClass.canonicalName
        )

        // example: v1_18_R2
        if (nmsRegex.find())
            this.nmsVersion = nmsRegex.group(1)
        else if (!isRunningPaper) {
            LevelledMobs.instance.logger.warning(
                "LevelledMobs: NMSHandler, Could not match regex for bukkit version: " + Bukkit.getServer()
                    .javaClass.canonicalName
            )
        }
    }

    private fun parseVersions(version: String){
        val versions = version.split(".")

        for (i in versions.indices) {
            when (i) {
                0 -> this.majorVersion = versions[i].toInt()
                1 -> this.minorVersion = versions[i].toInt()
                2 -> this.revision = versions[i].toInt()
            }
        }

        this.minecraftVersion = VersionInfo(version)
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
}