package io.github.arcaneplugins.levelledmobs.plugin.bukkit.nametag

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.enums.MinecraftMajorVersion
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.misc.Log
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.bukkit.Bukkit

class ServerVersionInfo {
    var majorVersion: Int = 0
        private set
    var majorVersionEnum: MinecraftMajorVersion = MinecraftMajorVersion.UNKNOWN
        private set
    var minorVersion: Int = 0
        private set
    var revision: Int = 0
        private set
    var minecraftVersion = 0.0
        private set
    private var _isRunningSpigot: Boolean? = null
    private var _isRunningPaper: Boolean? = null
    private var _isRunningFolia: Boolean? = null
    var nmsVersion = "unknown"
        private set
    private val versionPattern: Pattern = Pattern.compile(".*\\.(v\\d+_\\d+_R\\d+)(?:.+)?")
    private val versionShortPattern: Pattern = Pattern.compile(".*\\.(v\\d+_\\d+)(?:.+)?")

    private fun parseBukkitVersion(){
        val bukkitVersion: String = Bukkit.getBukkitVersion()
        val firstDash = bukkitVersion.indexOf('-')
        val versions = bukkitVersion.substring(0, firstDash).split("\\")

        for (i in 0..versions.size){
            when (i){
                0 -> this.majorVersion = versions[i].toInt()
                1 -> this.minorVersion = versions[i].toInt()
                2 -> this.revision = versions[i].toInt()
            }
        }
    }

    fun load(){
        parseBukkitVersion()
        parseNMSVersion()
    }

    private fun parseNMSVersion(){
        // example: org.bukkit.craftbukkit.v1_18_R2.CraftServer
        val nmsRegex: Matcher = versionPattern.matcher(Bukkit.getServer().javaClass.canonicalName)
        val nmsShortRegex: Matcher = versionShortPattern.matcher(Bukkit.getServer().javaClass.canonicalName)

        if (nmsShortRegex.find()){
            // example: 1.18
            var versionStr = nmsShortRegex.group(1).uppercase()

            try{
                this.majorVersionEnum = MinecraftMajorVersion.valueOf(versionStr.uppercase())
                versionStr = versionStr.replace("_", ".").replace("V", "")
                this.minecraftVersion = versionStr.toDouble()
            }
            catch (e: Exception){
                Log.warning(String.format(
                    "Could not extract the minecraft version from '%s'. %s",
                    Bukkit.getServer().javaClass.getCanonicalName(), e.message))
            }
        }

        // example: v1_18_R2
        if (nmsRegex.find()) {
            this.nmsVersion = nmsRegex.group(1)
        }
        else{
            Log.warning(
                "NMSHandler: Could not match regex for bukkit version: " + Bukkit.getServer()
                    .javaClass.getCanonicalName())
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

            return _isRunningSpigot == true
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

            return _isRunningPaper == true
        }

    val isRunningFolia: Boolean
        get() {
            if (_isRunningFolia == null){
                _isRunningFolia = Bukkit.getBukkitVersion().lowercase().contains("folia")
            }

            return _isRunningFolia == true
        }

    val isNMSVersionValid: Boolean
        get() = "unknown" != this.nmsVersion
}