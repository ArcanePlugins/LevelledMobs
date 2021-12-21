/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.misc;


import me.lokka30.microlib.messaging.MicroLogger;

/**
 * @author lokka30
 * @since v4.0.0
 * This class contains a bunch of public static methods
 * used across multiple LevelledMobs classes, centralising
 * it so it doesn't have to be repeated, making it easier to
 * update the methods since they are in one location.
 */
public class Utils {

    /**
     * @since v2.0.0
     * This contains an instance of MicroLogger, used
     * across the plugin to log things to the console.
     */
    public static final MicroLogger logger = new MicroLogger("&b&lLevelledMobs: &7");
}
