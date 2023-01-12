package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.impl;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.Condition;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.group.Group;
import io.github.arcaneplugins.levelledmobs.bukkit.util.modal.ModalCollection.Mode;
import io.github.arcaneplugins.levelledmobs.bukkit.util.modal.ModalList;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bukkit.block.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;

public class EntityBiomeCondition extends Condition {

    /* vars */

    /*
    Modal list of entity types

    Do not make this variable `final`.
     */
    private ModalList<Biome> biomeModalList = null;
    private ModalList<Group> groupModalList = null;

    /* constructors */

    public EntityBiomeCondition(
        final Process process,
        final CommentedConfigurationNode node
    ) {
        super(process, node);

        try {
            if (getConditionNode().hasChild("in-list")) {
                biomeModalList = new ModalList<>(
                    getConditionNode().node("in-list")
                        .getList(Biome.class, Collections.emptyList()),
                    Mode.INCLUSIVE
                );
            } else if (getConditionNode().hasChild("not-in-list")) {
                biomeModalList = new ModalList<>(
                    getConditionNode().node("not-in-list")
                        .getList(Biome.class, Collections.emptyList()),
                    Mode.EXCLUSIVE
                );
            } else if (getConditionNode().hasChild("in-group")) {
                groupModalList = new ModalList<>(
                    getConditionNode().node("in-group")
                        .getList(String.class, Collections.emptyList())
                        .stream()
                        .map(groupId -> {
                            final Optional<Group> group = LogicHandler.getGroups().stream()
                                .filter(otherGroup ->
                                    otherGroup.getIdentifier().equalsIgnoreCase(groupId))
                                .findFirst();

                            if(group.isEmpty()) {
                                throw new IllegalArgumentException("Unknown group: " + groupId);
                            }

                            return group.get();
                        })
                        .collect(Collectors.toList()),
                    Mode.INCLUSIVE
                );
            } else if (getConditionNode().hasChild("not-in-group")) {
                groupModalList = new ModalList<>(
                    getConditionNode().node("not-in-group")
                        .getList(String.class, Collections.emptyList())
                        .stream()
                        .map(groupId -> {
                            final Optional<Group> group = LogicHandler.getGroups().stream()
                                .filter(otherGroup ->
                                    otherGroup.getIdentifier().equalsIgnoreCase(groupId))
                                .findFirst();

                            if(group.isEmpty()) {
                                throw new IllegalArgumentException("Unknown group: " + groupId);
                            }

                            return group.get();
                        })
                        .collect(Collectors.toList()),
                    Mode.EXCLUSIVE
                );
            } else {
                throw new IllegalArgumentException(
                    "Missing 'in-list' or 'not-in-list' or 'in-group' or 'not-in-group' "
                        + "declaration in entity biome condition"
                );
            }
        } catch (final ConfigurateException ex) {
            throw new RuntimeException(ex);
        }
    }

    /* methods */

    @Override
    public boolean applies(final @NotNull Context context) {
        Objects.requireNonNull(context.getEntity(), "entity");
        Objects.requireNonNull(context.getEntity().getLocation(), "location");

        final Biome biome = context.getEntity().getLocation().getBlock().getBiome();

        if(getBiomeModalList() != null) {
            return getBiomeModalList().contains(biome);
        } else if(getGroupModalList() != null) {
            /*
            if any of the groups in the group modal list contain the biome, and the modal list
            mode is Inclusive, then return true
             */
            final boolean contains = getGroupModalList().getItems().stream()
                .anyMatch(group -> group.getItems().contains(biome.name()));

            final Mode mode = getGroupModalList().getMode();

            return switch (mode) {
                case INCLUSIVE -> contains;
                case EXCLUSIVE -> !contains;
            };
        } else {
            throw new IllegalStateException("Biome and group modal lists are undefined");
        }
    }


    /* getters and setters */

    @Nullable
    public ModalList<Biome> getBiomeModalList() {
        return biomeModalList;
    }

    @Nullable
    public ModalList<Group> getGroupModalList() { return groupModalList; }

}
