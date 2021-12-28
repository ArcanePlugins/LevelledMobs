/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.file.external;

public interface YamlExternalVersionedFile extends YamlExternalFile, VersionedFile {

    @Override
    default int getInstalledFileVersion() {
        return getData().getOrDefault("file-data-do-not-edit.version", -1);
    }

}
