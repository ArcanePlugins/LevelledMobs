package me.lokka30.levelledmobs.bukkit.utils;

import org.apache.commons.lang.WordUtils;

public class EnumUtils {

    /*
    Turns enum constant strings from e.g. 'WITHER_SKELETON' to 'Wither Skeleton'.
     */
    public static String formatEnumConstant(final Enum<?> constant) {
        return WordUtils.capitalizeFully(constant.toString().replace('_', ' '));
    }

}
