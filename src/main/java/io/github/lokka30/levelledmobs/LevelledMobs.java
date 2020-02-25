package io.github.lokka30.levelledmobs;

import de.leonhard.storage.LightningBuilder;
import de.leonhard.storage.internal.FlatFile;
import de.leonhard.storage.internal.exception.LightningValidationException;
import io.github.lokka30.levelledmobs.commands.CLevelledMobs;
import io.github.lokka30.levelledmobs.listeners.LDebug;
import io.github.lokka30.levelledmobs.listeners.LMobDamage;
import io.github.lokka30.levelledmobs.listeners.LMobDeath;
import io.github.lokka30.levelledmobs.listeners.LMobSpawn;
import io.github.lokka30.levelledmobs.utils.LogLevel;
import io.github.lokka30.levelledmobs.utils.UpdateChecker;
import io.github.lokka30.levelledmobs.utils.Utils;
import org.apache.commons.lang.StringUtils;
import org.bstats.bukkit.Metrics;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Arrays;
import java.util.List;
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
    - This plugin uses LightningStorage.
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
    public FlatFile settings; //The settings config file managed by LightningStorage.
    public NamespacedKey key; //The NamespacedKey which holds each levellable mob's level value.

    //Returns the instance class. Will return null if the plugin is disabled - this shouldn't happen.
    public static LevelledMobs getInstance() {
        return instance;
    }

    //When the plugin starts loading (when Bukkit announces that it's loading, but it isn't actually enabling yet),
    //the instance is set.
    public void onLoad() {
        instance = this;
    }

    //When the plugin starts enabling.
    public void onEnable() {
        log(LogLevel.INFO, "&8[&71&8/&75&8] &7Checking compatibility...");
        checkCompatibility(); //Is the server running the latest version? Dependencies required?

        log(LogLevel.INFO, "&8[&72&8/&75&8] &7Loading files...");
        loadFiles(); //Tell LightningStorage to get things started. Check if there's something wrong going on, such as an outdated file.

        log(LogLevel.INFO, "&8[&73&8/&75&8] &7Registering events...");
        key = new NamespacedKey(this, "level"); //Set the NamespacedKey, holding the mob's level.
        registerEvents(); //Start registering the listeners - these classes start with L.

        log(LogLevel.INFO, "&8[&74&8/&75&8] &7Registering commands...");
        registerCommands();

        log(LogLevel.INFO, "&8[&75&8/&75&8] &7Starting metrics...");
        new Metrics(this);

        log(LogLevel.INFO, "Loaded successfuly. Thank you for choosing LevelledMobs!");

        checkUpdates();
    }

    public void onDisable() {
        instance = null;
    }

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

    private void registerEvents() {
        final PluginManager pm = getServer().getPluginManager();

        pm.registerEvents(new LDebug(), this);
        pm.registerEvents(new LMobSpawn(), this);
        pm.registerEvents(new LMobDamage(), this);
        pm.registerEvents(new LMobDeath(), this);
    }

    private void registerCommands() {
        Objects.requireNonNull(getCommand("LevelledMobs")).setExecutor(new CLevelledMobs());
    }

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

    public String colorize(final String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

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

    //Checks if an entity can be levelled.
    public boolean isLevellable(final LivingEntity entity) {
        //Checks for the 'blacklisted-types' option.
        List<String> blacklistedTypes;

        //Set it to what's specified. If it's invalid, it'll just take a small predefiend list.
        blacklistedTypes = instance.settings.get("blacklisted-types", Arrays.asList("VILLAGER", "WANDERING_TRADER", "ENDER_DRAGON", "WITHER"));
        for (String blacklistedType : blacklistedTypes) {
            if (entity.getType().toString().equalsIgnoreCase(blacklistedType)) {
                return false;
            }
        }

        //Checks for the 'level-passive' option.
        return entity instanceof Monster || instance.settings.get("level-passive", false);
    }

    //Updates the nametag of a creature. Gets called by certain listeners.
    public void updateTag(final Entity entity) {
        if (entity instanceof LivingEntity && settings.get("enable-nametag-changes", true)) { //if the settings allows nametag changes, go ahead.
            final LivingEntity livingEntity = (LivingEntity) entity;

            if (entity.getPersistentDataContainer().get(key, PersistentDataType.INTEGER) == null) { //if the entity doesn't contain a level, skip this.
                return;
            }

            if (instance.isLevellable(livingEntity)) { // If the mob is levellable, go ahead.
                String customName = settings.get("creature-nametag", "&8[&7Level %level%&8 | &f%name%&8 | &c%health%&8/&c%max_health% %heart_symbol%&8]")
                        .replaceAll("%level%", entity.getPersistentDataContainer().get(key, PersistentDataType.INTEGER) + "")
                        .replaceAll("%name%", StringUtils.capitalize(entity.getType().name().toLowerCase()))
                        .replaceAll("%health%", round(livingEntity.getHealth(), 1) + "")
                        .replaceAll("%max_health%", round(Objects.requireNonNull(livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue(), 1) + "")
                        .replaceAll("%heart_symbol%", "❤");
                entity.setCustomName(colorize(customName));

                // CustomNameVisible
                // If true, players can see it from afar and through walls and roofs and the surface of the world if under caves.
                // If false, players can only see it when looking directly at it and within 4 or so blocks.
                //
                // I can't change anything else here, as it's a Minecraft feature.
                // Unfortunately no hybrid between the two where you can't see it through caves and that. :(
                entity.setCustomNameVisible(settings.get("fine-tuning.custom-name-visible", false));
            }
        }
    }

    //This is a method created by Jonik & Mustapha Hadid at StackOverflow.
    //It simply grabs 'value', being a double, and rounds it, leaving 'places' decimal places intact.
    public double round(double value, int places) {
        //Credit: Jonik & Mustapha Hadid @ stackoverflow
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }
}
