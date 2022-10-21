package me.lokka30.levelledmobs.result;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public record PlayerNetherOrWorldSpawnResult(@NotNull Location location, boolean isNetherPortalLocation,
                                             boolean isWorldPortalLocation) {

}
