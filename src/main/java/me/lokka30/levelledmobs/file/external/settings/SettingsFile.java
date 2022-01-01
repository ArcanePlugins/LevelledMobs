/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.file.external.settings;

import de.leonhard.storage.Yaml;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.file.external.YamlExternalVersionedFile;
import me.lokka30.levelledmobs.util.Utils;
import org.jetbrains.annotations.NotNull;

public class SettingsFile implements YamlExternalVersionedFile {

    private final @NotNull LevelledMobs main;
    private Yaml data;
    public SettingsFile(final @NotNull LevelledMobs main) {
        this.main = main;
    }

    @Override
    public void load(boolean fromReload) {
        // replace if not exists
        if(!exists(main)) { replace(main); }

        // reload data if method was called from a reload function
        // if not reload, then instantiate the yaml data object
        if(fromReload) {
            data.forceReload();
        } else {
            data = new Yaml(getNameWithoutExtension(), getFullPath(main));
        }

        // run the migrator
        migrate();
    }

    @Override
    public String getNameWithoutExtension() {
        return "settings";
    }

    @Override
    public String getRelativePath() {
        return getName();
    }

    @Override
    public int getSupportedFileVersion() {
        return 33;
    }

    @Override
    public void migrate() {
        switch(compareFileVersion()) {
            case CURRENT:
                return;
            case FUTURE:
                sendFutureFileVersionWarning(main);
                return;
            case OUTDATED:
                for(int i = getInstalledFileVersion(); i < getSupportedFileVersion(); i++) {
                    Utils.LOGGER.info("Attempting to migrate file '&b" + getName() + "&7' to version '&b" + i + "&7'...");

                    if(i < 32) {
                        // This file was present prior to LM 4 so we can't feasibly
                        // migrate versions other than the previous file version only.
                        Utils.LOGGER.error("Your '&b" + getName() + "&7' file is too old to be migrated. " +
                                "LevelledMobs will back this file up and instead load the default contents " +
                                "of the latest version one for you. You will need to manually modify the new file.");
                        backup(main);
                        replace(main);
                        break;
                    }

                    switch(i) {
                        case 32:
                            //TODO migrate from old version to new.
                            return;
                        case 33:
                            return;
                        default:
                            // this is reached if there is no migration logic for a specific version.
                            Utils.LOGGER.warning("Migration logic was not programmed for the file version '&b" + i + "&7' " +
                                    "of the file '&b" + getName() + "&7'! Please inform the LevelledMobs developers.");
                    }
                }
                Utils.LOGGER.info("Migration complete for file '&b" + getName() + "&7'.");
                return;
            default:
                throw new IllegalStateException(compareFileVersion().toString());
        }
    }

    @NotNull
    @Override
    public Yaml getData() {
        return data;
    }
}
