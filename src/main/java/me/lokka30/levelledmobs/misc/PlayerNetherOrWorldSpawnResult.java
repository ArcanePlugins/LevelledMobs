package me.lokka30.levelledmobs.misc;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public class PlayerNetherOrWorldSpawnResult {
    public PlayerNetherOrWorldSpawnResult(final @NotNull Location location, final boolean isNetherPortalLocation){
        this.location = location;
        this.isNetherPortalLocation = isNetherPortalLocation;
    }

    public final @NotNull Location location;
    public boolean isNetherPortalLocation;
}
