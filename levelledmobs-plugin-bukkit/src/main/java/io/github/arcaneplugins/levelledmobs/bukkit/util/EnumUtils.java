package io.github.arcaneplugins.levelledmobs.bukkit.util;

import org.apache.commons.text.WordUtils;
import org.jetbrains.annotations.NotNull;

public class EnumUtils {

    private EnumUtils() throws IllegalAccessException {
        throw new IllegalAccessException("Attempted instantiation of utility class");
    }

    /*
    Turns enum constant strings from e.g. 'WITHER_SKELETON' to 'Wither Skeleton'.
     */
    public static String formatEnumConstant(final @NotNull Enum<?> constant) {
        return WordUtils.capitalizeFully(constant.toString().replace('_', ' '));
    }

}
