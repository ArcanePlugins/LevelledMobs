package me.lokka30.levelledmobs.bukkit.util.modal.impl;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import me.lokka30.levelledmobs.bukkit.util.Log;
import me.lokka30.levelledmobs.bukkit.util.modal.ModalCollection;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;

public class ModalEntityTypeSet extends ModalCollection<EntityType> {

    public ModalEntityTypeSet(
        @NotNull List<String> strItems,
        @NotNull Mode mode
    ) {
        super(EnumSet.noneOf(EntityType.class), mode);

        for(String strItem : strItems) {
            getItems().add(EntityType.valueOf(strItem.toUpperCase()));
        }
    }

    public static ModalEntityTypeSet parseNode(final CommentedConfigurationNode node) {
        try {
            if(node.hasChild("in-list")) {
                return new ModalEntityTypeSet(
                    node.node("in-list").getList(String.class, new ArrayList<>()),
                    Mode.INCLUSIVE
                );
            } else if(node.hasChild("not-in-list")) {
                return new ModalEntityTypeSet(
                    node.node("not-in-list").getList(String.class, new ArrayList<>()),
                    Mode.EXCLUSIVE
                );
            } else {
                throw new IllegalStateException(
                    "Modal list at path '" + node.path().toString() +
                        "' does not have 'in-list' or 'not-in-list' declared.");
            }
        } catch(ConfigurateException ex) {
            Log.sev("Unable to parse modal list at path: " + node.path().toString(), true);
            throw new RuntimeException(ex);
        }
    }

}
