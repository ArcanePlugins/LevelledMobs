/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.plugin.old.file.internal;

import java.io.InputStream;
import me.lokka30.levelledmobs.plugin.old.LevelledMobs;

public interface InternalFile {

    void load(final boolean fromReload);

    default InputStream getInputStream() {
        return LevelledMobs.getInstance().getResource(getRelativePath());
    }

    String getName();

    String getNameWithoutExtension();

    String getRelativePath();

}