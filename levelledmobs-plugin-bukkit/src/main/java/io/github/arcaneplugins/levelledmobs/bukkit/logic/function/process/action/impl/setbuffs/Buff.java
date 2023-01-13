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

public class Buff {

    public static final String ATTRIBUTE_MODIFIER_PREFIX = "levelledmobs:multiplier.";

    private final boolean enabled;
    private final ModalEntityTypeSet affectedEntities;
    private final ModalBuffTypeSet buffTypes;
    private final String multiplierFormula;
    private final boolean adjustCurrentHealth;

    @SuppressWarnings("unused")
    public Buff(
        final boolean enabled,
        final ModalEntityTypeSet affectedEntities,
        final ModalBuffTypeSet buffTypes,
        final String multiplierFormula,
        final boolean adjustCurrentHealth
    ) {
        this.enabled = enabled;
        this.affectedEntities = affectedEntities;
        this.buffTypes = buffTypes;
        this.multiplierFormula = multiplierFormula;
        this.adjustCurrentHealth = adjustCurrentHealth;
    }

    public Buff(
        final CommentedConfigurationNode node
    ) {
        Log.debug(BUFFS, () -> "Initializing buff @ " + node.path());

        this.enabled = node.node("enabled").getBoolean(true);

        if(!isEnabled()) {
            this.affectedEntities = null;
            this.buffTypes = null;
            this.multiplierFormula = null;
            this.adjustCurrentHealth = true;
            return;
        }

        this.affectedEntities =
            ModalEntityTypeSet.parseNode(node.node("affected-entities"));

        buffTypes = ModalBuffTypeSet.fromCfgSection(node.node("types"));

        this.multiplierFormula =
            node.node("multiplier-formula").getString("1.0");

        this.adjustCurrentHealth =
            node.node("adjust-current-health").getBoolean(true);
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

                final double multiplier = LogicHandler.evaluateExpression(
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
                    ATTRIBUTE_MODIFIER_PREFIX + attr.toString().toLowerCase(Locale.ROOT),
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