package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.impl;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.Condition;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import io.github.arcaneplugins.levelledmobs.bukkit.util.modal.ModalCollection.Mode;
import io.github.arcaneplugins.levelledmobs.bukkit.util.modal.ModalList;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;

public class EntityTypeCondition extends Condition {

    /* vars */

    /*
    Modal list of entity types

    Do not make this variable `final`.
     */
    private ModalList<EntityType> modalList;

    /* constructors */

    public EntityTypeCondition(Process process, final CommentedConfigurationNode node) {
        super(process, node);

        final Mode mode;
        final List<String> entityTypesStr;

        try {
            if (getConditionNode().hasChild("in-list")) {
                mode = Mode.INCLUSIVE;
                entityTypesStr = getConditionNode().node("in-list").getList(
                    String.class, Collections.emptyList()
                );
            } else if (getConditionNode().hasChild("not-in-list")) {
                mode = Mode.EXCLUSIVE;
                entityTypesStr = getConditionNode().node("not-in-list").getList(
                    String.class, Collections.emptyList()
                );
            } else {
                //TODO make better error message
                Log.sev("entity type condition error: no in-list/not-in-list declaration",
                    true);
                return;
            }
        } catch (ConfigurateException ex) {
            //TODO make better error message
            Log.sev("entity type condition error: unable to parse yml", true);
            return;
        }

        final List<EntityType> entityTypes = new LinkedList<>();

        for (var entityTypeStr : entityTypesStr) {
            try {
                entityTypes.add(EntityType.valueOf(entityTypeStr));
            } catch (IllegalArgumentException ignored) {
            }
        }

        this.modalList = new ModalList<>(entityTypes, mode);
    }

    /* methods */

    @Override
    public boolean applies(Context context) {
        return getModalList().contains(context.getEntityType());
    }


    /* getters and setters */

    @NotNull
    public ModalList<EntityType> getModalList() {
        return modalList;
    }

}