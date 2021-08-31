/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.queue;

/**
 * @author lokka30
 * @since v4.0.0
 * This interface acts as a template for all Queues
 * the plugin has. For example, MobProcessingQueue,
 * being one of the most important.
 * @see me.lokka30.levelledmobs.queue.MobProcessingQueue
 */
public interface Queue {

    /**
     * @author lokka30
     * @since v4.0.0
     * @return the name of the Queue class (e.g. 'MobProcessingQueue')
     */
    String getName();

    /**
     * @author stumper66, lokka30
     * @since v4.0.0
     * Start the Queue.
     * @throws UnsupportedOperationException if it is already running.
     */
    void start();

    /**
     * @author stumper66, lokka30
     * @since v4.0.0
     * Stops the Queue.
     * @throws UnsupportedOperationException if it is already not running.
     */
    void stop();

    /**
     * @author stumper66, lokka30
     * @since v4.0.0
     * @param item adds this item to the Queue for processing.
     */
    void addItem(Object item);

}
