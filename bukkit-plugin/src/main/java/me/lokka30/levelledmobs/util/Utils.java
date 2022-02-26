/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.util;

import java.util.Locale;
import me.lokka30.microlib.messaging.MicroLogger;

/**
 * @author lokka30
 * @since 4.0.0 This class contains a bunch of public static methods used across multiple
 * LevelledMobs classes, centralising it so it doesn't have to be repeated, making it easier to
 * update the methods since they are in one location.
 */
public final class Utils {

    /**
     * @since 2.0.0 This contains an instance of MicroLogger, used across the plugin to log things
     * to the console.
     */
    public static final MicroLogger LOGGER = new MicroLogger("&b&lLevelledMobs: &7");

    public static String formatEnumConstant(String constant) {
        final String[] words = constant.split("_");;
        for(int i = 0; i < words.length; i++) {
            words[i] = words[i].substring(0, 1).toUpperCase(Locale.ROOT) +
                    words[i].substring(1).toLowerCase(Locale.ROOT);
        }
        return String.join(" ", words);
    }
}
