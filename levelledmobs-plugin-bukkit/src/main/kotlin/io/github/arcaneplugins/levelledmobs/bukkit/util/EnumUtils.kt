package io.github.arcaneplugins.levelledmobs.bukkit.util

import org.apache.commons.text.WordUtils

object EnumUtils {
    /*
    Turns enum constant strings from e.g. 'WITHER_SKELETON' to 'Wither Skeleton'.
     */
    fun formatEnumConstant(constant: Enum<*>): String {
        return WordUtils.capitalizeFully(constant.toString().replace('_', ' '))
    }
}