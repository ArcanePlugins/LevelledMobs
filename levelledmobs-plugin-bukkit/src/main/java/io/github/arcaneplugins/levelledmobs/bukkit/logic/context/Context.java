package io.github.arcaneplugins.levelledmobs.bukkit.logic.context;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.cdevent.CustomDropsEventType;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.LmFunction;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class Context {

    /* vars */

    private Entity entity;
    private EntityType entityType;
    private Player player;
    private World world;
    private Location location;
    private LivingEntity father;
    private LivingEntity mother;
    private final List<LmFunction> linkedFunctions = new LinkedList<>();
    private final Collection<LivingEntity> parents = new LinkedList<>();
    private Event event;
    private CustomDropsEventType customDropsEventType;

    /*
    Key: Misc Context ID (as used in placeholders)
    Val: Supplier for Context
     */
    private final Map<String, Supplier<Object>> miscContext = new HashMap<>();

    /* methods */

    @Nonnull
    public String replacePlaceholders(final @Nonnull String from) {
        Objects.requireNonNull(from, "from str");

        return LogicHandler.getContextPlaceholderHandler().replace(from, this);
    }

    /* getters and setters */

    @NotNull
    public Context withEntity(final @NotNull Entity entity) {
        this.entity = Objects.requireNonNull(entity, "entity");

        return this
            .withEntityType(entity.getType())
            .withLocation(entity.getLocation());
    }

    @Nullable
    public Entity getEntity() {
        return entity;
    }

    @NotNull
    public Context withEntityType(final @NotNull EntityType entityType) {
        this.entityType = Objects.requireNonNull(entityType, "entityType");
        return this;
    }

    @Nullable
    public EntityType getEntityType() {
        return entityType;
    }

    @NotNull
    public Context withPlayer(final @NotNull Player player) {
        this.player = Objects.requireNonNull(player, "player");
        return this;
    }

    @Nullable
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public Context withWorld(final @NotNull World world){
        this.world = Objects.requireNonNull(world, "world");
        return this;
    }

    @Nullable
    public World getWorld() {
        return world;
    }

    @NotNull
    public Context withLocation(final @Nullable Location location) {
        if(location != null && location.getWorld() != null) {
            withWorld(location.getWorld());
        }

        return this;
    }

    @Nullable
    public Location getLocation() {
        return location;
    }

    @NotNull
    public Context withLinkedFunction(final LmFunction linkedFunction) {
        getLinkedFunctions().add(linkedFunction);
        return this;
    }

    @NotNull
    public List<LmFunction> getLinkedFunctions() { return linkedFunctions; }

    @NotNull
    public Context withFather(final LivingEntity father) {
        this.father = father;
        return this;
    }

    @Nullable
    public LivingEntity getFather() { return father; }

    @NotNull
    public Context withMother(final LivingEntity mother) {
        this.mother = mother;
        return this;
    }

    @Nullable
    public LivingEntity getMother() { return mother; }

    @Nullable
    public Event getEvent() {
        return event;
    }

    @NotNull
    public Context withEvent(final @Nullable Event event) {
        this.event = event;
        return this;
    }

    @NotNull
    public Context withCustomDropsEventType(final CustomDropsEventType event) {
        Objects.requireNonNull(event, "event");
        this.customDropsEventType = event;
        return this;
    }

    public @Nullable CustomDropsEventType getCustomDropsEventType() {
        return customDropsEventType;
    }

    public Map<String, Supplier<Object>> getMiscContextMap() {
        return miscContext;
    }

    public @NotNull Context withMiscContext(final String key, final Supplier<Object> value) {
        getMiscContextMap().put(key, value);
        return this;
    }

    public @Nullable Supplier<Object> getMiscContext(final String key) {
        return getMiscContextMap().get(key);
    }

}
