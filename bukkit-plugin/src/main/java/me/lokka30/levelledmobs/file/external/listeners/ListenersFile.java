/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.file.external.listeners;

import de.leonhard.storage.LightningBuilder;
import de.leonhard.storage.Yaml;
import de.leonhard.storage.internal.settings.ConfigSettings;
import de.leonhard.storage.internal.settings.DataType;
import de.leonhard.storage.internal.settings.ReloadSettings;
import me.lokka30.levelledmobs.file.external.YamlExternalVersionedFile;
import me.lokka30.levelledmobs.util.Utils;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class ListenersFile implements YamlExternalVersionedFile {

    private Yaml data;

    @Override
    public void load(boolean fromReload) {
        // replace if not exists
        if (!exists()) {
            replace();
        }

        // load the data
        if (fromReload) {
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
        return "listeners";
    }

    @Override
    public String getRelativePath() {
        return getName();
    }

    @Override
    public int getSupportedFileVersion() {
        return 1;
    }

    @Override
    public void migrate() {
        switch (compareFileVersion()) {
            case CURRENT:
                return;
            case FUTURE:
                sendFutureFileVersionWarning();
                return;
            case OUTDATED:
                boolean stop = false;
                for (int i = getInstalledFileVersion(); i < getSupportedFileVersion(); i++) {
                    if (stop) {
                        break;
                    }
                    Utils.LOGGER.info(
                        "Attempting to migrate file '&b" + getName() + "&7' to version '&b" + i
                            + "&7'...");
                    switch (i) {
                        case 1:
                            return;
                        default:
                            // this is reached if there is no migration logic for a specific version.
                            Utils.LOGGER.warning(
                                "Migration logic was not programmed for the file version '&b" + i
                                    + "&7' " +
                                    "of the file '&b" + getName()
                                    + "&7'! Please inform the LevelledMobs developers.");
                            stop = true;
                            break;
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
