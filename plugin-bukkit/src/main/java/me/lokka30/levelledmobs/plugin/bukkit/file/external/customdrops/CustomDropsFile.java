/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.plugin.bukkit.file.external.customdrops;

import de.leonhard.storage.LightningBuilder;
import de.leonhard.storage.Yaml;
import de.leonhard.storage.internal.settings.ConfigSettings;
import de.leonhard.storage.internal.settings.DataType;
import de.leonhard.storage.internal.settings.ReloadSettings;
import java.io.File;
import me.lokka30.levelledmobs.plugin.bukkit.LevelledMobs;
import me.lokka30.levelledmobs.plugin.bukkit.file.external.YamlExternalVersionedFile;
import org.jetbrains.annotations.NotNull;

public class CustomDropsFile implements YamlExternalVersionedFile {

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
        return "customdrops";
    }

    @Override
    public String getRelativePath() {
        return getName();
    }

    @Override
    public int getSupportedFileVersion() {
        return 11;
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
                    LevelledMobs.logger().info(
                        "Attempting to migrate file '&b" + getName() + "&7' to version '&b" + i
                            + "&7'...");
                    if (i < 11) {
                        // This file was present prior to LM 4 so we can't feasibly
                        // migrate versions other than the previous file version only.
                        LevelledMobs.logger().severe(
                            "Your '&b" + getName() + "&7' file is too old to be migrated. " +
                                "LevelledMobs will back this file up and instead load the default contents "
                                +
                                "of the latest version one for you. You will need to manually modify the new file.");
                        backup();
                        replace();
                        stop = true;
                        break;
                    }
                    switch (i) {
                        case 11:
                            break;
                        default:
                            // this is reached if there is no migration logic for a specific version.

                            LevelledMobs.logger().warning(
                                "Migration logic was not programmed for the file version " +
                                    "'&b" + i + "&7' of the file '&b" + getName()
                                    + "&7'! Please inform the " +
                                    "LevelledMobs team.");
                            stop = true;
                            break;
                    }

                }
                LevelledMobs.logger().info("Migration complete for file '&b" + getName() + "&7'.");
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
