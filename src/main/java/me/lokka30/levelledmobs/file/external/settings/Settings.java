/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.file.external.settings;

import de.leonhard.storage.Yaml;
import me.lokka30.levelledmobs.file.external.VersionedFile;
import me.lokka30.levelledmobs.file.external.YamlExternalFile;
import org.jetbrains.annotations.NotNull;

public class Settings implements YamlExternalFile, VersionedFile {

    private Yaml data;

    @Override
    public void load() {

    }

    @Override
    public int getInstalledFileVersion() {
        return getData().getOrDefault("file-data-do-not-edit.version", -1);
    }

    @Override
    public int getLatestFileVersion() {
        return 33;
    }

    @Override
    public void migrate() {

    }

    @NotNull
    @Override
    public Yaml getData() {
        return null;
    }
}
