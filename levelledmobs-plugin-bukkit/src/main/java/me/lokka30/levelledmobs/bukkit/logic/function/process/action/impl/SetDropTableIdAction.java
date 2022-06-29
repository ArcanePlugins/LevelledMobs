package me.lokka30.levelledmobs.bukkit.logic.function.process.action.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import me.lokka30.levelledmobs.bukkit.data.InternalEntityDataUtil;
import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import me.lokka30.levelledmobs.bukkit.logic.function.process.Process;
import me.lokka30.levelledmobs.bukkit.logic.function.process.action.Action;
import me.lokka30.levelledmobs.bukkit.util.Log;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;

public class SetDropTableIdAction extends Action {

    /* vars */

    private final Set<String> dropTableIds = new HashSet<>();

    /* constructors */

    public SetDropTableIdAction(
        @NotNull Process parentProcess,
        @NotNull CommentedConfigurationNode actionNode
    ) {
        super(parentProcess, actionNode);

        try {
            if(getActionNode().hasChild("ids")) {
                this.dropTableIds.addAll(getActionNode().node("ids").getList(String.class, Collections.emptyList()));
            } else if(getActionNode().hasChild("id")) {
                this.dropTableIds.add(getActionNode().node("id").getString(""));
            } else {
                Log.sev(String.format(
                    "Action 'set-drop-table-id' in process '%s' (in function '%s') does not " +
                        "specify a valid drop table ID, or list of drop table IDs.",
                    getParentProcess().getIdentifier(),
                    getParentProcess().getParentFunction().getIdentifier()
                ), true);
            }
        } catch(ConfigurateException | NullPointerException ex) {
            Log.sev(String.format(
                "Unable to parse action 'set-drop-table-id' in process '%s' (in function '%s'). " +
                "This is usually caused by a syntax error created by the user, double-check " +
                "your formatting of the 'settings.yml' file.",
                getParentProcess().getIdentifier(),
                getParentProcess().getParentFunction().getIdentifier()
            ), true);
        }
    }

    /* methods */

    @Override
    public void run(Context context) {
        if(context.getEntity() == null) {
            Log.sev(String.format(
                "A 'set-drop-table-id' action has encountered an issue in process '%s' (in " +
                    "function '%s'), where a context is missing an entity to handle.",
                getParentProcess().getIdentifier(),
                getParentProcess().getParentFunction().getIdentifier()
            ), true);
            return;
        }

        if(context.getEntity() instanceof LivingEntity livingEntity) {
            InternalEntityDataUtil.setDropTableIds(livingEntity, dropTableIds);
        } else {
            Log.sev(String.format(
                "A 'set-drop-table-id' action has encountered an issue in process '%s' (in " +
                    "function '%s'), where a context has an entity which is not a LivingEntity.",
                getParentProcess().getIdentifier(),
                getParentProcess().getParentFunction().getIdentifier()
            ), true);
        }
    }
}
