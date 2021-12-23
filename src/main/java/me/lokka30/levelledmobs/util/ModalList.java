/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.util;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public record ModalList<T>(
        @NotNull ListMode listMode,
        @NotNull Collection<T> contents
) {

    public enum ListMode {
        INCLUSIVE,
        EXCLUSIVE
    }

    public boolean contains(@NotNull final T item) {
        return switch (listMode) {
            case INCLUSIVE -> contents.contains(item);
            case EXCLUSIVE -> !contents.contains(item);
        };
    }

}
