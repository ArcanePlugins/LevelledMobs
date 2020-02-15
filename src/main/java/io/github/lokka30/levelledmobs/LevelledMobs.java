package io.github.lokka30.levelledmobs;

import de.leonhard.storage.LightningBuilder;
import de.leonhard.storage.internal.FlatFile;
import de.leonhard.storage.internal.exception.LightningValidationException;
import io.github.lokka30.levelledmobs.commands.CLevelledMobs;
import io.github.lokka30.levelledmobs.listeners.LDebug;
import io.github.lokka30.levelledmobs.listeners.LMobSpawn;
import io.github.lokka30.levelledmobs.listeners.LTagUpdate;
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

    private static LevelledMobs instance;
    public FlatFile settings;
    public NamespacedKey key;

    public static LevelledMobs getInstance() {
        return instance;
    }

    public void onLoad() {
        instance = this;
    }

    public void onEnable() {
        log(LogLevel.INFO, "&8[&71&8/&75&8] &7Checking compatibility...");
        checkCompatibility();

        log(LogLevel.INFO, "&8[&72&8/&75&8] &7Loading files...");
        loadFiles();

        log(LogLevel.INFO, "&8[&73&8/&75&8] &7Registering events...");
        key = new NamespacedKey(this, "level");
        registerEvents();

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
        final String path = "plugins/PhantomEconomy/";
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

        pm.registerEvents(new LMobSpawn(), this);
        pm.registerEvents(new LDebug(), this);
        pm.registerEvents(new LTagUpdate(), this);
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

    //Updates the nametag of a creature
    public void updateTag(final Entity entity) {
        if (entity instanceof LivingEntity && settings.get("enable-nametag-changes", true)) {
            final LivingEntity livingEntity = (LivingEntity) entity;

            if (entity.getPersistentDataContainer().get(key, PersistentDataType.INTEGER) == null) {
                return;
            }

            if (instance.isLevellable(livingEntity)) {
                String customName = settings.get("creature-nametag", "&8[&7Level %level%&8 | &f%name%&8 | &c%health%&8/&c%max_health% %heart_symbol%&8]")
                        .replaceAll("%level%", entity.getPersistentDataContainer().get(key, PersistentDataType.INTEGER) + "")
                        .replaceAll("%name%", StringUtils.capitalize(entity.getType().name().toLowerCase()))
                        .replaceAll("%health%", round(livingEntity.getHealth(), 1) + "")
                        .replaceAll("%max_health%", round(Objects.requireNonNull(livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue(), 1) + "")
                        .replaceAll("%heart_symbol%", "❤");
                entity.setCustomName(colorize(customName));

                entity.setCustomNameVisible(settings.get("fine-tuning.custom-name-visible", false));
            }
        }
    }

    public double round(double value, int places) {
        //Credit: Jonik & Mustapha Hadid @ stackoverflow
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }
}
