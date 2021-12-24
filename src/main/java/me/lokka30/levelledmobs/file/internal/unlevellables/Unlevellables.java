/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.file.internal.unlevellables;

import de.leonhard.storage.Json;
import me.lokka30.levelledmobs.file.internal.JsonInternalFile;
import org.jetbrains.annotations.NotNull;

public class Unlevellables implements JsonInternalFile {

    @Override
    public void load() {

    }

    @NotNull
    @Override
    public Json getData() {
        return null;
    }
}
