/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.misc;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author stumper66
 * @since 3.1.0
 */
public class NBTApplyResult {
    @Nullable
    public ItemStack itemStack;
    public String exceptionMessage;
    @Nullable
    public List<String> objectsAdded;
    @Nullable
    public List<String> objectsUpdated;
    @Nullable
    public List<String> objectsRemoved;

    public boolean hadException() {
        return this.exceptionMessage != null;
    }
}
