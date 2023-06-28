/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.managers;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.QueueItem;
import me.lokka30.levelledmobs.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

/**
 * Queues up mob info so they can be processed in a background thread
 *
 * @author stumper66
 * @since 3.0.0
 */
public class MobsQueueManager {

    public MobsQueueManager(final LevelledMobs main) {
        this.main = main;
        this.queue = new LinkedBlockingQueue<>();
    }

    private final LevelledMobs main;
    private boolean isRunning;
    private boolean doThread;
    private final LinkedBlockingQueue<QueueItem> queue;

    public void start() {
        if (isRunning) {
            return;
        }
        doThread = true;
        isRunning = true;

        if (main.getDefinitions().getIsFolia()){
//            Consumer<ScheduledTask> bgThread = scheduledTask -> {
//                try {
//                    mainThread();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                Utils.logger.info("Mob processing queue Manager has exited");
//            };
//
//            org.bukkit.Bukkit.getAsyncScheduler().runNow(main, bgThread);
        }
        else{
            final BukkitRunnable bgThread = new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        mainThread();
                    } catch (final InterruptedException ignored) {
                        isRunning = false;
                    }
                    Utils.logger.info("Mob processing queue Manager has exited");
                }
            };

            bgThread.runTaskAsynchronously(main);
        }
    }

    public void stop() {
        doThread = false;
    }

    public void addToQueue(final @NotNull QueueItem item) {
        if (item.lmEntity.getLivingEntity() == null) {
            return;
        }

        if (main.getDefinitions().getIsFolia()){
            processItem(item);
        }
        else{
            item.lmEntity.inUseCount.getAndIncrement();
            this.queue.offer(item);
        }
    }

    private void mainThread() throws InterruptedException {
        while (doThread) {

            final QueueItem item = queue.poll(200, TimeUnit.MILLISECONDS);
            if (item == null) {
                continue;
            }

            try {
                processItem(item);
            } catch (final Exception e) {
                e.printStackTrace();
            } finally {
                item.lmEntity.free();
            }
        }

        isRunning = false;
    }

    private void processItem(final @NotNull QueueItem item){
        if (item.lmEntity.getLivingEntity() != null) {
            if (!item.lmEntity.getIsPopulated()) {
                return;
            }
            if (!item.lmEntity.getShouldShowLM_Nametag()) {
                return;
            }
            main.levelManager.entitySpawnListener.preprocessMob(item.lmEntity, item.event);
        }
    }
}
