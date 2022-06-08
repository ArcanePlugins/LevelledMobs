package me.lokka30.levelledmobs.bukkit.listener.condition;

import java.util.Collections;
import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import me.lokka30.levelledmobs.bukkit.logic.function.process.Process;
import me.lokka30.levelledmobs.bukkit.logic.function.process.condition.Condition;
import me.lokka30.levelledmobs.bukkit.util.Log;
import me.lokka30.levelledmobs.bukkit.util.modal.ModalCollection.Type;
import me.lokka30.levelledmobs.bukkit.util.modal.ModalList;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;

public class EntityCustomNameContains extends Condition {

    /* vars */

    /*
    Modal list of entity types

    Do not make this variable `final`.
     */
    private ModalList<String> modalList;

    /* constructors */

    public EntityCustomNameContains(Process process, final CommentedConfigurationNode node) {
        super(process, node);

        try {
            if (getConditionNode().hasChild("in-list")) {
                this.modalList = new ModalList<>(
                    getConditionNode().node("in-list")
                        .getList(String.class, Collections.emptyList()),
                    Type.INCLUSIVE
                );
            } else if (getConditionNode().hasChild("not-in-list")) {
                this.modalList = new ModalList<>(
                    getConditionNode().node("not-in-list")
                        .getList(String.class, Collections.emptyList()),
                    Type.EXCLUSIVE
                );
            } else {
                //TODO make better error message
                Log.sev("entity custom name contains condition error: no in-list/not-in-list declaration",
                    true);
            }
        } catch (ConfigurateException ex) {
            //TODO make better error message
            Log.sev("entity custom name contains condition error: unable to parse yml", true);
        }
    }

    /* methods */

    @Override
    public boolean applies(Context context) {

        assert context.getEntity() != null;

        final var customName = context.getEntity().getCustomName();

        if(customName == null)
            return getModalList().getType() == Type.EXCLUSIVE;

        final var hasAsterisk = getModalList().getItems().stream().anyMatch("*"::contains);

        if(hasAsterisk) {
            return switch(getModalList().getType()) {
                case INCLUSIVE -> true;
                case EXCLUSIVE -> false;
            };
        }

        final var contains = getModalList().getItems().stream()
            .anyMatch(customName::contains);

        return switch(getModalList().getType()) {
            case INCLUSIVE -> contains;
            case EXCLUSIVE -> !contains;
        };
    }


    /* getters and setters */

    @NotNull
    public ModalList<String> getModalList() {
        return modalList;
    }

}
