package io.github.lokka30.levelledmobs;

import de.leonhard.storage.LightningBuilder;
import de.leonhard.storage.internal.FlatFile;
import io.github.lokka30.levelledmobs.commands.CLevelledMobs;
import io.github.lokka30.levelledmobs.listeners.LMobSpawn;
import io.github.lokka30.levelledmobs.utils.LogLevel;
import io.github.lokka30.levelledmobs.utils.UpdateChecker;
import org.bstats.bukkit.Metrics;
import org.bukkit.ChatColor;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;
import java.util.logging.Logger;

public class LevelledMobs extends JavaPlugin {

    private static LevelledMobs instance;
    public final String recommendedVersion = "1.15.1";
    public FlatFile settings;

    public static LevelledMobs getInstance() {
        return instance;
    }

    public void onLoad() {
        instance = this;
    }

    public void onEnable() {
        log(LogLevel.INFO, "&8&m+------------------------------+");
        log(LogLevel.INFO, "&8[&71&8/&75&8] &7Checking compatibility...");
        checkCompatibility();

        log(LogLevel.INFO, "&8[&72&8/&75&8] &7Loading files...");
        loadFiles();

        log(LogLevel.INFO, "&8[&73&8/&75&8] &7Registering events...");
        registerEvents();

        log(LogLevel.INFO, "&8[&74&8/&75&8] &7Registering commands...");
        registerCommands();

        log(LogLevel.INFO, "&8[&75&8/&75&8] &7Starting metrics...");
        new Metrics(this);

        log(LogLevel.INFO, "&8[&7Loaded&8] &7Thank you for using LevelledMobs. Enjoy!");
        log(LogLevel.INFO, "&8&m+------------------------------+");

        checkUpdates();
    }

    public void onDisable() {
        instance = null;
    }

    private void checkCompatibility() {
        final String version = getServer().getVersion();
        if (!version.contains(recommendedVersion)) {
            log(LogLevel.WARNING, "Your server is running the unsupported version &a" + version + "&7. Please switch to the only supported version, &a" + recommendedVersion + "&7, otherwise issues may occur.");
        }
    }

    private void loadFiles() {
        settings = LightningBuilder
                .fromFile(new File("plugins/LevelledMobs/settings"))
                .addInputStreamFromResource("settings.yml")
                .createYaml();

        int recommendedSettingsVersion = 6;
        if (settings.get("file-version") == null) {
            saveResource("settings.yml", true);
        } else if (settings.getInt("file-version") != recommendedSettingsVersion) {
            log(LogLevel.WARNING, "Your &asettings.yml&7 file is outdated, please update it to version &a" + recommendedSettingsVersion + "&7.");
        }
    }

    private void registerEvents() {
        final PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new LMobSpawn(), this);
    }

    private void registerCommands() {
        Objects.requireNonNull(getCommand("LevelledMobs")).setExecutor(new CLevelledMobs());
    }

    private void checkUpdates() {
        if (settings.get("updater", true)) {
            log(LogLevel.INFO, "&8[&7Update Checker&8] &7Retrieving version comparison...");
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
                throw new IllegalStateException("Unexpected LogLevel: " + level);
        }
    }
}
