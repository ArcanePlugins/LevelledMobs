/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.plugin.old.queue;

import java.util.concurrent.ConcurrentLinkedQueue;
import me.lokka30.levelledmobs.plugin.old.LevelledMobs;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * @author lokka30, stumper66
 * @see Queue
 * @since 4.0.0 This Queue handles mob processing (after they spawn in, they get levelled).
 */
public class MobProcessingQueue implements Queue {

    private boolean isRunning = false;
    private boolean isCancelled = false;

    public final ConcurrentLinkedQueue<EntitySpawnEvent> queue = new ConcurrentLinkedQueue<>();

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public void start() {
        LevelledMobs.logger().info("Starting queue '&b" + getName() + "&7'...");

        if(isRunning) {
            throw new UnsupportedOperationException("Queue is already running");
        }

        isRunning = true;
        isCancelled = false;

        new BukkitRunnable() {
            @Override
            public void run() {
                while(!isCancelled) {
                    final EntitySpawnEvent item = queue.poll();

                    if(item == null) {
                        continue;
                    }

                    //TODO process mob. old code:
                    // main.levelManager.entitySpawnListener.preprocessMob(item.lmEntity, item.event);
                }

                this.cancel();
                isRunning = false;
            }
        }.runTaskAsynchronously(LevelledMobs.getInstance());
    }

    @Override
    public void stop() {
        LevelledMobs.logger().info("Stopping queue '&b" + getName() + "&7'...");

        if(!isRunning) {
            throw new UnsupportedOperationException("Queue is not running");
        }

        isCancelled = true;
        queue.clear();
    }

    @Override
    public void addItem(Object item) {
        queue.offer((EntitySpawnEvent) item);
    }
}
