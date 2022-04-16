/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.plugin.old.event;

import com.google.common.collect.ImmutableList;
import java.util.List;
import me.lokka30.levelledmobs.plugin.old.level.LevelledMob;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MobDropModificationEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    private boolean isCancelled = false;

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean isCancelled) {
        this.isCancelled = isCancelled;
    }

    private final LevelledMob levelledMob;
    private final ImmutableList<ItemStack> previousDrops;
    private final List<ItemStack> newDrops;

    public MobDropModificationEvent(
        final @NotNull LevelledMob levelledMob,
        final @NotNull ImmutableList<ItemStack> previousDrops,
        final @NotNull List<ItemStack> newDrops
    ) {
        this.levelledMob = levelledMob;
        this.previousDrops = previousDrops;
        this.newDrops = newDrops;
    }

    public LevelledMob getLevelledMob() {
        return levelledMob;
    }

    public ImmutableList<ItemStack> getPreviousDrops() {
        return previousDrops;
    }

    public List<ItemStack> getNewDrops() {
        return newDrops;
    }
}
