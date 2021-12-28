/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.file.internal.playerHeadTextures;

import de.leonhard.storage.Json;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.file.internal.JsonInternalFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class PlayerHeadTextures implements JsonInternalFile {

    private final @NotNull LevelledMobs main;
    private final @NotNull Json data;
    public PlayerHeadTextures(final @NotNull LevelledMobs main) {
        this.main = main;
        this.data = new Json(getNameWithoutExtension(), getRelativePath(), getInputStream(main));
    }

    @NotNull
    @Override
    public Json getData() {
        return data;
    }

    @Override
    public String getNameWithoutExtension() {
        return "player-head-textures";
    }

    @Override
    public String getRelativePath() {
        return "internal" + File.separator + getName();
    }
}
