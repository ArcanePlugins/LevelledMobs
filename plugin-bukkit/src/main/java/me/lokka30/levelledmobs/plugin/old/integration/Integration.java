/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.plugin.old.integration;

import me.lokka30.levelledmobs.plugin.old.LevelledMobs;
import org.jetbrains.annotations.NotNull;

/**
 * This interface is used by all Integrations, containing common methods used across all of them.
 *
 * @author lokka30
 * @since 4.0.0
 */
public interface Integration {

    /**
     * Get the name of the Integration.
     *
     * @return the name of the class providing the integration, e.g. {@code CitizensIntegration}.
     * @since 4.0.0
     */
    @NotNull
    String getName();

    /**
     * Check if the plugin being integrated with is installed.
     *
     * @return if the plugin being integrated with is installed.
     * @since 4.0.0
     */
    boolean isInstalled();

    /**
     * Integrations can be disabled by the user through the settings.yml file. Ensure the
     * integration is enabled prior to using it.
     *
     * @return if the integration is enabled.
     * @since 4.0.0
     */
    default boolean isEnabled() {
        return !(LevelledMobs.getInstance().fileHandler.settingsFile.getData()
            .getStringList("disabled-integrations")
            .contains(getName())) && isInstalled();
    }
}
