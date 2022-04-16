/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.plugin.old.queue;

/**
 * @author lokka30
 * @see MobProcessingQueue
 * @since 4.0.0 This interface acts as a template for all Queues the plugin has. For example,
 * MobProcessingQueue, being one of the most important.
 */
public interface Queue {

    /**
     * @return the name of the Queue class (e.g. 'MobProcessingQueue')
     * @author lokka30
     * @since 4.0.0
     */
    String getName();

    /**
     * @throws UnsupportedOperationException if it is already running.
     * @author stumper66, lokka30
     * @since 4.0.0 Start the Queue.
     */
    void start();

    /**
     * @throws UnsupportedOperationException if it is already not running.
     * @author stumper66, lokka30
     * @since 4.0.0 Stops the Queue.
     */
    void stop();

    /**
     * @param item adds this item to the Queue for processing.
     * @author stumper66, lokka30
     * @since 4.0.0
     */
    void addItem(Object item);

}
