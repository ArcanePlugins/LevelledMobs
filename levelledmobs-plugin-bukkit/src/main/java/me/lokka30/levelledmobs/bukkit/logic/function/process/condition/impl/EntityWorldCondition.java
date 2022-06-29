package me.lokka30.levelledmobs.bukkit.logic.function.process.condition.impl;

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

public class EntityWorldCondition extends Condition {

    /* vars */

    /*
    Modal list of entity types

    Do not make this variable `final`.
     */
    private ModalList<String> modalList;

    /* constructors */

    public EntityWorldCondition(Process process, final CommentedConfigurationNode node) {
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
                Log.sev("entity world condition error: no in-list/not-in-list declaration",
                    true);
            }
        } catch (ConfigurateException ex) {
            //TODO make better error message
            Log.sev("entity world condition error: unable to parse yml", true);
        }
    }

    /* methods */

    @Override
    public boolean applies(Context context) {
        assert context.getEntity() != null;
        return getModalList().contains(context.getEntity().getWorld().getName());
    }


    /* getters and setters */

    @NotNull
    public ModalList<String> getModalList() {
        return modalList;
    }

}
