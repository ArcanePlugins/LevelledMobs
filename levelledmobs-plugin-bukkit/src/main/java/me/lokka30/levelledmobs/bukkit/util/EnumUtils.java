package me.lokka30.levelledmobs.bukkit.util;

import org.apache.commons.lang.WordUtils;

public class EnumUtils extends UtilityClass {

    /*
    Turns enum constant strings from e.g. 'WITHER_SKELETON' to 'Wither Skeleton'.
     */
    public static String formatEnumConstant(final Enum<?> constant) {
        return WordUtils.capitalizeFully(constant.toString().replace('_', ' '));
    }

}
