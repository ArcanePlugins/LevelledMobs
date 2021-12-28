/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.file.external;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.util.Utils;
import org.jetbrains.annotations.NotNull;

public interface VersionedFile extends ExternalFile {

    int getInstalledFileVersion();

    int getSupportedFileVersion();

    @NotNull
    default VersionedFile.FileVersionComparisonResult compareFileVersion() {
        if(getInstalledFileVersion() > getSupportedFileVersion()) {
            return FileVersionComparisonResult.FUTURE;
        } else if(getInstalledFileVersion() < getSupportedFileVersion()) {
            return FileVersionComparisonResult.OUTDATED;
        } else {
            return FileVersionComparisonResult.CURRENT;
        }
    }

    enum FileVersionComparisonResult {
        FUTURE,
        CURRENT,
        OUTDATED
    }

    default void sendFutureFileVersionWarning(final @NotNull LevelledMobs main) {
        Utils.LOGGER.warning(
                "Your '&b" + getName() + "&7' file is running a version &onewer&7 " +
                        "than what is supported by this version of LevelledMobs. " +
                        "Please update LevelledMobs, unless this is a development error.");
        Utils.LOGGER.warning("&8 -> &7Installed file version: &b" + getInstalledFileVersion());
        Utils.LOGGER.warning("&8 -> &7Supported file version: &b" + getSupportedFileVersion());
        Utils.LOGGER.warning("&8 -> &7Plugin version: &b" + main.getDescription().getVersion());
    }

}
