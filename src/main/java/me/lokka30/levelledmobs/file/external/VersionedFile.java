/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.file.external;

import org.jetbrains.annotations.NotNull;

public interface VersionedFile {

    int getInstalledFileVersion();

    int getSupportedFileVersion();

    @NotNull default VersionedFile.FileVersionComparisonResult compareFileVersion() {
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

}
