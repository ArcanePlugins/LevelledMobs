package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.impl;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.Condition;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import java.util.Collections;
import io.github.arcaneplugins.levelledmobs.bukkit.util.modal.ModalCollection.Mode;
import io.github.arcaneplugins.levelledmobs.bukkit.util.modal.ModalList;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;

public class PlayerWorldCondition extends Condition {

    /* vars */

    /*
    Modal list of entity types

    Do not make this variable `final`.
     */
    private ModalList<String> modalList;

    /* constructors */

    public PlayerWorldCondition(Process process, final CommentedConfigurationNode node) {
        super(process, node);

        try {
            if (getConditionNode().hasChild("in-list")) {
                this.modalList = new ModalList<>(
                    getConditionNode().node("in-list")
                        .getList(String.class, Collections.emptyList()),
                    Mode.INCLUSIVE
                );
            } else if (getConditionNode().hasChild("not-in-list")) {
                this.modalList = new ModalList<>(
                    getConditionNode().node("not-in-list")
                        .getList(String.class, Collections.emptyList()),
                    Mode.EXCLUSIVE
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
        assert context.getPlayer() != null;
        return getModalList().contains(context.getPlayer().getWorld().getName());
    }


    /* getters and setters */

    @NotNull
    public ModalList<String> getModalList() {
        return modalList;
    }

}
