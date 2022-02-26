/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.file.external.readme;

import me.lokka30.levelledmobs.file.external.ExternalFile;

public class ReadmeFile implements ExternalFile {

    @Override
    public void load(final boolean fromReload) {
        replace();
    }

    @Override
    public String getNameWithoutExtension() {
        return "README";
    }

    @Override
    public String getName() {
        return getNameWithoutExtension() + ".txt";
    }

    @Override
    public String getRelativePath() {
        return getName();
    }

}
