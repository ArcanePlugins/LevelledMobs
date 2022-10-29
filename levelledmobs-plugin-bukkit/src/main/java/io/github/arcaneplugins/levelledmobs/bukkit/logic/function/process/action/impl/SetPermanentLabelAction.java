package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl;

import java.util.Map;
import javax.annotation.Nonnull;
import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.label.LabelHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.label.LabelRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class SetPermanentLabelAction extends Action {

    private static final String LABEL_ID = "Permanent";

    private final String formula;

    public SetPermanentLabelAction(
        @NotNull Process parentProcess,
        @NotNull CommentedConfigurationNode actionNode
    ) {
        super(parentProcess, actionNode);

        this.formula = getActionNode().node("formula").getString("");
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

        PermanentLabelHandler.INSTANCE.update(lent, context);
    }

    @Nonnull
    public String getFormula() {
        return formula;
    }

    @SuppressWarnings("unused")
    public static class PermanentLabelHandler extends LabelHandler {

        public static final PermanentLabelHandler INSTANCE = new PermanentLabelHandler();

        static {
            LabelRegistry.getLabelHandlers().add(INSTANCE);
        }

        private PermanentLabelHandler() {
            super(LABEL_ID);
        }

        @Override
        public void update(
            @NotNull LivingEntity lent,
            @Nonnull Context context
        ) {
            final String nametag = generateLabelLegacy(lent, context);
            for (final Player player : Bukkit.getOnlinePlayers()){
                LevelledMobs.getInstance().getNametagSender().sendNametag(lent, player, nametag);
            }
        }

        public static boolean isAlwaysVisible() {
            return LevelledMobs.getInstance().getConfigHandler().getSettingsCfg().getRoot()
                .node("advanced", "set-permanent-label-action", "always-visible")
                .getBoolean(true);
        }

    }
}
