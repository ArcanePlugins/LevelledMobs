package me.lokka30.levelledmobs.bukkit.util;

import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;

public class StringUtils {

    private StringUtils() {}

    @NotNull
    public static String replaceIfExists(
        final @NotNull String str,
        final @NotNull String target,
        final @NotNull Supplier<@NotNull String> operation
    ) {
        if(str.contains(target)) {
            return str.replace(target, operation.get());
        }
        return str;
    }

}
