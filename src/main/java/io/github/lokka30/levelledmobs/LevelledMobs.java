package io.github.lokka30.levelledmobs;

import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import de.leonhard.storage.LightningBuilder;
import de.leonhard.storage.internal.FlatFile;
import de.leonhard.storage.internal.exception.LightningValidationException;
import io.github.lokka30.levelledmobs.commands.CLevelledMobs;
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
    Information for Developers, about my weird programming stuff and what to know if you're going to touch this plugin.
    I apologise for the poor formatting. I just wanted to get it out of the way.

    - Class naming
     - I've adopted my own class naming thing. CommandExecutor classes begin with 'C' and Listener classes begin with 'L'.
      - This is so, for example, if I have a command called 'Death' and a listener called 'Death', the class names don't clash.
      - Also, when looking up the class in IntelliJ, I can easily start with a 'C' or a 'L' and it only shows the listeners, for example, in the suggested words.
    - Naming: LivingEntity !!!livingEntity!!! = e.getE ...
    - Always use curly brackets in if statements and such.
    - This plugin uses LightningStorage. Might put this wall of text in a different file included with the jar, or in the wiki.
      The syntax is [instance.]settings.get("path", (Object) defaultValue).
       Let me get a String in the file: instance.settings.get("string", "the default string here in case the settings file doesn't contain 'string'.")

       If that isn't enough to tell you how to use lightningStorage, see below:
       - "path" is the path of the setting.
       - (Object) is the object type that the value is. For example, getting a float from the settings file:
        - instance.settings.get("float-value", 1.2F)
     or - instance.settings.get("float-value", (float) 1.2)
       - defaultValue is the default value returned if the path doesn't exist.
        - For example, if a user hasn't updated their settings file, the defaultValue will be used instead of
          what is meant to be there. If the value in the settings file is too large to put into the defaultValue area,
          such as a large list of mob names or something, you can simply use a few of the values as
          Collection.asSingletonList("ZOMBIE", "CREEPER"), or Arrays.asList("ZOMBIE", "CREEPER").
       - e.g. getting a List<String> from the config and checking each value
        - for(String s : instance.settings.get("string-list", Collection.asSingletonList("ZOMBIE", "STRING2")) {
           if(s.equals("STRING2") {
            ...
           }
          }
      - When updating the settings file, change the value in the bottom of the settings.yml file and in the Utils class.
     */

    private static LevelledMobs instance; //The main class is stored here so other classes can access it.
    public static StateFlag allowlevelflag;
    public FlatFile settings; //The settings config file managed by LightningStorage.
    public NamespacedKey key; //The NamespacedKey which holds each levellable mob's level value.
    public static StringFlag minlevelflag, maxlevelflag;
    public LevelManager levelManager; //The LevelManager class which holds a bunch of common methods
    //WorldGuard
    public boolean worldguard; //if worldguard is on the server

    //Returns the instance class. Will return null if the plugin is disabled - this shouldn't happen.
    public static LevelledMobs getInstance() {
        return instance;
    }

    //When the plugin starts loading (when Bukkit announces that it's loading, but it hasn't started the enable process).
    //onLoad should only be used for setting the instance, or other classes, such as LevelManager.
    public void onLoad() {
        instance = this;
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
        key = new NamespacedKey(this, "level"); //Set the NamespacedKey, holding the mob's level.
        registerEvents(); //Start registering the listeners - these classes start with L.

        log(LogLevel.INFO, "&8[&74&8/&76&8] &7Registering commands...");
        registerCommands();

        log(LogLevel.INFO, "&8[&75&8/&76&8] &7Hooking to other plugins...");
        //will be added in the future.

        log(LogLevel.INFO, "&8[&76&8/&76&8] &7Starting metrics...");
        new Metrics(this);

        log(LogLevel.INFO, "Loaded successfuly. Thank you for choosing LevelledMobs!");

        checkUpdates();
    }

    public void onDisable() {
        instance = null;
    }

    //Checks if the server version is supported
    private void checkCompatibility() {
        final String currentVersion = getServer().getVersion();
        final String recommendedVersion = Utils.getRecommendedServerVersion();
        if (currentVersion.contains(recommendedVersion)) {
            log(LogLevel.INFO, "Server is running supported version &a" + currentVersion + "&7.");
        } else {
            log(LogLevel.WARNING, " ");
            log(LogLevel.WARNING, "Server is running &cunsupported&7 version &a" + currentVersion + "&7.");
            log(LogLevel.WARNING, "The recommended version is &a" + recommendedVersion + "&7.");
            log(LogLevel.WARNING, "You will not get support with the plugin whilst using an unsupported version!");
            log(LogLevel.WARNING, " ");
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
        if (settings.get("file-version", 0) != Utils.getRecommendedSettingsVersion()) {
            log(LogLevel.SEVERE, "File &asettings.yml&7 is out of date! Lower-quality default values will be used for the new features! Reset it or merge the old values to the new file.");
        }
    }

    //Registers the listener classes.
    private void registerEvents() {
        final PluginManager pm = getServer().getPluginManager();

        pm.registerEvents(new LDebugEntityDamage(), this);
        pm.registerEvents(new LCreatureSpawn(), this);
        pm.registerEvents(new LEntityDamage(), this);
        pm.registerEvents(new LEntityDeath(), this);
        pm.registerEvents(new LEntityRegainHealth(), this);
    }

    //Registers the command classes.
    private void registerCommands() {
        Objects.requireNonNull(getCommand("LevelledMobs")).setExecutor(new CLevelledMobs());
    }

    //Checks if WorldGuard is available. If so, start registering its flags.
    private void checkWorldGuard() {
        worldguard = getServer().getPluginManager().getPlugin("WorldGuard") != null;
        if (worldguard) {
            WorldGuardManager.registerFlags();
        }
    }

    //Check for updates on the Spigot page.
    private void checkUpdates() {
        if (settings.get("updater", true)) {
            log(LogLevel.INFO, "&8[&7Update Checker&8] &7Starting version comparison...");
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
                throw new IllegalStateException("Unexpected LogLevel value: " + level);
        }
    }

}
