/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.file.external.misc.license;

import me.lokka30.levelledmobs.file.external.ExternalFile;

import java.io.File;

public class LicenseFile implements ExternalFile {

    @Override
    public void load(boolean fromReload) {
        replace();
    }

    @Override
    public String getName() {
        return getNameWithoutExtension() + ".txt";
    }

    @Override
    public String getNameWithoutExtension() {
        return "license";
    }

    @Override
    public String getRelativePath() {
        return "misc" + File.separator + getName();
    }
}
