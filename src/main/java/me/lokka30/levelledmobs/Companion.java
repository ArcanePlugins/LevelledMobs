package me.lokka30.levelledmobs;

import me.lokka30.levelledmobs.commands.LevelledMobsCommand;
import me.lokka30.levelledmobs.customdrops.CustomDropsHandler;
import me.lokka30.levelledmobs.listeners.*;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.managers.LevelManager;
import me.lokka30.levelledmobs.managers.WorldGuardManager;
import me.lokka30.levelledmobs.misc.FileLoader;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.microlib.UpdateChecker;
import me.lokka30.microlib.VersionUtils;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * This class contains methods used by the main class.
 *
 * @author lokka30
 * @contributors stumper66
 */
public class Companion {

    private final LevelledMobs main;

    public Companion(final LevelledMobs main) {
        this.main = main;
    }

    private final PluginManager pluginManager = Bukkit.getPluginManager();

    protected void checkWorldGuard() {
        // Hook into WorldGuard, register LM's flags.
        // This cannot be moved to onEnable (stated in WorldGuard's documentation). It MUST be ran in onLoad.
        if (ExternalCompatibilityManager.hasWorldGuardInstalled()) {
            main.worldGuardManager = new WorldGuardManager();
        }
    }

    //Checks if the server version is supported
    public void checkCompatibility() {
        Utils.logger.info("&fCompatibility Checker: &7Checking compatibility with your server...");

        // Using a List system in case more compatibility checks are added.
        final List<String> incompatibilities = new ArrayList<>();

        // Check the MC version of the server.
        if (!VersionUtils.isOneFourteen()) {
            incompatibilities.add("Your server version &8(&b" + Bukkit.getVersion() + "&8)&7 is unsupported by &bLevelledMobs v" + main.getDescription().getVersion() + "&7!" +
                    "Compatible MC versions: &b" + String.join(", ", Utils.getSupportedServerVersions()) + "&7.");
        }

        if (!ExternalCompatibilityManager.hasProtocolLibInstalled()) {
            incompatibilities.add("Your server does not have &bProtocolLib&7 installed! This means that no levelled nametags will appear on the mobs. If you wish to see custom nametags above levelled mobs, then you must install ProtocolLib.");
        }

        main.incompatibilitiesAmount = incompatibilities.size();
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
        FileLoader.saveResourceIfNotExists(main, new File(main.getDataFolder(), "license.txt"));

        // load configurations
        main.settingsCfg = FileLoader.loadFile(main, "settings", FileLoader.SETTINGS_FILE_VERSION, false);

        if (main.settingsCfg != null) // only load if settings were loaded successfully
            main.messagesCfg = FileLoader.loadFile(main, "messages", FileLoader.MESSAGES_FILE_VERSION, false);
        else {
            // had an issue reading the file.  Disable the plugin now
            return false;
        }

        final boolean customDropsEnabled = main.settingsCfg.getBoolean("use-custom-item-drops-for-mobs");

        main.customDropsCfg = FileLoader.loadFile(main, "customdrops", FileLoader.CUSTOMDROPS_FILE_VERSION, customDropsEnabled);

        main.customCommands = FileLoader.loadFile(main, "customCommands", FileLoader.CUSTOMCOMMANDS_FILE_VERSION, false);

        main.configUtils.entityTypesLevelOverride_Min = main.configUtils.getMapFromConfigSection("entitytype-level-override.min-level");
        main.configUtils.entityTypesLevelOverride_Max = main.configUtils.getMapFromConfigSection("entitytype-level-override.max-level");
        main.configUtils.worldLevelOverride_Min = main.configUtils.getMapFromConfigSection("world-level-override.min-level");
        main.configUtils.worldLevelOverride_Max = main.configUtils.getMapFromConfigSection("world-level-override.max-level");
        main.configUtils.noDropMultiplierEntities = main.configUtils.getSetFromConfigSection("no-drop-multipler-entities");

        main.attributesCfg = loadEmbeddedResource("defaultAttributes.yml");
        main.dropsCfg = loadEmbeddedResource("defaultDrops.yml");

        main.configUtils.load();
        ExternalCompatibilityManager.load(main);

        // remove legacy files if they exist
        final String[] legacyFile = {"attributes.yml", "drops.yml"};
        for (String lFile : legacyFile) {
            final File delFile = new File(main.getDataFolder(), lFile);
            try {
                if (delFile.exists()) delFile.delete();
            } catch (Exception e) {
                Utils.logger.warning("Unable to delete file " + lFile + ", " + e.getMessage());
            }
        }

        // build custom drops
        main.customDropsHandler = new CustomDropsHandler(main);

        return true;
    }

