package io.github.arcaneplugins.levelledmobs.bukkit.util.modal;

import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;

public class ModalList<T> extends ModalCollection<T> {

    public ModalList(
        final @NotNull List<T> items,
        final @NotNull ModalCollection.Mode mode
    ) {
        super(items, mode);
    }

    public static ModalList<String> parseModalStringListFromNode(
        final CommentedConfigurationNode node
    ) {
        try {
            if(node.hasChild("in-list")) {
                return new ModalList<>(
                    node.node("in-list").getList(String.class, new ArrayList<>()),
                    Mode.INCLUSIVE
                );
            } else if(node.hasChild("not-in-list")) {
                return new ModalList<>(
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
