/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTransformEvent;

/**
 * @author lokka30
 * @since v4.0.0
 * TODO Edit Description Here
 */
public class EntityTransformListener implements Listener {

    /*
    TODO
        lokka30: edit javadoc description
        lokka30: complete event handler
     */

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onEntityTransform(final EntityTransformEvent event) {
        //TODO
    }

}
