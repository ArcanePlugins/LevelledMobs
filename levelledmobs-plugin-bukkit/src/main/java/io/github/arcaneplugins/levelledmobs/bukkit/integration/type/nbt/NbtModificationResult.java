package io.github.arcaneplugins.levelledmobs.bukkit.integration.type.nbt;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class NbtModificationResult {

    private ItemStack itemStack;
    private Entity entity;
    private Exception exception;
    private final List<String> objectsAdded = new LinkedList<>();
    private final List<String> objectsUpdated = new LinkedList<>();
    private final List<String> objectsRemoved = new LinkedList<>();

    public NbtModificationResult(final ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public NbtModificationResult(final Entity entity) {
        this.entity = entity;
    }

    @NotNull
    public NbtModificationResult withException(final Exception exception) {
        this.exception = exception;
        return this;
    }

    @NotNull
    public NbtModificationResult withObjectsAdded(final Object... objects) {
        for(var object : objects)
            getObjectsAdded().add(object.toString());
        return this;
    }

    @NotNull
    public NbtModificationResult withObjectsUpdated(final Object... objects) {
        for(var object : objects)
            getObjectsUpdated().add(object.toString());
        return this;
    }

    @NotNull
    public NbtModificationResult withObjectsRemoved(final Object... objects) {
        for(var object : objects)
            getObjectsRemoved().add(object.toString());
        return this;
    }

    @NotNull
    public Entity getEntity() {
        return Objects.requireNonNull(entity);
    }

    public boolean hasEntity() {
        return entity != null;
    }

    @NotNull
    public ItemStack getItemStack() {
        return Objects.requireNonNull(itemStack);
    }

    public boolean hasItemStack() {
        return itemStack != null;
    }

    @NotNull
    public Exception getException() {
        return Objects.requireNonNull(exception);
    }

    public boolean hasException() {
        return exception != null;
    }

    @NotNull
    public List<String> getObjectsAdded() {
        return objectsAdded;
    }

    @NotNull
    public List<String> getObjectsUpdated() {
        return objectsUpdated;
    }

    @NotNull
    public List<String> getObjectsRemoved() {
        return objectsRemoved;
    }

}
