/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.levelling;

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
    LEVELLABLE(LevellableState.defaultInfo);

    @NotNull public static final String defaultInfo = "N/A";
    @NotNull public String additionalInfo;
    LevellableState(@NotNull final String additionalInfo) { this.additionalInfo = additionalInfo; }

    /**
     * @param additionalInfo what additional info the levellable state should have
     * @since v4.0.0
     */
    public void setAdditionalInfo(@NotNull final String additionalInfo) { this.additionalInfo = additionalInfo; }
}
