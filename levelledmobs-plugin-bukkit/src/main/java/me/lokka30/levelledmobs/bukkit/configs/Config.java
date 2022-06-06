package me.lokka30.levelledmobs.bukkit.configs;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import me.lokka30.levelledmobs.bukkit.LevelledMobs;
import me.lokka30.levelledmobs.bukkit.utils.Log;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

public abstract class Config {

    /* vars */

    private final String fileName;
    private final int latestFileVersion;
    private YamlConfigurationLoader loader = null;
    private CommentedConfigurationNode root = null;

    /* constructors */

    public Config(
        final @NotNull String fileName,
        final int latestFileVersion
    ) {
        this.fileName = fileName;
        this.latestFileVersion = latestFileVersion;
    }

    /* methods */

    public boolean load() {
        Log.inf("Loading file '" + getFileName() + "'");

        saveDefaultFile(false);

        if(loader == null) {
            loader = YamlConfigurationLoader.builder().path(getAbsolutePath()).build();
        }

        try {
            root = getLoader().load();
        } catch(ConfigurateException ex) {
            Log.sev(
                "Unable to load configuration '" + getFileName() + "'. This is usually a " +
                    "user-caused error caused from YAML syntax errors inside the file, such as an " +
                    "incorrect indent or stray symbol. We recommend that you use a YAML parser " +
                    "website - such as the one linked here - to help locate where these errors " +
                    "are appearing. --> https://www.yaml-online-parser.appspot.com/ <-- A stack " +
                    "trace will be printed below for debugging purposes.", true
            );
            ex.printStackTrace();
            return false;
        }

        return update();
    }

    public boolean save() {
        try {
            getLoader().save(getRoot());
            return true;
        } catch(ConfigurateException ex) {
            Log.sev("LevelledMobs was unable to save data to the configuration '" +
                getFileName() + "'. A stack trace will be printed below for debugging purposes.",
                true);
            ex.printStackTrace();
            return false;
        }
    }

    public boolean update() {
        final var currentFileVersion = getCurrentFileVersion();

        if(currentFileVersion == 0) {
            Log.sev("Unable to detect the file version of configuration '" + getFileName() +
                "'. Was the file metadata modified?", true);
            return false;
        } else if(currentFileVersion > latestFileVersion) {
            Log.war("Configuration '" + getFileName() + "' is somehow newer than the latest " +
                "compatible file version. Was it modified by the user?", false);
            return true;
        } else if(currentFileVersion < latestFileVersion) {
            Log.inf("Update detected for configuration '" + getFileName() + "'; updating");
            if(updateLogic(currentFileVersion)) {
                Log.inf("Configuration '" + getFileName() + "' has been updated");
                return true;
            } else {
                Log.sev("Update for configuration '" + getFileName() + "' failed",
                    true);
                return false;
            }
        } else {
            return true;
        }
    }

    /*
    Run abstracted update logic handled per-file.
    Returns if the file update was successful (no exception occurred).
    This method should only be called by the `update` method.
     */
    protected abstract boolean updateLogic(final int fromVersion);

    public int getCurrentFileVersion() {
        final var fileVersionLm4 = Math.max(0,
            getRoot().node("metadata", "version", "current").getInt(0));

        final var fileVersionLm3 = Math.max(0,
            getRoot().node("file-version").getInt(0));

        if(fileVersionLm4 == 0) {
            /*
            Either the user has tampered with the file, or they are migrating from an old LM
            revision. Let's check the file version at the classic LM 1/2/3 node. If even that is
            missing, then it'll default to `0` which will send a severe warning to the console.

            Using the `max` method since we don't want numbers lower than 0.
             */
            if(fileVersionLm3 == 0) {
                Log.sev("Unable to retrieve current file version of config '" + getFileName() +
                    "'. Was it removed or modified by the user?", true);
                return 0;
            } else {
                Log.inf("LM4-style file version not found for config '" + getFileName() +
                    "'; falling back to the LM3-style file version until the file is updated");
                return fileVersionLm3;
            }
        } else {
            return fileVersionLm4;
        }
    }

    public void saveDefaultFile(final boolean replaceExistingFile) {
        if(!replaceExistingFile && getAbsolutePath().toFile().exists())
            return;

        Log.inf("Saving default file of configuration '" + getFileName() + "'");
        LevelledMobs.getInstance().saveResource(getFileName(), replaceExistingFile);
    }

    @NotNull
    public Path getAbsolutePath() {
        return Path.of(LevelledMobs.getInstance()
            .getDataFolder().getAbsolutePath() + File.separator + getFileName());
    }

    /* getters and setters */

    @NotNull
    public String getFileName() {
        return fileName;
    }

    public int getLatestFileVersion() {
        return latestFileVersion;
    }

    @NotNull
    protected YamlConfigurationLoader getLoader() {
        return Objects.requireNonNull(loader, "loader");
    }

    @NotNull
    public CommentedConfigurationNode getRoot() {
        return Objects.requireNonNull(root, "root");
    }

}
