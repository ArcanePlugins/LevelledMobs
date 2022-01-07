/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.file.internal;

import me.lokka30.levelledmobs.LevelledMobs;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;

public interface InternalFile {

    void load(final boolean fromReload);

    default InputStream getInputStream(final @NotNull LevelledMobs main) {
        return main.getResource(getRelativePath());
    }

    String getName();

    String getNameWithoutExtension();

    String getRelativePath() ;

}