/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.file.external;

import de.leonhard.storage.Yaml;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.util.Utils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public interface YamlExternalFile extends ExternalFile {

    @NotNull Yaml getData();

    void migrate();

    default void backup(final @NotNull LevelledMobs main) {
        final File from = new File(getFullPath(main));
        final File backupDirectory = new File(main.getDataFolder() + File.separator + "backups");
        final File to = new File(
                backupDirectory.getPath()
                        + File.separator
                        + System.nanoTime()
                        + File.separator + getName()
        );

        try {
            Files.copy(from.toPath(), to.toPath());
        } catch (IOException ex) {
            Utils.LOGGER.error("Unable to back up file '&b" + getName() + "&7': &f" + ex.getMessage());
        }
    }

}
