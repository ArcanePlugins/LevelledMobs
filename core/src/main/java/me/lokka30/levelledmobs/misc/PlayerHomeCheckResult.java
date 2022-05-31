package me.lokka30.levelledmobs.misc;

import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

public class PlayerHomeCheckResult {
    public @Nullable Location location;
    public @Nullable final String resultMessage;
    public @Nullable String homeNameUsed;

    public PlayerHomeCheckResult(final @Nullable String resultMessage){
        this.resultMessage = resultMessage;
    }

    public PlayerHomeCheckResult(final @Nullable String resultMessage, final @Nullable Location location){
        this.resultMessage = resultMessage;
        this.location = location;
    }

    public PlayerHomeCheckResult(final @Nullable String resultMessage, final @Nullable Location location, final @Nullable String homeNameUsed){
        this.resultMessage = resultMessage;
        this.location = location;
        this.homeNameUsed = homeNameUsed;
    }
}
