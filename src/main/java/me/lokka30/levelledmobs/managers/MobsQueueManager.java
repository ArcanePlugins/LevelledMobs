/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.managers;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.QueueItem;
import me.lokka30.levelledmobs.util.Utils;
import me.lokka30.levelledmobs.wrappers.SchedulerWrapper;
import org.bukkit.Bukkit;
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

        if (!main.getVerInfo().getIsRunningFolia()){
            final Runnable bgThread = () -> {
                try {
                    mainThread();
                } catch (final InterruptedException ignored) {
                    isRunning = false;
                }
                Utils.logger.info("Mob processing queue Manager has exited");
            };

            Bukkit.getScheduler().runTaskAsynchronously(main, bgThread);
        }
    }

    public void stop() {
        doThread = false;
    }

    public void addToQueue(final @NotNull QueueItem item) {
        if (item.lmEntity.getLivingEntity() == null) {
            return;
        }

        item.lmEntity.inUseCount.getAndIncrement();

        if (main.getVerInfo().getIsRunningFolia()){
            final SchedulerWrapper wrapper = new SchedulerWrapper(
                    item.lmEntity.getLivingEntity(), () -> {
                processItem(item);
                item.lmEntity.free();
            });

            wrapper.run();
        }
        else{
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
