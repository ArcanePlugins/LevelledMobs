/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.file.internal;

import me.lokka30.levelledmobs.LevelledMobs;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;

public interface InternalFile {

    default InputStream getInputStream(final @NotNull LevelledMobs main) {
        return main.getResource(getRelativePath());
    }

    String getName();

    String getNameWithoutExtension();

    String getRelativePath() ;

}