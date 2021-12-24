/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.file.external.misc.license;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.file.external.ExternalFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class License implements ExternalFile {

    private @NotNull final LevelledMobs main;
    public License(@NotNull final LevelledMobs main) {
        this.main = main;
    }

    @Override
    public void load() {
        main.saveResource("external" + File.separator + "license.txt", true);
    }

}
