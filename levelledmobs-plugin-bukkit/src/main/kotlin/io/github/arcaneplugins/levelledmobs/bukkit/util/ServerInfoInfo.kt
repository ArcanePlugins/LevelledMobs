package io.github.arcaneplugins.levelledmobs.bukkit.util

import org.bukkit.Bukkit
import java.util.regex.Matcher
import java.util.regex.Pattern

object ServerInfoInfo {
    /**
     * The first digit of the version (1.20.3 would return 1)
     */
    var majorVersion = 0
        private set

    /**
     * An enum of the Minecraft version
     */
    var majorVersionEnum: MinecraftMajorVersion = MinecraftMajorVersion.V1_20
        private set

    /**
     * The second digit of the version (1.20.3 would return 20)
     */
    var minorVersion = 0
        private set

    /**
     * The last digit of the version (1.20.3 would return 3)
     */
    var revision = 0
        private set

    /**
     * A double representing the last 2 digits of the version (1.20.3 would return 20.3)
     */
    var minecraftVersion = 0.0
        private set
    private var _isRunningSpigot: Boolean? = null
    private var _isRunningPaper: Boolean? = null
    private var _isRunningFolia: Boolean? = null
    var nmsVersion = "unknown"
        private set
    private val versionPattern = Pattern.compile(".*\\.(v\\d+_\\d+_R\\d+)(?:.+)?")
    private val versionShortPattern = Pattern.compile(".*\\.(v\\d+_\\d+)(?:.+)?")

    init {
        parseBukkitVersion()
        parseNMSVersion()
    }

    private fun parseBukkitVersion(){
        val bukkitVersion = Bukkit.getBukkitVersion()
        // 1.19.2-R0.1-SNAPSHOT --> 1.19.2
        // 1.19.2-R0.1-SNAPSHOT --> 1.19.2
        val firstDash = bukkitVersion.indexOf("-")
        val versions = bukkitVersion.substring(0, firstDash).split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        for (i in versions.indices) {
            when (i) {
                0 -> majorVersion = versions[i].toInt()
                1 -> minorVersion = versions[i].toInt()
                2 -> revision = versions[i].toInt()
            }
        }
    }

    private fun parseNMSVersion(){
        // example: org.bukkit.craftbukkit.v1_18_R2.CraftServer
        val nmsRegex: Matcher = versionPattern.matcher(
            Bukkit.getServer().javaClass.getCanonicalName()
        )
        val nmsShortRegex: Matcher = versionShortPattern.matcher(
            Bukkit.getServer().javaClass.getCanonicalName()
        )

        if (nmsShortRegex.find()) {
            // example: 1.18
            var versionStr = nmsShortRegex
                .group(1).uppercase()

            try {
                majorVersionEnum = MinecraftMajorVersion.valueOf(versionStr.uppercase())
                versionStr = versionStr.replace("_", ".").replace("V", "")
                minecraftVersion = versionStr.toDouble()
            } catch (e: Exception) {
                Log.war(
                "Could not extract the minecraft version from " +
                        "'${Bukkit.getServer().javaClass.getCanonicalName()}'. ${e.message}",
                )
            }
        }


        // example: v1_18_R2
        if (nmsRegex.find()) {
            this.nmsVersion = nmsRegex.group(1)
        } else {
            Log.war(
                "NMSHandler: Could not match regex for bukkit version: " + Bukkit.getServer()
                    .javaClass.canonicalName
            )
        }
    }

    val isRunningSpigot: Boolean
        get() {
            if (_isRunningSpigot == null) {
                _isRunningSpigot = try {
                    Class.forName("net.md_5.bungee.api.ChatColor")
                    true
                } catch (ignored: ClassNotFoundException) {
                    false
                }
            }

            return _isRunningSpigot!!
        }
    val isRunningPaper: Boolean
        get() {
            if (_isRunningPaper == null) {
                _isRunningPaper = try {
                    Class.forName("com.destroystokyo.paper.ParticleBuilder")
                    true
                } catch (ignored: ClassNotFoundException) {
                    false
                }
            }

            return _isRunningPaper!!
        }
    val isRunningFolia: Boolean
        get() {
            if (_isRunningFolia == null) {
                _isRunningFolia = try {
                    Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
                    true
                } catch (e: ClassNotFoundException) {
                    false
                }
            }
            return _isRunningFolia!!
        }

    enum class MinecraftMajorVersion {
        V1_16,
        V1_17,
        V1_18,
        V1_19,
        V1_20,
        V1_21
    }

    override fun toString() : String {
        return "$majorVersion.$minorVersion.$revision - $nmsVersion"
    }
}