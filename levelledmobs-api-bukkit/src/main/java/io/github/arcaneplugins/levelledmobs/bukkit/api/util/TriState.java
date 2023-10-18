package io.github.arcaneplugins.levelledmobs.bukkit.api.util;

import org.jetbrains.annotations.Nullable;

public enum TriState {
    TRUE,
    UNKNOWN,
    FALSE;

    public static TriState of(final boolean bool) {
        return bool ? TRUE : FALSE;
    }

    public static TriState of(final @Nullable Boolean bool) {
        return bool == null ? UNKNOWN : of(bool);
    }

    // 'falsy': treats only TRUE as Boolean.TRUE
    public boolean toFalsyBoolean() {
        return this == TRUE;
    }
}
