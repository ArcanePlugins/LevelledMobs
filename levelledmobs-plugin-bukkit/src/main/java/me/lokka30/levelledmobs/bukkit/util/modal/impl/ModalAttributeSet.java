package me.lokka30.levelledmobs.bukkit.util.modal.impl;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import me.lokka30.levelledmobs.bukkit.util.Log;
import me.lokka30.levelledmobs.bukkit.util.modal.ModalCollection;
import org.bukkit.attribute.Attribute;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;

public class ModalAttributeSet extends ModalCollection<Attribute> {

    public ModalAttributeSet(
        @NotNull List<String> strItems,
        @NotNull Mode mode
    ) {
        super(EnumSet.noneOf(Attribute.class), mode);

        for(String strItem : strItems) {
            if(strItem.equals("*")) {
                getItems().clear();
                setMode(getMode().inverse());
                break;
            }
            if(strItem.startsWith("CUSTOM_")) {
                Log.tmpdebug("Note: Skipping custom attribute " + strItem + ": not implemented");
                continue;
            }
            if(strItem.equalsIgnoreCase("ZOMBIE_SPAWN_REINFORCEMENTS")) {
                // TODO note: Mojang might not have patched a major bug with reinforcements
                // that constantly multiply everywhere. keeping it disabled
                continue;
            }
            getItems().add(Attribute.valueOf(strItem.toUpperCase()));
        }
    }

    public static ModalAttributeSet parseNode(final CommentedConfigurationNode node) {
        try {
            if(node.hasChild("in-list")) {
                return new ModalAttributeSet(
                    node.node("in-list").getList(String.class, new ArrayList<>()),
                    Mode.INCLUSIVE
                );
            } else if(node.hasChild("not-in-list")) {
                return new ModalAttributeSet(
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
