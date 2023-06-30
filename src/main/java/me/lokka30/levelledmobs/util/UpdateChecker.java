package me.lokka30.levelledmobs.util;

import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;
import java.util.function.Consumer;

/**
 * An adapted version of the Update Checker from the SpigotMC.org Wiki.
 *
 * @author lokka30
 * @see UpdateChecker#getLatestVersion(Consumer)
 * @since unknown
 */
public class UpdateChecker {

    private final JavaPlugin plugin;
    private final int resourceId;

    public UpdateChecker(JavaPlugin plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    /**
     * Credit to the editors of <a href="https://www.spigotmc.org/wiki/creating-an-update-checker-that-checks-for-updates">this</a> wiki page. (sourced at 15th September 2020)
     *
     * @param consumer what to do once an update checker result is found
     * @since unknown
     */
    public void getLatestVersion(final Consumer<String> consumer) {
        if (LevelledMobs.getInstance().getVerInfo().getIsRunningFolia()){
            org.bukkit.Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> checkVersion(consumer));
        }
        else{
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> checkVersion(consumer));
        }
    }

    private void checkVersion(final Consumer<String> consumer){
        InputStream inputStream;
        try {
            inputStream = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + this.resourceId).openStream();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Scanner scanner = new Scanner(inputStream);
        if (scanner.hasNext()) {
            consumer.accept(scanner.next());
        }
    }

    /**
     * @return the version string from the plugin's plugin.yml file, i.e., what the user is currently running.
     */
    public String getCurrentVersion() {
        return plugin.getDescription().getVersion();
    }
}