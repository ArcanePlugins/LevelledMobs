/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.levelling;

import org.jetbrains.annotations.NotNull;

/**
 * @author lokka30
 * @since v4.0.0
 * This enum contains constants that allow
 * LM code to say 'this mob is levellable/not levellable',
 * and if 'not levellable', there are a bunch of constants
 * in this enum that can explain why a mob is considered such.
 */
public enum LevellableState {

    /*
    TODO
        lokka30: Add more constants as they become required.
     */

    /**
     * @since v4.0.0
     * The mob is levellable.
     */
    LEVELLABLE(LevellableState.defaultInfo);

    @NotNull public static final String defaultInfo = "N/A";
    @NotNull public String additionalInfo;
    LevellableState(@NotNull final String additionalInfo) { this.additionalInfo = additionalInfo; }

    /**
     * @author lokka30
     * @since v4.0.0
     * @param additionalInfo what additional info the levellable state should have
     */
    public void setAdditionalInfo(@NotNull final String additionalInfo) { this.additionalInfo = additionalInfo; }
}
