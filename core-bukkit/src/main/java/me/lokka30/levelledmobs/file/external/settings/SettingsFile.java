/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.file.external.settings;

import de.leonhard.storage.LightningBuilder;
import de.leonhard.storage.Yaml;
import de.leonhard.storage.internal.settings.ConfigSettings;
import de.leonhard.storage.internal.settings.DataType;
import de.leonhard.storage.internal.settings.ReloadSettings;
import me.lokka30.levelledmobs.file.external.YamlExternalVersionedFile;
import me.lokka30.levelledmobs.util.Utils;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class SettingsFile implements YamlExternalVersionedFile {

    private Yaml data;

    @Override
    public void load(boolean fromReload) {
        // replace if not exists
        if(!exists()) { replace(); }

        // load the data
        if(fromReload) {
            getData().forceReload();
        } else {
            data = LightningBuilder
                    .fromFile(new File(getFullPath()))
                    .setReloadSettings(ReloadSettings.MANUALLY)
                    .setConfigSettings(ConfigSettings.PRESERVE_COMMENTS)
                    .setDataType(DataType.SORTED)
                    .createYaml();
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
                sendFutureFileVersionWarning();
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
                        backup();
                        replace();
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
