package io.github.arcaneplugins.levelledmobs.util

import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.Scanner
import java.util.function.Consumer
import io.github.arcaneplugins.levelledmobs.wrappers.SchedulerWrapper
import org.bukkit.plugin.java.JavaPlugin

/**
 * An adapted version of the Update Checker from the SpigotMC.org Wiki.
 *
 * @author lokka30
 * @see UpdateChecker#getLatestVersion(Consumer)
 * @since unknown
 */
class UpdateChecker(
    private var plugin: JavaPlugin,
    private var resourceId: Int
) {

    /**
     * Credit to the editors of [this](https://www.spigotmc.org/wiki/creating-an-update-checker-that-checks-for-updates) wiki page. (sourced at 15th September 2020)
     *
     * @param consumer what to do once an update checker result is found
     * @since unknown
     */
    fun getLatestVersion(
        consumer: Consumer<String>
    ) {
        val scheduler = SchedulerWrapper { checkVersion(consumer) }
        scheduler.run()
    }

    private fun checkVersion(
        consumer: Consumer<String>
    ) {
        val inputStream: InputStream
        try {
            inputStream = URL("https://api.spigotmc.org/legacy/update.php?resource=" + this.resourceId).openStream()
        } catch (e: IOException) {
            e.printStackTrace()
            return
        }

        val scanner = Scanner(inputStream)
        if (scanner.hasNext()) {
            consumer.accept(scanner.next())
        }
    }

    /**
     * @return the version string from the plugin's plugin.yml file, i.e., what the user is currently running.
     */
    @Suppress("DEPRECATION")
    val currentVersion: String
        get() = plugin.description.version
}