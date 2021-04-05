package me.lokka30.levelledmobs.misc;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.FileUtil;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;

/**
 * @author lokka30
 * @contributors stumper66
 */
public final class FileLoader {

    public static final int SETTINGS_FILE_VERSION = 28; // Last changed: b289
    public static final int MESSAGES_FILE_VERSION = 2; // Last changed: v2.3.0
    public static final int CUSTOMDROPS_FILE_VERSION = 7; // Last changed: 2.4.0 b328

    private FileLoader() {
        throw new UnsupportedOperationException();
    }

    public static YamlConfiguration loadFile(final Plugin plugin, String cfgName, final int compatibleVersion, final boolean customDropsEnabled) {
        cfgName = cfgName + ".yml";

        Utils.logger.info("&fFile Loader: &7Loading file '&b" + cfgName + "&7'...");

        final File file = new File(plugin.getDataFolder(), cfgName);

        saveResourceIfNotExists(plugin, file);
        try (FileInputStream fs = new FileInputStream(file)) {
            new Yaml().load(fs);
        } catch (Exception e) {
            Utils.logger.error("&4Error reading " + cfgName + ". " + e.getMessage());
            return null;
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.options().copyDefaults(true);

        final int fileVersion = cfg.getInt("file-version");
        final boolean isCustomDrops = cfgName.equals("customdrops.yml");

        MigrateBehavior migrateBehavior = MigrateBehavior.MIGRATE;

        if (fileVersion < compatibleVersion && (migrateBehavior == MigrateBehavior.MIGRATE || migrateBehavior == MigrateBehavior.RESET)) {
            final File backedupFile = new File(plugin.getDataFolder(), cfgName + ".v" + fileVersion + ".old");

            // copy to old file
            FileUtil.copy(file, backedupFile);
            Utils.logger.info("&fFile Loader: &8(Migration) &b" + cfgName + " backed up to " + backedupFile.getName());
            // overwrite settings.yml from new version
            plugin.saveResource(file.getName(), true);

            if (migrateBehavior == MigrateBehavior.MIGRATE) {
                // copy supported values from old file to new
                Utils.logger.info("&fFile Loader: &8(Migration) &7Migrating &b" + cfgName + "&7 from old version to new version.");
                if (isCustomDrops)
                    FileMigrator.copyCustomDrops(backedupFile, file, fileVersion, customDropsEnabled);
                else
                    FileMigrator.copyYmlValues(backedupFile, file, fileVersion);
            }

            // reload cfg from the updated values
            cfg = YamlConfiguration.loadConfiguration(file);

            if (migrateBehavior == MigrateBehavior.RESET) {
                Utils.logger.warning("&fFile Loader: &8(Migration) &b" + cfgName + "&7 has been reset to default values.");
            }

        } else {
            checkFileVersion(file, compatibleVersion, cfg.getInt("file-version"));
        }

        return cfg;
    }

    public static void saveResourceIfNotExists(final Plugin instance, final File file) {
        if (!file.exists()) {
            Utils.logger.info("&fFile Loader: &7File '&b" + file.getName() + "&7' doesn't exist, creating it now...");
            instance.saveResource(file.getName(), false);
        }
    }

    private static void checkFileVersion(final File file, final int compatibleVersion, final int installedVersion) {
        if (compatibleVersion == installedVersion) {
            return;
        }

        String what;
        if (installedVersion < compatibleVersion) {
            what = "outdated";
        } else {
            what = "ahead of the compatible version of this file for this version of the plugin";
        }

        Utils.logger.error("&fFile Loader: &7The version of &b" + file.getName() + "&7 you have installed is " + what + "! Fix this as soon as possible, else the plugin will most likely malfunction.");
        Utils.logger.error("&fFile Loader: &8(&7You have &bv" + installedVersion + "&7 installed but you are meant to be running &bv" + compatibleVersion + "&8)");
    }

    /**
     * @author stumper66
     */
    public enum MigrateBehavior {
        IGNORE,
        MIGRATE,
        RESET
    }
}
