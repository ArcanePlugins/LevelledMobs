/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.file.external;

import me.lokka30.levelledmobs.LevelledMobs;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public interface ExternalFile {

    void load(boolean isReload);

    // e.g. `"customdrops.yml"`
    String getName();

    // Full path, excluding the data folder.
    // e.g. for constants.yml: `translations\constants.yml`
    String getRelativePath();

    // Full path of the file
    default String getFullPath(final @NotNull LevelledMobs main) {
        return main.getDataFolder() + File.separator + getRelativePath();
    }

    default void replace(final @NotNull LevelledMobs main) {
        main.saveResource(getRelativePath() + File.separator + getName(), true);
    }

}