    @Nullable
    protected YamlConfiguration loadEmbeddedResource(final String filename) {
        YamlConfiguration result = null;
        final InputStream inputStream = main.getResource(filename);
        if (inputStream == null) return null;

        try {
            InputStreamReader reader = new InputStreamReader(inputStream);
            result = YamlConfiguration.loadConfiguration(reader);
            reader.close();
            inputStream.close();
        } catch (IOException e) {
            Utils.logger.error("Error reading embedded file: " + filename + ", " + e.getMessage());
        }

        return result;
    }

    protected void registerListeners() {
        Utils.logger.info("&fListeners: &7Registering event listeners...");

        main.levelManager = new LevelManager(main);
        main.levelManager.creatureSpawnListener = new CreatureSpawnListener(main); // we're saving this reference so the summon command has access to it
        main.entityDamageDebugListener = new EntityDamageDebugListener(main);

        if (main.settingsCfg.getBoolean("debug-entity-damage")) {
            // we'll load and unload this listener based on the above setting when reloading
            main.configUtils.debugEntityDamageWasEnabled = true;
            pluginManager.registerEvents(main.entityDamageDebugListener, main);
        }

        pluginManager.registerEvents(main.levelManager.creatureSpawnListener, main);
        pluginManager.registerEvents(new EntityDamageListener(main), main);
        pluginManager.registerEvents(new EntityDeathListener(main), main);
        pluginManager.registerEvents(new EntityRegainHealthListener(main), main);
        pluginManager.registerEvents(new PlayerJoinWorldNametagListener(main), main);
        pluginManager.registerEvents(new EntityTransformListener(main), main);
        pluginManager.registerEvents(new EntityNametagListener(main), main);
        pluginManager.registerEvents(new EntityTargetListener(main), main);
        pluginManager.registerEvents(new PlayerJoinListener(main), main);
        pluginManager.registerEvents(new EntityTameListener(main), main);
        main.chunkLoadListener = new ChunkLoadListener(main);

        if (ExternalCompatibilityManager.hasMythicMobsInstalled())
            pluginManager.registerEvents(new MythicMobsListener(main), main);

        if (main.settingsCfg.getBoolean("ensure-mobs-are-levelled-on-chunk-load"))
            pluginManager.registerEvents(main.chunkLoadListener, main);
    }

    protected void registerCommands() {
        Utils.logger.info("&fCommands: &7Registering commands...");

        final PluginCommand levelledMobsCommand = main.getCommand("levelledmobs");
        if (levelledMobsCommand == null) {
            Utils.logger.error("Command &b/levelledmobs&7 is unavailable, is it not registered in plugin.yml?");
        } else {
            levelledMobsCommand.setExecutor(new LevelledMobsCommand(main));
        }
    }

    protected void setupMetrics() {
        new Metrics(main, 6269);
    }

    //Check for updates on the Spigot page.
    protected void checkUpdates() {
        if (main.settingsCfg.getBoolean("use-update-checker")) {
            final UpdateChecker updateChecker = new UpdateChecker(main, 74304);
            updateChecker.getLatestVersion(latestVersion -> {
                if (!updateChecker.getCurrentVersion().split(" ")[0].equals(latestVersion)) {
                    Utils.logger.warning("&fUpdate Checker: &7The plugin has an update available! You're running &bv" + updateChecker.getCurrentVersion() + "&7, latest version is &bv" + latestVersion + "&7.");
                }
            });
        }
    }

    protected void shutDownAsyncTasks() {
        Utils.logger.info("&fTasks: &7Shutting down other async tasks...");
        Bukkit.getScheduler().cancelTasks(main);
    }
}
