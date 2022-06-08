package me.lokka30.levelledmobs.misc;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public class PlayerNetherOrWorldSpawnResult {
    public PlayerNetherOrWorldSpawnResult(final @NotNull Location location, final boolean isNetherPortalLocation, final boolean isWorldPortalLocation){
        this.location = location;
        this.isNetherPortalLocation = isNetherPortalLocation;
        this.isWorldPortalLocation = isWorldPortalLocation;
    }

    public final @NotNull Location location;
    public final boolean isNetherPortalLocation;
    public final boolean isWorldPortalLocation;
}
