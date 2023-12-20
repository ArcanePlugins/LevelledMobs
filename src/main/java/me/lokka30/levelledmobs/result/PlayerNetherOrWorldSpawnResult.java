package me.lokka30.levelledmobs.result;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Used to hold info that is used for various
 * custom placeholders
 *
 * @author stumper66
 * @since 3.6.0
 */
public record PlayerNetherOrWorldSpawnResult(@NotNull Location location, boolean isNetherPortalLocation,
                                             boolean isWorldPortalLocation) {

}
