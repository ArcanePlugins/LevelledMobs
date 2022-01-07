/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.file.internal.playerHeadTextures;

import de.leonhard.storage.Json;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.file.internal.JsonInternalFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class PlayerHeadTexturesFile implements JsonInternalFile {

    private final @NotNull LevelledMobs main;
    private Json data;
    public PlayerHeadTexturesFile(final @NotNull LevelledMobs main) {
        this.main = main;
    }

    @Override
    public void load(final boolean fromReload) {
        if(fromReload) return;

        data = new Json(getNameWithoutExtension(), getRelativePath(), getInputStream(main));
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
