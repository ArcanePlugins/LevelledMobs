package io.github.arcaneplugins.levelledmobs.util

import java.io.InputStream
import java.util.Scanner
import java.util.function.Consumer
import io.github.arcaneplugins.levelledmobs.wrappers.SchedulerWrapper
import java.io.FileNotFoundException
import java.net.URI
import org.bukkit.plugin.java.JavaPlugin

/**
 * An adapted version of the Update Checker from the SpigotMC.org Wiki.
 *
 * @author lokka30
 * @see UpdateChecker#getLatestVersion(Consumer)
 * @since 1.9
 */
class UpdateChecker(
    private var plugin: JavaPlugin,
    private var resourceName: String
) {

    /**
     * Credit to the editors of [this](https://www.spigotmc.org/wiki/creating-an-update-checker-that-checks-for-updates) wiki page. (sourced at 15th September 2020)
     *
     * @param consumer what to do once an update checker result is found
     * @since unknown
     */
    fun getLatestVersion(
        consumer: Consumer<String?>
    ) {
        val scheduler = SchedulerWrapper { checkVersion(consumer) }
        scheduler.run()
    }

    private fun checkVersion(
        consumer: Consumer<String?>
    ) {
        val inputStream: InputStream
        try {
            inputStream = URI ("https://hangar.papermc.io/api/v1/projects/$resourceName/latest?channel=Release")
                .toURL().openStream()
        }
        catch (e: FileNotFoundException) {
            Log.war("Error checking for latest version, file not found: ${e.message}")
            return
        }
        catch (e: Exception) {
            Log.war("Error checking for latest version. ${e.message}")
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