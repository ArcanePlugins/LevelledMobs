package io.github.lokka30.levelledmobs.utils;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.FileUtil;

import java.io.File;

public final class FileLoader {

    public static final int SETTINGS_FILE_VERSION = 24; // Last changed: 2.1.0
    public static final int MESSAGES_FILE_VERSION = 1; // Last changed: 2.1.0

    private FileLoader() {
        throw new UnsupportedOperationException();
    }

    public static YamlConfiguration loadFile(final Plugin plugin, String cfgName, final int fileVersion, boolean doMigrate) {
        cfgName = cfgName + ".yml";

        Utils.logger.info("&fFile Loader: &7Loading file '&b" + cfgName + "&7'...");

        final File file = new File(plugin.getDataFolder(), cfgName);

        saveResourceIfNotExists(plugin, file);

        final YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.options().copyDefaults(true);

        if (doMigrate){
            File backedupFile = new File(plugin.getDataFolder(), "settings.old");
            // copy settings.yml to settings.old
            FileUtil.copy(file, backedupFile);
            // overwrite settings.yml from new version
            plugin.saveResource(file.getName(), true);

            // TODO: work in progress
            //copyYmlValues(backedupFile, file, false, fileVersion);

        } else{
            checkFileVersion(file, fileVersion, cfg.getInt("file-version"));
        }

        return cfg;
    }

    public static void saveResourceIfNotExists(final Plugin instance, final File file) {
        if (!file.exists()) {
            Utils.logger.info("&fFile Loader: &7Configuration file '&b" + file.getName() + "&7' doesn't exist, creating it now...");
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
}
