package me.lokka30.levelledmobs.bukkit.config.customdrops;

import me.lokka30.levelledmobs.bukkit.config.Config;
import me.lokka30.levelledmobs.bukkit.util.Log;

public final class CustomDropsCfg extends Config {

    /* vars */

    /*
    The latest file version of 'customdrops.yml' in the previous LevelledMobs revision (LM3).
     */
    private static final int LATEST_LM_3_CUSTOM_DROPS_FILE_VERSION = 10;

    /*
    The minimum file version which the updater is willing to migrate from.
     */
    private static final int UPDATER_CUTOFF_FILE_VERSION = LATEST_LM_3_CUSTOM_DROPS_FILE_VERSION;


    /* constructors */

    public CustomDropsCfg() {
        super("customdrops.yml", 11);
    }

    /* methods */

    @Override
    protected boolean updateLogic(int fromVersion) {
        if(fromVersion < UPDATER_CUTOFF_FILE_VERSION) {
            Log.sev("Configuration '" + getFileName() + "' is too old to be migrated: it " +
                "is version '" + fromVersion + "', but the 'cutoff' version is '" +
                UPDATER_CUTOFF_FILE_VERSION + "'", true);
            return false;
        }

        @SuppressWarnings("UnnecessaryLocalVariable")
        var currentFileVersion = fromVersion;

        //noinspection LoopStatementThatDoesntLoop
        while(currentFileVersion < getLatestFileVersion()) {
            Log.inf("Updating configuration '" + getFileName() + "' from file version '" +
                currentFileVersion + "' to '" + (currentFileVersion + 1) + "'");

            switch(currentFileVersion) {
                case LATEST_LM_3_CUSTOM_DROPS_FILE_VERSION -> {
                    //TODO some migration code  here ...
                    /*
                    currentFileVersion++
                    root node("metadata", "version", "current").set(currentFileVersion)
                    loader save(root)
                     */
                    Log.sev("Update logic is not yet available for LM3 Custom Drops", true);
                    return false;
                }
                default -> {
                    Log.sev("Attempted to update from file version '" + currentFileVersion +
                        "' of configuration '" + getFileName() + "', but no updater logic is " +
                        "present for that file version", true);
                    return false;
                }
            }
        }
        return true;
    }
}
