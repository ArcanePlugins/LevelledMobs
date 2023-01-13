package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl;

import io.github.arcaneplugins.levelledmobs.bukkit.integration.IntegrationHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.integration.type.nbt.NbtModificationResult;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;

public class AddNbtTagAction extends Action {

    /* vars */

    private final Set<String> tags = new HashSet<>();

    /* constructors */

    public AddNbtTagAction(
        final @NotNull Process parentProcess,
        final @NotNull CommentedConfigurationNode actionNode
    ) {
        super(parentProcess, actionNode);

        try {
            if(getActionNode().hasChild("tags")) {
                this.tags.addAll(getActionNode().node("tags").getList(String.class, Collections.emptyList()));
            } else if(getActionNode().hasChild("tag")) {
                this.tags.add(getActionNode().node("tag").getString(""));
            } else {
                throw new IllegalStateException("No valid NBT tag(s) were specified.");
            }
        } catch(ConfigurateException | NullPointerException ex) {
            throw new IllegalStateException("Parsing error - likely caused by a user " +
                "syntax error.", ex);
        }
    }

    /* methods */

    @Override
    public void run(Context context) {
        if(context.getEntity() == null) {
            throw new IllegalStateException("No Entity context available");
        }

        if(context.getEntity() instanceof LivingEntity livingEntity) {
            final var nbtProvider = IntegrationHandler.getPrimaryNbtProvider();

            if(nbtProvider == null) {
                throw new IllegalStateException("Can't run AddNbtTagAction: no NBT " +
                    "provider available.");
            }

            for(final String tag : tags) {
                final NbtModificationResult result = nbtProvider.addNbtTag(livingEntity, tag);
                //TODO should probably make the NbtProvider accept a collection of tags instead of
                //     calling the method for each tag
                if(result.hasException()) {
                    throw new RuntimeException(result.getException());
                }
            }
        } else {
            throw new IllegalStateException("Context's entity is not a LivingEntity");
        }
    }
}
