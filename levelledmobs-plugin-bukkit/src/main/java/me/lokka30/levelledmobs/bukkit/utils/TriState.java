package me.lokka30.levelledmobs.bukkit.utils;

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
}
