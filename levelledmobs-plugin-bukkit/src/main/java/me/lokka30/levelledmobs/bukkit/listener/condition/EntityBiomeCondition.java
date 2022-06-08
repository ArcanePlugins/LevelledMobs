package me.lokka30.levelledmobs.bukkit.listener.condition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import me.lokka30.levelledmobs.bukkit.logic.function.process.Process;
import me.lokka30.levelledmobs.bukkit.logic.function.process.condition.Condition;
import me.lokka30.levelledmobs.bukkit.util.Log;
import me.lokka30.levelledmobs.bukkit.util.modal.ModalCollection.Type;
import me.lokka30.levelledmobs.bukkit.util.modal.ModalList;
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

    public EntityBiomeCondition(Process process, final CommentedConfigurationNode node) {
        super(process, node);

        final Type type;
        final List<String> biomeTypesStr;

        try {
            if (getConditionNode().hasChild("in-list")) {
                type = Type.INCLUSIVE;
                biomeTypesStr = getConditionNode().node("in-list").getList(
                    String.class, Collections.emptyList()
                );
            } else if (getConditionNode().hasChild("not-in-list")) {
                type = Type.EXCLUSIVE;
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

        final List<Biome> biomeTypes = new ArrayList<>();

        for (var entityTypeStr : biomeTypesStr) {
            try {
                biomeTypes.add(Biome.valueOf(entityTypeStr));
            } catch (IllegalArgumentException ignored) {
            }
        }

        this.modalList = new ModalList<>(biomeTypes, type);
    }

    /* methods */

    @Override
    public boolean applies(Context context) {
        assert context.getLocation() != null;
        return getModalList().contains(context.getLocation().getBlock().getBiome());
    }


    /* getters and setters */

    @NotNull
    public ModalList<Biome> getModalList() {
        return modalList;
    }

}
