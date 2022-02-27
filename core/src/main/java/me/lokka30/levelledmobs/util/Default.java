package me.lokka30.levelledmobs.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Default {

    public static Object of(@Nullable Object current, @NotNull Object def) {
        return current == null ? def : current;
    }

}
