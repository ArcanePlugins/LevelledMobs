/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.level;

import org.jetbrains.annotations.NotNull;

/**
 * This enum contains constants that allow
 * LM code to say 'this mob is levellable/not levellable',
 * and if 'not levellable', there are a bunch of constants
 * in this enum that can explain why a mob is considered such.
 *
 * @author lokka30
 * @since v4.0.0
 */
public enum LevellableState {

    /*
    TODO
        lokka30: Add more constants as they become required.
     */

    /**
     * The mob is levellable.
     *
     * @since v4.0.0
     */
    LEVELLABLE(LevellableState.DEFAULT_INFO);

    @NotNull public static final String DEFAULT_INFO = "N/A";
    @NotNull public String additionalInfo;
    LevellableState(@NotNull final String additionalInfo) { this.additionalInfo = additionalInfo; }

    /**
     * @param additionalInfo what additional info the levellable state should have
     * @since v4.0.0
     */
    public void setAdditionalInfo(@NotNull final String additionalInfo) { this.additionalInfo = additionalInfo; }
}
