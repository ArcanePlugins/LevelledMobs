/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.file.external;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.util.Utils;
import org.jetbrains.annotations.NotNull;

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
    default String getFullPath(final @NotNull LevelledMobs main) {
        return main.getDataFolder() + File.separator + getRelativePath();
    }

    // replace the existing file or create a new file, contents transferred from the default file in the jar
    default void replace(final @NotNull LevelledMobs main) {
        main.saveResource(getRelativePath(), true);
        if(getRelativePath().contains(File.separator)) {
            try {
                Files.copy(
                        Path.of(getName()),
                        Path.of(getRelativePath())
                );
            } catch (IOException ex) {
                Utils.LOGGER.error("Unable to move file '&b" + getName() + "&7': &f" + ex.getMessage());
            }
        }
    }

    // whether the file exists or not in the data folder.
    default boolean exists(final @NotNull LevelledMobs main) {
        return new File(getFullPath(main)).exists();
    }

}
