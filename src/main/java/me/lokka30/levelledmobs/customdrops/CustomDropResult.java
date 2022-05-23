/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.customdrops;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Used internally to determine if the mob's
 * vanilla items should be removed or not
 *
 * @author stumper66
 * @since 2.6.0
 */
public class CustomDropResult {
    public CustomDropResult(final @NotNull List<Map.Entry<ItemStack, CustomDropItem>> stackToItem, final boolean hasOverride){
        this.stackToItem = stackToItem;
        this.hasOverride = hasOverride;
    }
    public final boolean hasOverride;
    public final List<Map.Entry<ItemStack, CustomDropItem>> stackToItem;
}
