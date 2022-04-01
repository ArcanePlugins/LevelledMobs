/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.plugin.bukkit.queue;

import java.util.concurrent.ConcurrentLinkedQueue;
import me.lokka30.levelledmobs.plugin.bukkit.LevelledMobs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * @author lokka30, stumper66
 * @see Queue
 * @since 4.0.0 This Queue contains nametags that will be sent to various players.
 */
public final class NametagProcessingQueue implements Queue {

    private boolean isRunning = false;
    private boolean isCancelled = false;

    public final ConcurrentLinkedQueue<NametagUpdateQueueItem> queue = new ConcurrentLinkedQueue<>();

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public void start() {
        LevelledMobs.logger().info("Starting queue '&b" + getName() + "&7'...");

        if (isRunning) {
            throw new UnsupportedOperationException("Queue is already running.");
        }

        isRunning = true;
        isCancelled = false;

        new BukkitRunnable() {
            @Override
            public void run() {
                while (!isCancelled) {
                    final NametagUpdateQueueItem item = queue.poll();

                    if (item == null) {
                        continue;
                    }

                    //TODO process nametag.
                }

                this.cancel();
                isRunning = false;
            }
        }.runTaskAsynchronously(LevelledMobs.getInstance());
    }

    @Override
    public void stop() {
        LevelledMobs.logger().info("Stopping queue '&b" + getName() + "&7'...");

        if (!isRunning) {
            throw new UnsupportedOperationException("Queue is not running");
        }

        isCancelled = true;
        queue.clear();
    }

    @Override
    public void addItem(Object item) {
        queue.offer((NametagUpdateQueueItem) item);
    }

    /**
     * @author lokka30
     * @see NametagProcessingQueue
     * @since 4.0.0 Used to create objects contained within the queue, encompassing the entity that
     * will present the nametag, and the text content of the nametag. The recipients of the nametag
     * packet will be determined as the item is processed.
     */
    public static class NametagUpdateQueueItem {

        public final LivingEntity entity;
        public final String nametag;

        public NametagUpdateQueueItem(final LivingEntity entity, final String nametag) {
            this.entity = entity;
            this.nametag = nametag;
        }

    }


}
