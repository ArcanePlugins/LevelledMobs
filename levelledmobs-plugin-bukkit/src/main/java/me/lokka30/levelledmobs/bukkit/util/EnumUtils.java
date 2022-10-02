package me.lokka30.levelledmobs.bukkit.util;

import org.apache.commons.text.WordUtils;
import org.jetbrains.annotations.NotNull;

public class EnumUtils extends UtilityClass {

    /*
    Turns enum constant strings from e.g. 'WITHER_SKELETON' to 'Wither Skeleton'.
     */
    public static String formatEnumConstant(final @NotNull Enum<?> constant) {
        return WordUtils.capitalizeFully(constant.toString().replace('_', ' '));
    }

}
