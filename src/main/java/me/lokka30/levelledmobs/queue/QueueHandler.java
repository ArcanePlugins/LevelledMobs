/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.queue;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.Utils;

import java.util.Arrays;
import java.util.HashSet;

/**
 * @author lokka30
 * @since v4.0.0
 * This class handles all the Queues used in LevelledMobs.
 * @see Queue
 */
public class QueueHandler {

    private final LevelledMobs main;
    public QueueHandler(final LevelledMobs main) {
        this.main = main;

        this.queues = new HashSet<>(Arrays.asList(
                new MobProcessingQueue(main),
                new NametagProcessingQueue(main)
        ));
    }

    /**
     * @since v4.0.0
     * ALL queues must be included here. See the constructor, each Queue is added there.
     */
    public final HashSet<Queue> queues;

    /**
     * @author lokka30
     * @since v4.0.0
     * Start all queues.
     */
    public void startQueues() {
        Utils.LOGGER.info("Starting all queues...");
        queues.forEach(Queue::start);
    }

    /**
     * @author lokka30
     * @since v4.0.0
     * Stop all queues.
     */
    public void stopQueues() {
        Utils.LOGGER.info("Stopping all queues...");
        queues.forEach(Queue::stop);
    }
}
