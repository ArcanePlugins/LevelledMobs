package io.github.lokka30.levelledmobs;

import io.github.lokka30.levelledmobs.commands.LevelledMobsCommand;
import io.github.lokka30.levelledmobs.listeners.*;
import io.github.lokka30.levelledmobs.utils.*;
import me.lokka30.microlib.QuickTimer;
import me.lokka30.microlib.UpdateChecker;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the main class of the plugin. Bukkit will call onLoad and onEnable on startup, and onDisable on shutdown.
 */
public class LevelledMobs extends JavaPlugin {

    public YamlConfiguration settingsCfg;
    public YamlConfiguration messagesCfg;
    public YamlConfiguration attributesCfg;
    public ConfigUtils configUtils;

    public AttributeManager attributeManager;
    public LevelManager levelManager;


    public boolean hasWorldGuardInstalled;
    public WorldGuardManager worldGuardManager;
    public boolean nametagContainsHealth;

    private long loadTime;

    public void onLoad() {
        Utils.logger.info("&f~ Initiating start-up procedure ~");

        QuickTimer loadTimer = new QuickTimer();
        loadTimer.start(); // Record how long it takes for the plugin to load.

        attributeManager = new AttributeManager(this);
        levelManager = new LevelManager(this);

        // Hook into WorldGuard, register LM's flags.
        // This cannot be moved to onEnable (stated in WorldGuard's documentation).
        hasWorldGuardInstalled = getServer().getPluginManager().getPlugin("WorldGuard") != null;
        if (hasWorldGuardInstalled) {
            worldGuardManager = new WorldGuardManager(this);
        }

        loadTime = loadTimer.getTimer();
    }

    public void onEnable() {
        QuickTimer enableTimer = new QuickTimer();
        enableTimer.start(); // Record how long it takes for the plugin to enable.

        checkCompatibility(); //Is the server running the latest version? Dependencies required?
        loadFiles();
        registerListeners();
        registerCommands();

        Utils.logger.info("&fStart-up: &7Running misc procedures...");
        setupMetrics();
        checkUpdates();

        Utils.logger.info("&f~ Start-up complete, took &b" + (enableTimer.getTimer() + loadTime) + "ms&f ~");
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

        // save license.txt
        FileLoader.saveResourceIfNotExists(this, new File(getDataFolder(), "license.txt"));

        // load configurations
        settingsCfg = FileLoader.loadFile(this, "settings", FileLoader.SETTINGS_FILE_VERSION);
        messagesCfg = FileLoader.loadFile(this, "messages", FileLoader.MESSAGES_FILE_VERSION);

        // make sure spawn-distance-levelling AND y-distance-levelling are not both enabled.  If so we'll disable y-distance-levelling
        if (settingsCfg.getBoolean("spawn-distance-levelling.active") &&
                settingsCfg.getBoolean("y-distance-levelling.active")){
            Utils.logger.warning("both spawn-distance-levelling AND y-distance-levelling are enabled so we are disabling y-distance-levelling");
            settingsCfg.set("y-distance-levelling.active", false);
        }

        // Replace/copy attributes file
        saveResource("attributes.yml", true);
        attributesCfg = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "attributes.yml"));

        // load configutils
        configUtils = new ConfigUtils(this);

        this.nametagContainsHealth = false;
        String nametag = settingsCfg.getString("creature-nametag");
        if (Utils.isNotNullOrEmpty(nametag)){
            nametag = nametag.toLowerCase();
            this.nametagContainsHealth = nametag.contains("%health%") || nametag.contains("%max_health%");
        }
    }

    private void registerListeners() {
        Utils.logger.info("&fListeners: &7Registering event listeners...");

        final PluginManager pluginManager = getServer().getPluginManager();

        levelManager.creatureSpawnListener = new CreatureSpawnListener(this); // we're saving this reference so the summon command has access to it

        pluginManager.registerEvents(new EntityDamageDebugListener(this), this);
        pluginManager.registerEvents(levelManager.creatureSpawnListener, this);
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


}
