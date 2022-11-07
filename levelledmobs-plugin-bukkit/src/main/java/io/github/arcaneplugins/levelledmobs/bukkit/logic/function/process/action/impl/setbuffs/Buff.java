package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.setbuffs;

import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.util.modal.impl.ModalEntityTypeSet;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import javax.annotation.Nonnull;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import redempt.crunch.Crunch;

public class Buff {

    private final CommentedConfigurationNode node;
    private final boolean enabled;
    private final ModalEntityTypeSet affectedEntities;
    private final EnumSet<BuffType> buffTypes = EnumSet.noneOf(BuffType.class);
    private final String multiplierFormula;
    private final boolean adjustCurrentHealth;

    public Buff(
        final CommentedConfigurationNode node
    ) {
        this.node = node;

        this.enabled = getNode().node("enabled").getBoolean(true);

        this.affectedEntities =
            ModalEntityTypeSet.parseNode(getNode().node("affected-entities"));

        try {
            for(final String buffTypeStr : getNode().node("types")
                .getList(String.class, Collections.emptyList())
            ) {
                try {
                    getBuffTypes().add(BuffType.valueOf(buffTypeStr.toUpperCase(Locale.ROOT)));
                } catch(IllegalArgumentException ex) {
                    throw new IllegalArgumentException("Invalid BuffType value '" +
                        buffTypeStr + "'!");
                }
            }
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }

        this.multiplierFormula =
            getNode().node("multiplier-formula").getString("1.0");

        this.adjustCurrentHealth =
            getNode().node("adjust-current-health").getBoolean(true);
    }

    public void apply(
        final @NotNull Context context,
        final @NotNull LivingEntity entity
    ) {
        if(!isEnabled()) return;

        if(!EntityDataUtil.isLevelled(entity, true)) {
            throw new IllegalArgumentException("SetBuffsAction requires a levelled mob context");
        }

        if(!getAffectedEntities().contains(entity.getType())) {
            return;
        }

        final double multiplier = Crunch.evaluateExpression(
            LogicHandler.replacePapiAndContextPlaceholders(getMultiplierFormula(), context)
        );

        for(final BuffType buffType : getBuffTypes()) {
            if(buffType.representsAttribute()) {
                // Add attribute buff (NOT custom LM implementation) to entity
                final Attribute attr = buffType.getAttribute();
                final AttributeInstance attrInst = entity.getAttribute(attr);
                if(attrInst == null) continue;

                final double currentVal = attrInst.getValue();

                final boolean isAdjustingHealth = buffType == BuffType.MAX_HEALTH &&
                    shouldAdjustCurrentHealth();
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
                buffType.getCustomMultiplierConsumer().accept(entity, multiplier);
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
    public EnumSet<BuffType> getBuffTypes() {
        return buffTypes;
    }
}