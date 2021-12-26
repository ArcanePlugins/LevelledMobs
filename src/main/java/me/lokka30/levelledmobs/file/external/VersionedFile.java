/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.file.external;

import org.jetbrains.annotations.NotNull;

public interface VersionedFile {

    int getInstalledFileVersion();

    int getLatestFileVersion();

    @NotNull default Result compareFileVersion() {
        if(getInstalledFileVersion() > getLatestFileVersion()) {
            return Result.FUTURE;
        } else if(getInstalledFileVersion() < getLatestFileVersion()) {
            return Result.OUTDATED;
        } else {
            return Result.CURRENT;
        }
    }

    enum Result {
        FUTURE,
        CURRENT,
        OUTDATED
    }

}
