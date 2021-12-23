/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.debug;

/**
 * @author lokka30
 * @since v4.0.0
 * This class handles all debug-logging in the plugin.
 */
public class DebugHandler {

    /**
     * @author lokka30
     * @since v4.0.0
     * Defines what 'category' a debug message falls under.
     * For example, a debug message from the Update Checker
     * could be 'DebugCategory.UPDATE_CHECKER'.
     */
    public enum DebugCategory {
        /*
        TODO
            lokka30: This enum serves the exact same purpose as LM3's
                     'DebugType' enum, although it starts over with
                     none of the old constants being carried over.
                     Add new constants as they are required.
         */
    }

    /**
     * @param category category that is being checked
     * @return if the category is enabled or not
     * Checks the advanced-settings.yml file to see if a debug category is enabled or not.
     * @author lokka30
     * @since v4.0.0
     */
    public boolean isDebugCategoryEnabled(final DebugCategory category) {
        /*
        TODO
            lokka30: Complete method body.
         */
        return false;
    }

    public void sendDebugLog(final DebugCategory category, final String msg) {
        /*
        TODO
            lokka30: Complete method body.
            lokka30: Add javadoc.
         */
    }
}
