/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.managers;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.QueueItem;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Queues up mob info so they can be processed in a background thread
 *
 * @author stumper66
 */
public class QueueManager_Mobs {

    public QueueManager_Mobs(final LevelledMobs main){
        this.main = main;
        this.queue = new LinkedBlockingQueue<>();
    }

    private final LevelledMobs main;
    private boolean isRunning;
    private boolean doThread;
    private final LinkedBlockingQueue<QueueItem> queue;

    public void start(){
        if (isRunning) return;
        doThread = true;
        isRunning = true;

        final BukkitRunnable bgThread = new BukkitRunnable(){
            @Override
            public void run() {
                try {
                    main();
                } catch (InterruptedException ignored) {
                    isRunning = false;
                }
                Utils.logger.info("Mob processing queue Manager has exited");
            }
        };

        bgThread.runTaskAsynchronously(main);
    }

    public void stop(){
        doThread = false;
    }

    public void addToQueue(final QueueItem item) { this.queue.offer(item); }

    private void main() throws InterruptedException{
        while (doThread) {

            final QueueItem item = queue.poll(200, TimeUnit.MILLISECONDS);
            if (item == null) continue;

            main.levelManager.entitySpawnListener.preprocessMob(item.lmEntity, item.event);
        }

        isRunning = false;
    }
}
