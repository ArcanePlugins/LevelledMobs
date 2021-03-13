package io.github.lokka30.levelledmobs;

import io.github.lokka30.levelledmobs.commands.LevelledMobsCommand;
import io.github.lokka30.levelledmobs.customdrops.CustomDropsHandler;
import io.github.lokka30.levelledmobs.listeners.*;
import io.github.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import io.github.lokka30.levelledmobs.managers.LevelManager;
import io.github.lokka30.levelledmobs.managers.MobDataManager;
import io.github.lokka30.levelledmobs.managers.WorldGuardManager;
import io.github.lokka30.levelledmobs.misc.ConfigUtils;
import io.github.lokka30.levelledmobs.misc.FileLoader;
import io.github.lokka30.levelledmobs.misc.MigrateBehavior;
import io.github.lokka30.levelledmobs.misc.Utils;
import me.lokka30.microlib.QuickTimer;
import me.lokka30.microlib.UpdateChecker;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/**
 * This is the main class of the plugin. Bukkit will call onLoad and onEnable on startup, and onDisable on shutdown.
 *
 * @author lokka30
 * @contributors stumper66
 */
public class LevelledMobs extends JavaPlugin {

    // Manager classes
    public LevelManager levelManager;
    public MobDataManager mobDataManager;
    public ExternalCompatibilityManager externalCompatibilityManager;
    public WorldGuardManager worldGuardManager;
    public CustomDropsHandler customDropsHandler;

    // Configuration
    public YamlConfiguration settingsCfg;
    public YamlConfiguration messagesCfg;
    public YamlConfiguration attributesCfg;
    public YamlConfiguration dropsCfg;
    public YamlConfiguration customDropsCfg;
    public ConfigUtils configUtils;

    // Misc
    public final PluginManager pluginManager = Bukkit.getPluginManager();
    public EntityDamageDebugListener entityDamageDebugListener;
    public int incompatibilitiesAmount;
    private long loadTime;

    // These will be moved in the near future.
    public boolean isMCVersion_16_OrHigher;
    public boolean debugEntityDamageWasEnabled = false;
    public TreeMap<String, Integer> entityTypesLevelOverride_Min;
    public TreeMap<String, Integer> entityTypesLevelOverride_Max;
    public TreeMap<String, Integer> worldLevelOverride_Min;
    public TreeMap<String, Integer> worldLevelOverride_Max;
    public Set<String> noDropMultiplierEntities;

    @Override
    public void onLoad() {
        Utils.logger.info("&f~ Initiating start-up procedure ~");

        final QuickTimer loadTimer = new QuickTimer();
        loadTimer.start(); // Record how long it takes for the plugin to load.

        mobDataManager = new MobDataManager(this);
        checkWorldGuard(); // Do not move this from onLoad. It will not work otherwise.
        externalCompatibilityManager = new ExternalCompatibilityManager(this);
        configUtils = new ConfigUtils(this);

        loadTime = loadTimer.getTimer(); // combine the load time with enable time.
    }

    @Override
    public void onEnable() {
        final QuickTimer enableTimer = new QuickTimer();
        enableTimer.start(); // Record how long it takes for the plugin to enable.

        checkCompatibility();
        if (!loadFiles()){
            // had fatal error reading required files
            this.setEnabled(false);
            return;
        }

        registerListeners();
        registerCommands();
        if (ExternalCompatibilityManager.hasProtocolLibInstalled()) levelManager.startNametagAutoUpdateTask();

        Utils.logger.info("&fStart-up: &7Running misc procedures...");
        setupMetrics();
        checkUpdates();

        Utils.logger.info("&f~ Start-up complete, took &b" + (enableTimer.getTimer() + loadTime) + "ms&f ~");
    }

    @Override
    public void onDisable() {
        Utils.logger.info("&f~ Initiating shut-down procedure ~");

        final QuickTimer disableTimer = new QuickTimer();
        disableTimer.start();

        levelManager.stopNametagAutoUpdateTask();
        shutDownAsyncTasks();

        Utils.logger.info("&f~ Shut-down complete, took &b" + disableTimer.getTimer() + "ms&f ~");
    }

    private void checkWorldGuard() {
        // Hook into WorldGuard, register LM's flags.
        // This cannot be moved to onEnable (stated in WorldGuard's documentation). It MUST be ran in onLoad.
        if (ExternalCompatibilityManager.hasWorldGuardInstalled()) {
            worldGuardManager = new WorldGuardManager();
        }
    }

    //Checks if the server version is supported
    public void checkCompatibility() {
        Utils.logger.info("&fCompatibility Checker: &7Checking compatibility with your server...");

        final String[] bukkitVer = Bukkit.getBukkitVersion().split("\\.");
        final int middleVer = Integer.parseInt(bukkitVer[1]);
        if (middleVer >= 16) {
            this.isMCVersion_16_OrHigher = true;
        }

        // Using a List system in case more compatibility checks are added.
        final List<String> incompatibilities = new ArrayList<>();

        // Check the MC version of the server.
        final String currentServerVersion = getServer().getVersion();
        boolean isRunningSupportedVersion = false;
        for (final String supportedServerVersion : Utils.getSupportedServerVersions()) {
            if (currentServerVersion.contains(supportedServerVersion)) {
                isRunningSupportedVersion = true;
                break;
            }
        }
        if (!isRunningSupportedVersion) {
            incompatibilities.add("Your server version &8(&b" + currentServerVersion + "&8)&7 is unsupported by &bLevelledMobs v" + getDescription().getVersion() + "&7!" +
                    "Compatible MC versions: &b" + String.join(", ", Utils.getSupportedServerVersions()) + "&7.");
        }

        if (!ExternalCompatibilityManager.hasProtocolLibInstalled()) {
            incompatibilities.add("Your server does not have &bProtocolLib&7 installed! This means that no levelled nametags will appear on the mobs. If you wish to see custom nametags above levelled mobs, then you must install ProtocolLib.");
        }

        incompatibilitiesAmount = incompatibilities.size();
        if (incompatibilities.isEmpty()) {
            Utils.logger.info("&fCompatibility Checker: &7No incompatibilities found.");
        } else {
            Utils.logger.warning("&fCompatibility Checker: &7Found the following possible incompatibilities:");
            incompatibilities.forEach(incompatibility -> Utils.logger.info("&8 - &7" + incompatibility));
        }
    }

