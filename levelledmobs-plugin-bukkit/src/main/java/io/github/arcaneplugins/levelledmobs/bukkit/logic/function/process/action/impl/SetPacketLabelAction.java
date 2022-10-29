package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl;

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.label.LabelHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.label.LabelRegistry;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.label.type.packet.VisibilityMethod;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nonnull;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public class SetPacketLabelAction extends Action {

    private static final String LABEL_ID = "Permanent";

    private final String formula;
    private final EnumSet<VisibilityMethod> visibilityMethods = EnumSet
        .noneOf(VisibilityMethod.class);
    private final float visibilityDuration;

    public SetPacketLabelAction(
        @NotNull Process parentProcess,
        @NotNull CommentedConfigurationNode actionNode
    ) {
        super(parentProcess, actionNode);

        this.formula = getActionNode().node("formula").getString("");

        final List<String> visibilityMethodsStr;
        try {
           visibilityMethodsStr = getActionNode().node("visibility-methods")
                .getList(String.class, Collections.emptyList());
        } catch (SerializationException ex) {
            throw new RuntimeException(ex);
        }
        for(final String visibilityMethodStr : visibilityMethodsStr) {
            try {
                getVisibilityMethods().add(VisibilityMethod.valueOf(
                    visibilityMethodStr.toUpperCase(Locale.ROOT)));
            } catch(IllegalArgumentException ex) {
                throw ex;
            }
        }

        this.visibilityDuration = getActionNode().node("visibility-duration")
            .getFloat(5.0f);
    }

    @Override
    public void run(Context context) {

        final Entity ent = context.getEntity();

        if(ent == null) return;
        if(!(ent instanceof LivingEntity lent)) return;

        final Map<String, String> labelHandlerFormulaMap = InternalEntityDataUtil
            .getLabelHandlerFormulaMap(lent, false);

        labelHandlerFormulaMap.put(LABEL_ID, getFormula());

        InternalEntityDataUtil
            .setLabelHandlerFormulaMap(lent, labelHandlerFormulaMap, false);

        PacketLabelHandler.INSTANCE.update(lent, context);
    }

    /*

     */

    public EnumSet<VisibilityMethod> getVisibilityMethods() {
        return visibilityMethods;
    }

    public String getFormula() {
        return formula;
    }

    public float getVisibilityDuration() {
        return visibilityDuration;
    }

    public static class PacketLabelHandler extends LabelHandler {

        public static final PacketLabelHandler INSTANCE = new PacketLabelHandler();

        static {
            LabelRegistry.getLabelHandlers().add(INSTANCE);
        }

        private PacketLabelHandler() {
            super(LABEL_ID);
        }

        @Override
        public void update(
            @NotNull LivingEntity lent,
            @Nonnull Context context
        ) {
            deferEntityUpdate(lent, context);
        }

        @Override
        public void update(
            @NotNull Player player,
            @NotNull Context context
        ) {
            deferPlayerUpdate(player, context);
        }

        @Override
        public void update(
            @NotNull LivingEntity lent,
            @NotNull Player player,
            @NotNull Context context
        ) {
            if(!EntityDataUtil.isLevelled(lent, false)) return;

            LevelledMobs.getInstance().getNametagSender()
                .sendNametag(lent, player, generateLabelLegacy(lent, context));

            //TODO: factor in visibility methods
            //TODO: factor in visibility duration
        }

    }
}
