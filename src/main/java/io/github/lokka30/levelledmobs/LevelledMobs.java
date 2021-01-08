package io.github.lokka30.levelledmobs;

import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import io.github.lokka30.levelledmobs.commands.LevelledMobsCommand;
import io.github.lokka30.levelledmobs.listeners.*;
import io.github.lokka30.levelledmobs.utils.FileLoader;
import io.github.lokka30.levelledmobs.utils.LevelManager;
import io.github.lokka30.levelledmobs.utils.Utils;
import io.github.lokka30.levelledmobs.utils.WorldGuardManager;
import me.lokka30.microlib.QuickTimer;
import me.lokka30.microlib.UpdateChecker;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class LevelledMobs extends JavaPlugin {

    // Configuration files.
    public YamlConfiguration settingsCfg;
    public YamlConfiguration messagesCfg;

    // TODO Move these.
    // WorldGuard
    public boolean hasWorldGuard; //if worldguard is on the server
    public static StringFlag minlevelflag, maxlevelflag; //The WorldGuard flags of the min and max mob levels.
    public static StateFlag allowlevelflag; //The WorldGuard flag if mobs can be levelled.
    public WorldGuardManager worldGuardManager; //The WorldGuardManager class brings WorldGuard support to LM.

    // Level Manager
    public LevelManager levelManager; //The LevelManager class which holds a bunch of common methods

    // TODO Move these.
    public final static int maxCreeperBlastRadius = 100; // prevent creepers from blowing up the world!
    public Pattern slimeRegex;
    public CreatureSpawnListener creatureSpawnListener;

    //When the plugin starts loading (when Bukkit announces that it's loading, but it hasn't started the enable process).
    //onLoad should only be used for setting other classes like LevelManager.
    public void onLoad() {
        levelManager = new LevelManager(this);
        // [Level 10 | Slime]
        // [&7Level 10&8 | &fSlime&8]
        // "Level.*?(\\d{1,2})"
        slimeRegex = Pattern.compile("Level.*?(\\d{1,2})", Pattern.CASE_INSENSITIVE);
        checkWorldGuard(); //WorldGuard check is onLoad as it is required to be here instead of onEnable (as stated in its documentation).
    }

    public void onEnable() {
        Utils.logger.info("&f~ Initiating start-up procedure ~");
        QuickTimer timer = new QuickTimer();
        timer.start();

        checkCompatibility(); //Is the server running the latest version? Dependencies required?
        loadFiles();
        registerListeners();
        registerCommands();

        Utils.logger.info("&fStart-up: &7Running misc procedures...");
        setupMetrics();
        checkUpdates();

        Utils.logger.info("&f~ Start-up complete, took &b" + timer.getTimer() + "ms&f ~");
    }

    //Checks if the server version is supported
    private void checkCompatibility() {
        Utils.logger.info("&fCompatibility Checker: &7Checking compatibility with your server...");

        // Using a List system in case more compatibility checks are added.
        List<String> incompatibilities = new ArrayList<>();

        // Check the MC version of the server.
        final String currentServerVersion = getServer().getVersion();
        boolean isRunningSupportedVersion = false;
        for (String supportedServerVersion : Utils.getSupportedServerVersions()) {
            if (currentServerVersion.contains(supportedServerVersion)) {
                isRunningSupportedVersion = true;
                break;
            }
        }
        if (!isRunningSupportedVersion) {
            incompatibilities.add("Your server version &8(&b" + currentServerVersion + "&8)&7 is unsupported by &bLevelledMobs v" + getDescription().getVersion() + "&7!" +
                    "Compatible MC versions: &b" + String.join(", ", Utils.getSupportedServerVersions()) + "&7.");
        }

        if (incompatibilities.isEmpty()) {
            Utils.logger.info("&fCompatibility Checker: &7No incompatibilities found.");
        } else {
            Utils.logger.warning("&fCompatibility Checker: &7Found the following possible incompatibilities:");
            incompatibilities.forEach(incompatibility -> Utils.logger.info("&8 - &7" + incompatibility));
        }
    }

    // Note: also called by the reload subcommand.
    public void loadFiles() {
        Utils.logger.info("&fFile Loader: &7Loading files...");

        settingsCfg = FileLoader.loadFile(this, "settings", FileLoader.SETTINGS_FILE_VERSION);
        messagesCfg = FileLoader.loadFile(this, "messages", FileLoader.MESSAGES_FILE_VERSION);
    }

    private void registerListeners() {
        Utils.logger.info("&fListeners: &7Registering event listeners...");

        final PluginManager pluginManager = getServer().getPluginManager();

        creatureSpawnListener = new CreatureSpawnListener(this); // we're saving this reference so the summon command has access to it

        pluginManager.registerEvents(new EntityDamageDebugListener(this), this);
        pluginManager.registerEvents(creatureSpawnListener, this);
        pluginManager.registerEvents(new EntityDamageListener(this), this);
        pluginManager.registerEvents(new EntityDeathListener(this), this);
        pluginManager.registerEvents(new EntityRegainHealthListener(this), this);
    }

    private void registerCommands() {
        Utils.logger.info("&fCommands: &7Registering commands...");

        PluginCommand levelledMobsCommand = getCommand("levelledmobs");
        if (levelledMobsCommand == null) {
            Utils.logger.error("Command &b/levelledmobs&7 is unavailable, is it not registered in plugin.yml?");
        } else {
            levelledMobsCommand.setExecutor(new LevelledMobsCommand(this));
        }
    }

    private void setupMetrics() {
        new Metrics(this, 6269);
    }

    //Check for updates on the Spigot page.
    private void checkUpdates() {
        if (settingsCfg.getBoolean("use-update-checker")) {
            UpdateChecker updateChecker = new UpdateChecker(this, 74304);
            updateChecker.getLatestVersion(latestVersion -> {
                if (!updateChecker.getCurrentVersion().equals(latestVersion)) {
                    Utils.logger.warning("&fUpdate Checker: &7The plugin has an update available! You're running &bv" + updateChecker.getCurrentVersion() + "&7, latest version is &bv" + latestVersion + "&7.");
                }
            });
        }
    }

    //Checks if WorldGuard is available. If so, start registering its flags.
    private void checkWorldGuard() {
        hasWorldGuard = getServer().getPluginManager().getPlugin("WorldGuard") != null;
        if (hasWorldGuard) {
            worldGuardManager = new WorldGuardManager(this);
            worldGuardManager.registerFlags();
        }
    }


}