    // Note: also called by the reload subcommand.
    public boolean loadFiles() {
        Utils.logger.info("&fFile Loader: &7Loading files...");

        // save license.txt
        FileLoader.saveResourceIfNotExists(this, new File(getDataFolder(), "license.txt"));

        // load configurations
        settingsCfg = FileLoader.loadFile(this, "settings", FileLoader.SETTINGS_FILE_VERSION, MigrateBehavior.MIGRATE);

        if (settingsCfg != null) // only load if settings were loaded successfully
            messagesCfg = FileLoader.loadFile(this, "messages", FileLoader.MESSAGES_FILE_VERSION, MigrateBehavior.MIGRATE);
        else {
            // had an issue reading the file.  Disable the plugin now
            return false;
        }

        customDropsCfg = FileLoader.loadFile(this, "customdrops", FileLoader.CUSTOMDROPS_FILE_VERSION, MigrateBehavior.RESET);

        this.entityTypesLevelOverride_Min = configUtils.getMapFromConfigSection("entitytype-level-override.min-level");
        this.entityTypesLevelOverride_Max = configUtils.getMapFromConfigSection("entitytype-level-override.max-level");
        this.worldLevelOverride_Min = configUtils.getMapFromConfigSection("world-level-override.min-level");
        this.worldLevelOverride_Max = configUtils.getMapFromConfigSection("world-level-override.max-level");
        this.noDropMultiplierEntities = configUtils.getSetFromConfigSection("no-drop-multipler-entities");

        attributesCfg = loadEmbeddedResource("attributes.yml");
        dropsCfg = loadEmbeddedResource("drops.yml");

        configUtils.load();
        externalCompatibilityManager.load();

        // remove legacy files if they exist
        final String[] legacyFile = {"attributes.yml", "drops.yml"};
        for (String lFile : legacyFile) {
            final File delFile = new File(getDataFolder(), lFile);
            try {
                if (delFile.exists()) delFile.delete();
            } catch (Exception e) {
                Utils.logger.warning("Unable to delete file " + lFile + ", " + e.getMessage());
            }
        }

        // build custom drops
        customDropsHandler = new CustomDropsHandler(this);

        return true;
    }

    @Nullable
    private YamlConfiguration loadEmbeddedResource(final String filename){
        YamlConfiguration result = null;
        final InputStream inputStream = this.getResource(filename);
        if (inputStream == null) return null;

        try {
            InputStreamReader reader = new InputStreamReader(inputStream);
            result = YamlConfiguration.loadConfiguration(reader);
            reader.close();
            inputStream.close();
        }
        catch (IOException e){
            Utils.logger.error("Error reading embedded file: " + filename + ", " + e.getMessage());
        }

        return result;
    }



    private void registerListeners() {
        Utils.logger.info("&fListeners: &7Registering event listeners...");

        levelManager = new LevelManager(this);
        levelManager.creatureSpawnListener = new CreatureSpawnListener(this); // we're saving this reference so the summon command has access to it
        entityDamageDebugListener = new EntityDamageDebugListener(this);

        if (settingsCfg.getBoolean("debug-entity-damage")) {
            // we'll load and unload this listener based on the above setting when reloading
            debugEntityDamageWasEnabled = true;
            pluginManager.registerEvents(this.entityDamageDebugListener, this);
        }

        pluginManager.registerEvents(levelManager.creatureSpawnListener, this);
        pluginManager.registerEvents(new EntityDamageListener(this), this);
        pluginManager.registerEvents(new EntityDeathListener(this), this);
        pluginManager.registerEvents(new EntityRegainHealthListener(this), this);
        pluginManager.registerEvents(new PlayerJoinWorldNametagListener(this), this);
        pluginManager.registerEvents(new EntityTransformListener(this), this);
        pluginManager.registerEvents(new EntityNametagListener(this), this);
        pluginManager.registerEvents(new EntityTargetListener(this), this);
        pluginManager.registerEvents(new PlayerJoinListener(this), this);
        pluginManager.registerEvents(new EntityTameListener(this), this);

        if (ExternalCompatibilityManager.hasMythicMobsInstalled())
            pluginManager.registerEvents(new MythicMobsListener(this), this);
    }

    private void registerCommands() {
        Utils.logger.info("&fCommands: &7Registering commands...");

        final PluginCommand levelledMobsCommand = getCommand("levelledmobs");
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
            final UpdateChecker updateChecker = new UpdateChecker(this, 74304);
            updateChecker.getLatestVersion(latestVersion -> {
                if (!updateChecker.getCurrentVersion().equals(latestVersion)) {
                    Utils.logger.warning("&fUpdate Checker: &7The plugin has an update available! You're running &bv" + updateChecker.getCurrentVersion() + "&7, latest version is &bv" + latestVersion + "&7.");
                }
            });
        }
    }

    private void shutDownAsyncTasks() {
        Utils.logger.info("&fTasks: &7Shutting down other async tasks...");
        Bukkit.getScheduler().cancelTasks(this);
    }
}
