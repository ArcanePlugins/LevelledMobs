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
import org.bukkit.block.Biome;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;

public class EntityBiomeCondition extends Condition {

    /* vars */

    /*
    Modal list of entity types

    Do not make this variable `final`.
     */
    private ModalList<Biome> modalList;

    /* constructors */

    public EntityBiomeCondition(final Process process, final CommentedConfigurationNode node) {
        super(process, node);

        final Mode mode;
        final List<String> biomeTypesStr;

        try {
            if (getConditionNode().hasChild("in-list")) {
                mode = Mode.INCLUSIVE;
                biomeTypesStr = getConditionNode().node("in-list").getList(
                    String.class, Collections.emptyList()
                );
            } else if (getConditionNode().hasChild("not-in-list")) {
                mode = Mode.EXCLUSIVE;
                biomeTypesStr = getConditionNode().node("not-in-list").getList(
                    String.class, Collections.emptyList()
                );
            } else {
                //TODO make better error message
                Log.sev("entity biome condition error: no in-list/not-in-list declaration",
                    true);
                return;
            }
        } catch (ConfigurateException ex) {
            //TODO make better error message
            Log.sev("entity biome condition error: unable to parse yml", true);
            return;
        }

        final List<Biome> biomeTypes = new LinkedList<>();

        for (var entityTypeStr : biomeTypesStr) {
            try {
                biomeTypes.add(Biome.valueOf(entityTypeStr));
            } catch (IllegalArgumentException ignored) {
            }
        }

        this.modalList = new ModalList<>(biomeTypes, mode);
    }

    /* methods */

    @Override
    public boolean applies(final @NotNull Context context) {
        assert context.getLocation() != null;
        return getModalList().contains(context.getLocation().getBlock().getBiome());
    }


    /* getters and setters */

    @NotNull
    public ModalList<Biome> getModalList() {
        return modalList;
    }

}
