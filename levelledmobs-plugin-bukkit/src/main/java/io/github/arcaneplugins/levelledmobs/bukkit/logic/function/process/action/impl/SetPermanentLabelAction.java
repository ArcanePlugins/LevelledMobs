package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl;

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.label.LabelHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.label.LabelRegistry;
import java.util.Map;
import javax.annotation.Nonnull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class SetPermanentLabelAction extends Action {

    public static final String LABEL_ID = "Permanent";

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
            if(!EntityDataUtil.isLevelled(lent, false)) return;

            final Component comp = generateLabelComponents(lent, context);

            try {
                lent.customName(comp);
            } catch(NoSuchMethodError er) {
                //TODO check if this works on spigot
                //noinspection deprecation
                lent.setCustomName(LegacyComponentSerializer.legacySection().serialize(comp));
            }

            lent.setCustomNameVisible(isAlwaysVisible());
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
            update(lent, context);
        }

        public static boolean isAlwaysVisible() {
            return LevelledMobs.getInstance().getConfigHandler().getSettingsCfg().getRoot()
                .node("advanced", "set-permanent-label-action", "always-visible")
                .getBoolean(true);
        }

    }
}
