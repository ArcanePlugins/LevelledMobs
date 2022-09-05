package me.lokka30.levelledmobs.result;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public record PlayerNetherOrWorldSpawnResult(@NotNull Location location, boolean isNetherPortalLocation,
                                             boolean isWorldPortalLocation) {

    public PlayerNetherOrWorldSpawnResult(final @NotNull Location location,
                                          final boolean isNetherPortalLocation, final boolean isWorldPortalLocation) {
        this.location = location;
        this.isNetherPortalLocation = isNetherPortalLocation;
        this.isWorldPortalLocation = isWorldPortalLocation;
    }
}
