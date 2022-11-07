package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.setbuffs;

import static io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugCategory.BUFFS;

import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import io.github.arcaneplugins.levelledmobs.bukkit.util.modal.impl.ModalBuffTypeSet;
import io.github.arcaneplugins.levelledmobs.bukkit.util.modal.impl.ModalEntityTypeSet;
import java.util.Locale;
import javax.annotation.Nonnull;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import redempt.crunch.Crunch;

public class Buff {

    private final CommentedConfigurationNode node;
    private final boolean enabled;
    private final ModalEntityTypeSet affectedEntities;
    private final ModalBuffTypeSet buffTypes;
    private final String multiplierFormula;
    private final boolean adjustCurrentHealth;

    public Buff(
        final CommentedConfigurationNode node
    ) {
        Log.debug(BUFFS, () -> "Initializing buff @ " + node.path());

        this.node = node;

        this.enabled = getNode().node("enabled").getBoolean(true);

        if(!isEnabled()) {
            this.affectedEntities = null;
            this.buffTypes = null;
            this.multiplierFormula = null;
            this.adjustCurrentHealth = true;
            return;
        }

        this.affectedEntities =
            ModalEntityTypeSet.parseNode(getNode().node("affected-entities"));

        buffTypes = ModalBuffTypeSet.fromCfgSection(getNode().node("types"));

        this.multiplierFormula =
            getNode().node("multiplier-formula").getString("1.0");

        this.adjustCurrentHealth =
            getNode().node("adjust-current-health").getBoolean(true);
    }

    public void apply(
        final @NotNull Context context,
        final @NotNull LivingEntity entity
    ) {
        Log.debug(BUFFS, () -> "Applying buffs to " + entity.getType() + "?= " + isEnabled());
        if(!isEnabled()) return;

        if(!EntityDataUtil.isLevelled(entity, true)) {
            throw new IllegalArgumentException("SetBuffsAction requires a levelled mob context");
        }

        if(!getAffectedEntities().contains(entity.getType())) {
            Log.debug(BUFFS, () -> entity.getType() + "is not targeted by this buff; returning");
            return;
        }

        for(final BuffType buffType : getBuffTypes().getItems()) {
            Log.debug(BUFFS, () -> "Applying buff type " + buffType +
                ". Attribute?= " + buffType.representsAttribute());

            if(buffType.representsAttribute()) {
                // Add attribute buff (NOT custom LM implementation) to entity

                final double multiplier = Crunch.evaluateExpression(
                    LogicHandler.replacePapiAndContextPlaceholders(getMultiplierFormula(), context)
                );
                Log.debug(BUFFS, () -> "Evaluated multiplier = " + multiplier);

                final Attribute attr = buffType.getAttribute();
                final AttributeInstance attrInst = entity.getAttribute(attr);
                if(attrInst == null) continue;

                final double currentVal = attrInst.getValue();

                final boolean isAdjustingHealth = buffType == BuffType.MAX_HEALTH &&
                    shouldAdjustCurrentHealth();
                Log.debug(BUFFS, () -> "Adjusting health: " + isAdjustingHealth);
                double healthRatio = 0.0d;

                if(isAdjustingHealth) {
                    healthRatio = entity.getHealth() / currentVal;
                }

                /*
                Bukkit doesn't have Operation.MULTIPLY, so we have to implement it ourselves.
                The formula Bukkit sees is: CurrentValue + (Multiplier * CurrentValue) - CurrentValue
                That simplifies to: Multiplier * CurrentValue
                That's how we essentially make Operation.MULTIPLIER out of Operation.ADD_NUMBER.
                 */

                attrInst.addModifier(new AttributeModifier(
                    "levelledmobs:multiplier." + attr.toString().toLowerCase(Locale.ROOT),
                    (multiplier * currentVal) - currentVal,
                    Operation.ADD_NUMBER
                ));

                if(isAdjustingHealth) {
                    entity.setHealth(healthRatio * attrInst.getValue());
                }
            } else {
                // Set non-attribute buff (custom LM implementation) to entity
                buffType.getCustomFormulaConsumer().accept(entity, getMultiplierFormula());
            }
        }
    }

    public CommentedConfigurationNode getNode() {
        return node;
    }

    public boolean isEnabled() {
        return enabled;
    }

    private ModalEntityTypeSet getAffectedEntities() {
        return affectedEntities;
    }

    public String getMultiplierFormula() {
        return multiplierFormula;
    }

    public boolean shouldAdjustCurrentHealth() {
        return adjustCurrentHealth;
    }

    @Nonnull
    public ModalBuffTypeSet getBuffTypes() {
        return buffTypes;
    }
}