/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.levelling;

import me.lokka30.levelledmobs.LevelledMobs;

/**
 * @author lokka30
 * @since v4.0.0
 * This class handles the management and
 * deployment of nametags on mobs, be it
 * ProtocolLib-based or CustomName-based.
 */
public class NametagHandler {

    private final LevelledMobs main;

    public NametagHandler(final LevelledMobs main) {
        this.main = main;
    }

    /**
     * A list of available nametag systems
     */
    public enum NametagSystem {

        /**
         * @since v4.0.0
         * Nametags sent as packets through
         * the ProtocolLib plugin, if it's
         * installed.
         */
        PROTOCOL_LIB_SYSTEM,

        /**
         * @since v4.0.0
         * This system was used in LevelledMobs 1,
         * it changes the actual nametag of the mob
         * with the LevelledMobs one.
         * Not recommended, but available in case
         * specific server requirements justify it.
         */
        CUSTOM_NAME_SYSTEM,

        /**
         * @since v4.0.0
         * No nametag system is in use.
         */
        DISABLED
    }

    /*
    TODO
        lokka30: Complete class with methods.
     */
}
