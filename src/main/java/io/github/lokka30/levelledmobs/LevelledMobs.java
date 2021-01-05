package io.github.lokka30.levelledmobs;

import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import de.leonhard.storage.LightningBuilder;
import de.leonhard.storage.internal.FlatFile;
import de.leonhard.storage.internal.exceptions.LightningValidationException;
import io.github.lokka30.levelledmobs.commands.LevelledMobsCommand;
import io.github.lokka30.levelledmobs.listeners.*;
import io.github.lokka30.levelledmobs.utils.*;
import io.github.lokka30.phantomlib.PhantomLib;
import io.github.lokka30.phantomlib.classes.MessageMethods;
import io.github.lokka30.phantomlib.classes.PhantomLogger;
import io.github.lokka30.phantomlib.enums.LogLevel;
import org.bstats.bukkit.Metrics;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class LevelledMobs extends JavaPlugin {

    public static StateFlag allowlevelflag; //The WorldGuard flag if mobs can be levelled.
    public PhantomLib phantomLib;
    public PhantomLogger phantomLogger;
    public String PREFIX = "&b&lLevelledMobs: &7";
    public MessageMethods messageMethods;
    public static StringFlag minlevelflag, maxlevelflag; //The WorldGuard flags of the min and max mob levels.
    public boolean hasWorldGuard; //if worldguard is on the server
    public WorldGuardManager worldGuardManager; //The WorldGuardManager class brings WorldGuard support to LM.
    public FlatFile settings; //The settings config file.
    public FileCache fileCache; //The class which stores the settings in memory and provides useful methods for getting specific settings
    public NamespacedKey levelKey; //What's the mob's level?
    public NamespacedKey isLevelledKey; //Is the mob levelled?
    public LevelManager levelManager; //The LevelManager class which holds a bunch of common methods
    public Utils utils; //The Utils class which holds some common utility methods
    private PluginManager pluginManager;

    //When the plugin starts loading (when Bukkit announces that it's loading, but it hasn't started the enable process).
    //onLoad should only be used for setting other classes like LevelManager.
    public void onLoad() {
        pluginManager = getServer().getPluginManager();

        //Make sure that PhantomLib is installed.
        if (pluginManager.getPlugin("PhantomLib") == null) {
            getLogger().severe(" ----- WARNING -----");
            getLogger().severe("PhantomLib is not installed! You must install PhantomLib for the plugin to function.");
            getLogger().severe("Link to the SpigotMC resource: https://www.spigotmc.org/resources/%E2%99%A6-phantomlib-%E2%99%A6-1-7-10-1-15-2.78556/");
            getLogger().severe("Plugin will now disable itself!");
            getLogger().severe(" ----- WARNING -----");
            pluginManager.disablePlugin(this);
            return;
        } else {
            phantomLib = PhantomLib.getInstance();
            phantomLogger = phantomLib.getPhantomLogger();
            messageMethods = phantomLib.getMessageMethods();
        }

        utils = new Utils();
        levelManager = new LevelManager(this);
        checkWorldGuard(); //WorldGuard check is onLoad as it is required to be here instead of onEnable (as stated in its documentation).
    }

    public void onEnable() {
        phantomLogger.log(LogLevel.INFO, PREFIX, "&8+----+ &f(Enable Started) &8+----+");
        final long startingTime = System.currentTimeMillis();

        checkCompatibility(); //Is the server running the latest version? Dependencies required?

        phantomLogger.log(LogLevel.INFO, PREFIX, "&8(&3Startup &8- &32&8/&36&8) &7Loading files...");
        loadFiles();

        levelKey = new NamespacedKey(this, "level");
        isLevelledKey = new NamespacedKey(this, "isLevelled");
        registerEvents();

        registerCommands();

        hookToOtherPlugins();

        setupMetrics();

        phantomLogger.log(LogLevel.INFO, PREFIX, "&8+----+ &f(Enable Complete, took " + (System.currentTimeMillis() - startingTime) + "ms) &8+----+");

        checkUpdates();
    }

    //Checks if the server version is supported
    private void checkCompatibility() {
        phantomLogger.log(LogLevel.INFO, PREFIX, "&8(&3Startup &8- &31&8/&36&8) &7Checking compatibility...");

        final String currentVersion = getServer().getVersion();
        boolean isRunningSupportedVersion = false;
        for (String supportedVersion : utils.getSupportedServerVersions()) {
            if (currentVersion.contains(supportedVersion)) {
                isRunningSupportedVersion = true;
                break;
            }
        }
        if (!isRunningSupportedVersion) {
            phantomLogger.log(LogLevel.WARNING, PREFIX, "'&b" + currentVersion + "&7' is not a supported server version for this version of LevelledMobs. You will not receive the author's support whilst running this unsupported configuration.");
        }
    }

    //Manages the setting file.
    public void loadFiles() {
        try {
            settings = LightningBuilder
                    .fromFile(new File(getDataFolder(), "settings"))
                    .addInputStreamFromResource("settings.yml")
                    .createYaml();
        } catch (LightningValidationException e) {
            phantomLogger.log(LogLevel.SEVERE, PREFIX, "Unable to load &bsettings.yml&7! Disabling plugin.");
            pluginManager.disablePlugin(this);
            return;
        }

        //Check if they exist
        final File settingsFile = new File(getDataFolder(), "settings.yml");
        

        if (!(settingsFile.exists() && !settingsFile.isDirectory())) {
            phantomLogger.log(LogLevel.INFO, PREFIX, "File &bsettings.yml&7 doesn't exist. Creating it now.");
            saveResource("settings.yml", false);
        }

        //Check their versions
        if (settings.get("file-version", 0) != utils.getLatestSettingsVersion()) {
            phantomLogger.log(LogLevel.SEVERE, PREFIX, "File &bsettings.yml&7 is out of date! Lower-quality default values will be used for the new changes! Reset it or merge the old values to the new file.");
        }

        fileCache = new FileCache(this);
        fileCache.loadLatest();
    }

    //Registers the listener classes.
    private void registerEvents() {
        phantomLogger.log(LogLevel.INFO, PREFIX, "&8(&3Startup &8- &33&8/&36&8) &7Registering events...");

        pluginManager.registerEvents(new EntityDamageDebugListener(this), this);
        pluginManager.registerEvents(new CreatureSpawnListener(this), this);
        pluginManager.registerEvents(new EntityDamageListener(this), this);
        pluginManager.registerEvents(new EntityDeathListener(this), this);
        pluginManager.registerEvents(new EntityRegainHealthListener(this), this);
    }

    //Registers the command classes.
    private void registerCommands() {
        phantomLogger.log(LogLevel.INFO, PREFIX, "&8(&3Startup &8- &34&8/&36&8) &7Registering commands...");

        phantomLib.getCommandRegister().registerCommand(this, "levelledmobs", new LevelledMobsCommand(this));
    }

    // Things will be added in the future if needed.
    private void hookToOtherPlugins() {
        phantomLogger.log(LogLevel.INFO, PREFIX, "&8(&3Startup &8- &35&8/&36&8) &7Hooking to other plugins...");

        //...
    }

    private void setupMetrics() {
        phantomLogger.log(LogLevel.INFO, PREFIX, "&8(&3Startup &8- &36&8/&36&8) &7Setting up bStats...");

        new Metrics(this, 6269);
    }

    //Checks if WorldGuard is available. If so, start registering its flags.
    private void checkWorldGuard() {
        hasWorldGuard = getServer().getPluginManager().getPlugin("WorldGuard") != null;
        if (hasWorldGuard) {
            worldGuardManager = new WorldGuardManager(this);
            worldGuardManager.registerFlags();
        }
    }

    //Check for updates on the Spigot page.
    private void checkUpdates() {
        if (fileCache.SETTINGS_USE_UPDATE_CHECKER) {
            phantomLogger.log(LogLevel.INFO, PREFIX, "&8(&3Update Checker&8) &7Checking for updates...");
            new UpdateChecker(this, 74304).getVersion(version -> {
                if (getDescription().getVersion().equalsIgnoreCase(version)) {
                    phantomLogger.log(LogLevel.INFO, PREFIX, "&8(&3Update Checker&8) &7You're running the latest version.");
                } else {
                    phantomLogger.log(LogLevel.WARNING, PREFIX, "&8(&3Update Checker&8) &7There's a new update available: '&b" + version + "&7'. You're running '&b" + getDescription().getVersion() + "&7'.");
                }
            });
        }
    }

}
