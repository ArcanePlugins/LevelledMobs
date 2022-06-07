package me.lokka30.levelledmobs.bukkit.logic;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Context {

    /* vars */

    private Entity entity;
    private EntityType entityType;
    private Location location;
    private Player player;

    /* constructors */

    public Context() {}

    /* getters and setters */

    @NotNull
    public Context withEntity(final @NotNull Entity entity) {
        this.entity = entity;
        return this;
    }

    @Nullable
    public Entity getEntity() {
        return entity;
    }

    @NotNull
    public Context withEntityType(final @NotNull EntityType entityType) {
        this.entityType = entityType;
        return this;
    }

    @Nullable
    public EntityType getEntityType() {
        return entityType;
    }

    @NotNull
    public Context withLocation(final @NotNull Location location) {
        this.location = location;
        return this;
    }

    @Nullable
    public Location getLocation() {
        return location;
    }

    @NotNull
    public Context withPlayer(final @NotNull Player player) {
        this.player = player;
        return this;
    }

    @Nullable
    public Player getPlayer() { return player; }

}
