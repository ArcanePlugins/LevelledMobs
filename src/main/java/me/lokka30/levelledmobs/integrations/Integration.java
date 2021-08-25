/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.integrations;

/**
 * @author lokka30
 * @since v4.0.0
 * This interface is used by all Integrations,
 * containing common methods used across all of
 * them.
 */
public interface Integration {

    /**
     * @return if the plugin being integrated with is installed
     * @since v4.0.0
     */
    boolean isInstalled();

    /**
     * @return if the integration is force-disabled in advanced.yml and/or the other plugin is not installed.
     * @since v4.0.0
     * Note: integrations are disabled in advanced.yml - see 'disabled-integrations'.
     */
    boolean isForceDisabled();
}
