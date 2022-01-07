/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.queue;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.levelling.LevelledMob;
import me.lokka30.levelledmobs.util.Utils;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author lokka30, stumper66
 * @since v4.0.0
 * This Queue contains nametags that will be sent
 * to various players.
 * @see Queue
 */
public class NametagProcessingQueue implements Queue {

    private final LevelledMobs main;
    public NametagProcessingQueue(final LevelledMobs main) { this.main = main; }

    private boolean isRunning = false;
    private boolean isCancelled = false;

    public final ConcurrentLinkedQueue<NametagUpdateQueueItem> queue = new ConcurrentLinkedQueue<>();

    @Override
    public String getName() { return getClass().getName(); }

    @Override
    public void start() {
        Utils.LOGGER.info("Starting queue '&b" + getName() + "&7'...");

        if(isRunning) throw new UnsupportedOperationException("Queue is already running.");

        isRunning = true;
        isCancelled = false;

        new BukkitRunnable() {
            @Override
            public void run() {
                while(!isCancelled) {
                    final NametagUpdateQueueItem item = queue.poll();

                    if(item == null) continue;

                    //TODO process nametag.
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
        queue.offer((NametagUpdateQueueItem) item);
    }

    /**
     * @author lokka30
     * @since v4.0.0
     * Used to create objects contained within the queue,
     * encompassing the entity that will present the nametag,
     * and the text content of the nametag. The recipients of
     * the nametag packet will be determined as the item is processed.
     * @see NametagProcessingQueue
     */
    public static class NametagUpdateQueueItem {

        public final LevelledMob entity;
        public final String nametag;

        public NametagUpdateQueueItem(final LevelledMob entity, final String nametag) {
            this.entity = entity;
            this.nametag = nametag;
        }

    }


}
