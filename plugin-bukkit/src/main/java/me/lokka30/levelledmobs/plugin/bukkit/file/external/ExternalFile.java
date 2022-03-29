/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.plugin.bukkit.file.external;

import me.lokka30.levelledmobs.plugin.bukkit.LevelledMobs;
import me.lokka30.levelledmobs.plugin.bukkit.util.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public interface ExternalFile {

    void load(boolean isReload);

    // e.g. `"customdrops.yml"`
    String getName();

    // e.g. '.yml'
    String getNameWithoutExtension();

    // Full path, excluding the data folder.
    // e.g. for constants.yml: `translations\constants.yml`
    String getRelativePath();

    // Full path of the file
    default String getFullPath() {
        return LevelledMobs.getInstance().getDataFolder() + File.separator + getRelativePath();
    }

    // replace the existing file or create a new file, contents transferred from the default file in the jar
    default void replace() {
        LevelledMobs.getInstance().saveResource(getRelativePath(), true);
        if (getRelativePath().contains(File.separator)) {
            try {
                Files.copy(
                    Path.of(getName()),
                    Path.of(getRelativePath())
                );
            } catch (IOException ex) {
                Utils.LOGGER.severe(
                    "Unable to move file '&b" + getName() + "&7': &f" + ex.getMessage());
            }
        }
    }

    // whether the file exists or not in the data folder.
    default boolean exists() {
        return new File(getFullPath()).exists();
    }

}
