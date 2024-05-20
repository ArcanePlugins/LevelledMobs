package io.github.arcaneplugins.levelledmobs.result

import org.bukkit.Location

/**
 * Used to hold info that is used for various
 * custom placeholders
 *
 * @author stumper66
 * @since 3.6.0
 */
class PlayerNetherOrWorldSpawnResult(
    val location: Location?,
    var isNetherPortalLocation: Boolean,
    var isWorldPortalLocation: Boolean
)