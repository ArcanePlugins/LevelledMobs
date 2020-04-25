package io.github.lokka30.levelledmobs;

import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import de.leonhard.storage.LightningBuilder;
import de.leonhard.storage.internal.FlatFile;
import de.leonhard.storage.internal.exception.LightningValidationException;
import io.github.lokka30.levelledmobs.commands.LevelledMobsCommand;
import io.github.lokka30.levelledmobs.listeners.*;
import io.github.lokka30.levelledmobs.utils.*;
import org.bstats.bukkit.Metrics;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;
import java.util.logging.Logger;

public class LevelledMobs extends JavaPlugin {

    /*
    Are you planning on contributing to the plugin?
    Thanks! Please take note:

    - Please name variables as the 'good' example:
     - (GOOD) LivingEntity livingEntity
     - (OKAY) LivingEntity entity
     - (BAD) LivingEntity le / e / l
     - (GOOD) PlayerJoinEvent e (either is fine)
     - (GOOD) PlayerJoinEvent playerJoinEvent (either is fine)
    - Please use curly brackets in if/for/class/etc.
     */

    public static StateFlag allowlevelflag; //The WorldGuard flag if mobs can be levelled.
    public static StringFlag minlevelflag, maxlevelflag; //The WorldGuard flags of the min and max mob levels.
    public FlatFile settings; //The settings config file.
    public boolean hasWorldGuard; //if worldguard is on the server
    public NamespacedKey levelKey; //What's the mob's level?
    public NamespacedKey isLevelledKey; //Is the mob levelled?
    public LevelManager levelManager; //The LevelManager class which holds a bunch of common methods
    public Utils utils; //The Utils class which holds some common utility methods
    public WorldGuardManager worldGuardManager; //The WorldGuardManager class brings WorldGuard support to LM.

    //When the plugin starts loading (when Bukkit announces that it's loading, but it hasn't started the enable process).
    //onLoad should only be used for setting other classes like LevelManager.
    public void onLoad() {
        utils = new Utils();
        levelManager = new LevelManager(this);
        checkWorldGuard(); //WorldGuard check is onLoad as it is required to be here instead of onEnable (as stated in its documentation).
    }

    //When the plugin starts enabling.
    public void onEnable() {
        log(LogLevel.INFO, "&8[&71&8/&76&8] &7Checking compatibility...");
        checkCompatibility(); //Is the server running the latest version? Dependencies required?

        log(LogLevel.INFO, "&8[&72&8/&76&8] &7Loading files...");
        loadFiles(); //Tell LightningStorage to get things started. Check if there's something wrong going on, such as an outdated file.

        log(LogLevel.INFO, "&8[&73&8/&76&8] &7Registering events...");
        levelKey = new NamespacedKey(this, "level");
        isLevelledKey = new NamespacedKey(this, "isLevelled");
        registerEvents(); //Start registering the listeners - these classes start with L.

        log(LogLevel.INFO, "&8[&74&8/&76&8] &7Registering commands...");
        registerCommands();

        log(LogLevel.INFO, "&8[&75&8/&76&8] &7Hooking to other plugins...");
        //will be added in the future.

        log(LogLevel.INFO, "&8[&76&8/&76&8] &7Starting metrics...");
        new Metrics(this, 6269);

        log(LogLevel.INFO, "Loaded successfuly. Thank you for choosing LevelledMobs!");

        checkUpdates();
    }

    //Checks if the server version is supported
    private void checkCompatibility() {
        final String currentVersion = getServer().getVersion();
        final String recommendedVersion = utils.getRecommendedServerVersion();
        if (currentVersion.contains(recommendedVersion)) {
            log(LogLevel.INFO, "Server is running supported version &a" + currentVersion + "&7.");
        } else {
            log(LogLevel.INFO, "&a" + currentVersion + "&7 is not a recommended server version! You will not receive support if you encounter issues whilst running this server version.");
        }
    }

    //Manages the LightningStorage files.
    private void loadFiles() {
        //Load the files
        final PluginManager pm = getServer().getPluginManager();
        final String path = "plugins/LevelledMobs/";
        try {
            settings = LightningBuilder
                    .fromFile(new File(path + "settings"))
                    .addInputStreamFromResource("settings.yml")
                    .createYaml();
        } catch (LightningValidationException e) {
            log(LogLevel.SEVERE, "Unable to load &asettings.yml&7!");
            pm.disablePlugin(this);
            return;
        }

        //Check if they exist
        final File settingsFile = new File(path + "settings.yml");

        if (!(settingsFile.exists() && !settingsFile.isDirectory())) {
            log(LogLevel.INFO, "File &asettings.yml&7 doesn't exist. Creating it now.");
            saveResource("settings.yml", false);
        }

        //Check their versions
        if (settings.get("file-version", 0) != utils.getRecommendedSettingsVersion()) {
            log(LogLevel.SEVERE, "File &asettings.yml&7 is out of date! Lower-quality default values will be used for the new features! Reset it or merge the old values to the new file.");
        }
    }

    //Registers the listener classes.
    private void registerEvents() {
        final PluginManager pm = getServer().getPluginManager();

        pm.registerEvents(new EntityDamageDebugListener(this), this);
        pm.registerEvents(new CreatureSpawnListener(this), this);
        pm.registerEvents(new EntityDamageListener(this), this);
        pm.registerEvents(new EntityDeathListener(this), this);
        pm.registerEvents(new EntityRegainHealthListener(this), this);
    }

    //Registers the command classes.
    private void registerCommands() {
        Objects.requireNonNull(getCommand("levelledmobs")).setExecutor(new LevelledMobsCommand(this));
    }

    //Checks if WorldGuard is available. If so, start registering its flags.
    private void checkWorldGuard() {
        hasWorldGuard = getServer().getPluginManager().getPlugin("WorldGuard") != null;
        if (hasWorldGuard) {
            worldGuardManager = new WorldGuardManager(this);
            worldGuardManager.registerFlags();
            log(LogLevel.INFO, "WorldGuard hook &aenabled&7.");
        } else {
            log(LogLevel.INFO, "WorldGuard hook &cdisabled&7.");
        }
    }

    //Check for updates on the Spigot page.
    private void checkUpdates() {
        if (settings.get("updater", true)) {
            log(LogLevel.INFO, "&8[&7Update Checker&8] &7Checking for updates...");
            new UpdateChecker(this, 74304).getVersion(version -> {
                if (getDescription().getVersion().equalsIgnoreCase(version)) {
                    log(LogLevel.INFO, "&8[&7Update Checker&8] &7You're running the latest version.");
                } else {
                    log(LogLevel.WARNING, "&8[&7Update Checker&8] &7There's a new update available: &a" + version + "&7. You're running &a" + getDescription().getVersion() + "&7.");
                }
            });
        }
    }

    //Replace color codes in the message with colors.
    public String colorize(final String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    //My favoured logging system.
    public void log(final LogLevel level, String msg) {
        final Logger logger = getLogger();
        msg = colorize("&7" + msg);
        switch (level) {
            case INFO:
                logger.info(msg);
                break;
            case WARNING:
                logger.warning(msg);
                break;
            case SEVERE:
                logger.severe(msg);
                break;
            default:
                throw new IllegalStateException("Unexpected LogLevel value: " + level + ". Message: " + msg);
        }
    }

}
