/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.misc;

import me.lokka30.microlib.messaging.MessageUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;

/**
 * Used to load various configuration files and migrate if necessary
 *
 * @author lokka30, stumper66
 * @since 2.4.0
 */
public final class FileLoader {

    public static final int SETTINGS_FILE_VERSION = 33;    // Last changed: 2021/12/19
    public static final int MESSAGES_FILE_VERSION = 8;     // Last changed: 2021/12/14
    public static final int CUSTOMDROPS_FILE_VERSION = 10; // Last changed: v3.1.0 b474
    public static final int RULES_FILE_VERSION = 2;        // Last changed: v3.2.0 b529

    private FileLoader() {
        throw new UnsupportedOperationException();
    }

    @Nullable
    public static YamlConfiguration loadFile(@NotNull final Plugin plugin, String cfgName, final int compatibleVersion) {
        cfgName = cfgName + ".yml";

        Utils.logger.info("&fFile Loader: &7Loading file '&b" + cfgName + "&7'...");

        final File file = new File(plugin.getDataFolder(), cfgName);

        saveResourceIfNotExists(plugin, file);
        try (final FileInputStream fs = new FileInputStream(file)) {
            new Yaml().load(fs);
        } catch (final Exception e) {
            final String parseErrorMessage = "Unable to parse file &b%s&r due to a user-caused YAML syntax error. Please copy the contents of this file into a Yaml Parser website (https://tinyurl.com/yamlp) to help diagnose which line you caused the error on. The parsing error is available below. It indicates line numbers around where the error occurred.\n" +
                    "&b---- START ERROR ----&r\n" +
                    "&4%s&r\n" +
                    "&b---- END ERROR ----&r\n" +
                    "If you have gone through unsuccessful efforts to fix this issue, you may contact our support team: &bhttps://discord.io/arcaneplugins";

            Utils.logger.error(String.format(parseErrorMessage, cfgName, e));
            return null;
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.options().copyDefaults(true);
        final YmlParsingHelper ymlHelper = new YmlParsingHelper();

        final int fileVersion = ymlHelper.getInt(cfg,"file-version");
        final boolean isCustomDrops = cfgName.equals("customdrops.yml");
        final boolean isRules = cfgName.equals("rules.yml");

        if (fileVersion < compatibleVersion) {
            final File backedupFile = new File(plugin.getDataFolder(), cfgName + ".v" + fileVersion + ".old");

            // copy to old file
            FileUtil.copy(file, backedupFile);
            Utils.logger.info("&fFile Loader: &8(Migration) &b" + cfgName + " backed up to " + backedupFile.getName());
            // overwrite the file from new version
            if (!isRules)
                plugin.saveResource(file.getName(), true);

            // copy supported values from old file to new
            Utils.logger.info("&fFile Loader: &8(Migration) &7Migrating &b" + cfgName + "&7 from old version to new version.");

            if (isCustomDrops)
                FileMigrator.copyCustomDrops(backedupFile, file, fileVersion);
            else if (!isRules)
                FileMigrator.copyYmlValues(backedupFile, file, fileVersion);
            else
                FileMigrator.migrateRules(file);

            // reload cfg from the updated values
            cfg = YamlConfiguration.loadConfiguration(file);

        } else if (!isRules)
            checkFileVersion(file, compatibleVersion, ymlHelper.getInt(cfg, "file-version"));

        return cfg;
    }

    public static @NotNull String getFileLoadErrorMessage(){
        return MessageUtils.colorizeStandardCodes(
                "&4An error occured&r whilst attempting to parse the file &brules.yml&r due to a user-caused YAML syntax error. Please see the console logs for more details."
        );
    }

    public static void saveResourceIfNotExists(final Plugin instance, @NotNull final File file) {
        if (!file.exists()) {
            Utils.logger.info("&fFile Loader: &7File '&b" + file.getName() + "&7' doesn't exist, creating it now...");
            instance.saveResource(file.getName(), false);
        }
    }

    private static void checkFileVersion(final File file, final int compatibleVersion, final int installedVersion) {
        if (compatibleVersion == installedVersion)
            return;

        final String what = installedVersion < compatibleVersion ?
                "outdated" : "ahead of the compatible version of this file for this version of the plugin";

        Utils.logger.error("&fFile Loader: &7The version of &b" + file.getName() + "&7 you have installed is " + what + "! Fix this as soon as possible, else the plugin will most likely malfunction.");
        Utils.logger.error("&fFile Loader: &8(&7You have &bv" + installedVersion + "&7 installed but you are meant to be running &bv" + compatibleVersion + "&8)");
    }
}
