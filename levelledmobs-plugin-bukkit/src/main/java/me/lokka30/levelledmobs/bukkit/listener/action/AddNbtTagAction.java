package me.lokka30.levelledmobs.bukkit.listener.action;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import me.lokka30.levelledmobs.bukkit.LevelledMobs;
import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import me.lokka30.levelledmobs.bukkit.logic.function.process.Process;
import me.lokka30.levelledmobs.bukkit.logic.function.process.action.Action;
import me.lokka30.levelledmobs.bukkit.util.Log;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;

public class AddNbtTagAction extends Action {

    /* vars */

    private final Set<String> tags = new HashSet<>();

    /* constructors */

    public AddNbtTagAction(
        @NotNull Process parentProcess,
        @NotNull CommentedConfigurationNode actionNode
    ) {
        super(parentProcess, actionNode);

        try {
            if(getActionNode().hasChild("tags")) {
                this.tags.addAll(getActionNode().node("tags").getList(String.class, Collections.emptyList()));
            } else if(getActionNode().hasChild("tag")) {
                this.tags.add(getActionNode().node("tag").getString(""));
            } else {
                Log.sev(String.format(
                    "Action 'add-nbt-tag' in process '%s' (in function '%s') does not " +
                        "specify a valid NBT tag, or list of NBT tags.",
                    getParentProcess().getIdentifier(),
                    getParentProcess().getParentFunction().getIdentifier()
                ), true);
            }
        } catch(ConfigurateException | NullPointerException ex) {
            Log.sev(String.format(
                "Unable to parse action 'add-nbt-tag' in process '%s' (in function '%s'). " +
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
                "A 'add-nbt-tag' action has encountered an issue in process '%s' (in " +
                    "function '%s'), where a context is missing an entity to handle.",
                getParentProcess().getIdentifier(),
                getParentProcess().getParentFunction().getIdentifier()
            ), true);
            return;
        }

        if(context.getEntity() instanceof LivingEntity livingEntity) {
            final var nbtProvider = LevelledMobs.getInstance()
                .getIntegrationHandler()
                .getPrimaryNbtProvider();

            if(nbtProvider == null) {
                //TODO improve error message
                Log.sev("nbt error: no nbt provider", true);
                getParentProcess().setShouldExit(true);
                getParentProcess().getParentFunction().setShouldExit(true);
                getParentProcess().getParentFunction().setShouldExitAll(true);
                //todo darn, this really needs to be cleaned up
                return;
            }

            for(var tag : tags) {
                final var result = nbtProvider.addNbtTag(livingEntity, tag);
                //TODO should probably make the NbtProvider accept a collection of tags instead of
                //     calling the method for each tag
                if(result.hasException()) {
                    //TODO improve error message
                    Log.war("nbt error: " + result.getException(), true);
                    getParentProcess().setShouldExit(true);
                    getParentProcess().getParentFunction().setShouldExit(true);
                    getParentProcess().getParentFunction().setShouldExitAll(true);
                }
            }
        } else {
            Log.sev(String.format(
                "A 'add-nbt-tag' action has encountered an issue in process '%s' (in " +
                    "function '%s'), where a context has an entity which is not a LivingEntity.",
                getParentProcess().getIdentifier(),
                getParentProcess().getParentFunction().getIdentifier()
            ), true);
            getParentProcess().setShouldExit(true);
            getParentProcess().getParentFunction().setShouldExit(true);
            getParentProcess().getParentFunction().setShouldExitAll(true);
        }
    }
}
