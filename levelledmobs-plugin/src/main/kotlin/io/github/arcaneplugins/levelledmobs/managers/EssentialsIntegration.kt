package io.github.arcaneplugins.levelledmobs.managers

import io.github.arcaneplugins.levelledmobs.result.PlayerHomeCheckResult
import io.github.arcaneplugins.levelledmobs.util.Log
import java.lang.reflect.Method
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

/**
 * Accesses the Essentials API if installed via reflection
 * Coded against version 2.20.1
 *
 * @author stumper66
 * @since 4.3.0
 */
object EssentialsIntegration {
    private var isPresent = false
    private var plugin: Plugin? = null
    private var clazzEssentials: Class<*>? = null
    private var clazzUserData: Class<*>? = null
    private var methodGetUser: Method? = null
    private var methodGetHome: Method? = null
    private var methodGetHomes: Method? = null

    fun load(){
        plugin = Bukkit.getPluginManager().getPlugin("essentials")
        isPresent = plugin != null && plugin!!.isEnabled

        if (!isPresent) return

        Log.inf("Found Essentials, loading integration")
        // public class Essentials extends JavaPlugin implements IEssentials
        clazzEssentials = Class.forName(
            "com.earth2me.essentials.Essentials"
        )

        // public abstract class UserData extends PlayerExtension implements IConf
        clazzUserData = Class.forName(
            "com.earth2me.essentials.UserData"
        )

        // public User getUser(final Player base)
        methodGetUser = clazzEssentials!!.getDeclaredMethod("getUser", Player::class.java)

        // public Location getHome(final String name)
        methodGetHome = clazzUserData!!.getDeclaredMethod("getHome", String::class.java)

        // public List<String> getHomes()
        methodGetHomes = clazzUserData!!.getDeclaredMethod("getHomes")
    }

    @Suppress("UNCHECKED_CAST")
    fun getHomeLocation(player: Player): PlayerHomeCheckResult {
        if (!isPresent) {
            return PlayerHomeCheckResult(
                "Unable to get player home, Essentials is not installed", null
            )
        }

        val objUserData = methodGetUser!!.invoke(plugin, player) ?:
            return PlayerHomeCheckResult("Unable to locate player information in essentials")

        val objList = methodGetHomes!!.invoke(objUserData) ?:
            return PlayerHomeCheckResult(null, null)

        val homesList = objList as MutableList<String>
        if (homesList.isEmpty()) return PlayerHomeCheckResult(null, null)

        val location = methodGetHome!!.invoke(objUserData, homesList[0]) as Location?
        return PlayerHomeCheckResult(null, location, homesList[0])
    }
}