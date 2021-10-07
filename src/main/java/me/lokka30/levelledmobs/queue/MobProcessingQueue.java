/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.queue;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author lokka30, stumper66
 * @since v4.0.0
 * This Queue handles mob processing (after they spawn in, they get levelled).
 * @see Queue
 */
public class MobProcessingQueue implements Queue {

    private final LevelledMobs main;
    public MobProcessingQueue(final LevelledMobs main) { this.main = main; }

    private boolean isRunning = false;
    private boolean isCancelled = false;

    public final ConcurrentLinkedQueue<EntitySpawnEvent> queue = new ConcurrentLinkedQueue<>();

    @Override
    public String getName() { return getClass().getName(); }

    @Override
    public void start() {
        Utils.LOGGER.info("Starting queue '&b" + getName() + "&7'...");

        if(isRunning) throw new UnsupportedOperationException("Queue is already running");

        isRunning = true;
        isCancelled = false;

        new BukkitRunnable() {
            @Override
            public void run() {
                while(!isCancelled) {
                    final EntitySpawnEvent item = queue.poll();

                    if(item == null) continue;

                    //TODO process mob. old code:
                    // main.levelManager.entitySpawnListener.preprocessMob(item.lmEntity, item.event);
                }

                this.cancel();
                isRunning = false;
            }
        }.runTaskAsynchronously(main);
    }

    @Override
    public void stop() {
        Utils.LOGGER.info("Stopping queue '&b" + getName() + "&7'...");

        if(!isRunning) throw new UnsupportedOperationException("Queue is not running");

        isCancelled = true;
        queue.clear();
    }

    @Override
    public void addItem(Object item) {
        queue.offer((EntitySpawnEvent) item);
    }
}
