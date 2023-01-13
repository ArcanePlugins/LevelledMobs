package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.setbuffs;

import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import java.util.Objects;
import java.util.function.BiConsumer;
import javax.annotation.Nonnull;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public enum BuffType {

    ARMOR_TOUGHNESS(
        Attribute.GENERIC_ARMOR_TOUGHNESS
    ),

    ATTACK_DAMAGE(
        Attribute.GENERIC_ATTACK_DAMAGE
    ),

    ATTACK_KNOCKBACK(
        Attribute.GENERIC_ATTACK_KNOCKBACK
    ),

    CREEPER_BLAST_DAMAGE(
        (lent, formula) -> {
            if(!(lent instanceof Creeper creeper)) return;

            InternalEntityDataUtil
                .setCreeperBlastRadiusMultiplierFormula(lent, formula, true);

            final double multiplier = LogicHandler.evaluateExpression(
                LogicHandler.replacePapiAndContextPlaceholders(
                    formula, new Context().withEntity(creeper)
                )
            );

            creeper.setExplosionRadius((int) (creeper.getExplosionRadius() * multiplier));
        }
    ),

    EXP_DROP(
        (lent, formula) -> InternalEntityDataUtil
            .setExpDropMultiplierFormula(lent, formula, true)
    ),

    FLYING_SPEED(
        Attribute.GENERIC_FLYING_SPEED
    ),

    FOLLOW_RANGE(
        Attribute.GENERIC_FOLLOW_RANGE
    ),

    HORSE_JUMP_STRENGTH(
        Attribute.HORSE_JUMP_STRENGTH
    ),

    ITEM_DROP(
        (lent, formula) -> InternalEntityDataUtil
            .setItemDropMultiplier(lent, formula, true)
    ),

    KNOCKBACK_RESISTANCE(
        Attribute.GENERIC_KNOCKBACK_RESISTANCE
    ),

    MAX_HEALTH(
        Attribute.GENERIC_MAX_HEALTH
    ),

    MOVEMENT_SPEED(
        Attribute.GENERIC_MOVEMENT_SPEED
    ),

    //TODO Not Implemented
    RANGED_ATTACK_DAMAGE(
        (lent, formula) -> {}
    ),

    //Not Implemented; see GitHub Issue
    SHIELD_BREAKER(
        (lent, formula) -> InternalEntityDataUtil
            .setShieldBreakerMultiplierFormula(lent, formula, true)
    ),

    ZOMBIE_SPAWN_REINFORCEMENTS(
        Attribute.ZOMBIE_SPAWN_REINFORCEMENTS
    );

    private final @Nullable Attribute attribute;
    private final @Nullable BiConsumer<LivingEntity, String> formulaConsumer;
    private final boolean representsAttribute;

    BuffType(
        final @Nonnull Attribute attribute
    ) {
        this.attribute = attribute;
        this.formulaConsumer = null;
        this.representsAttribute = true;
    }

    BuffType(
        final @Nonnull BiConsumer<LivingEntity, String> formulaConsumer
    ) {
        this.attribute = null;
        this.formulaConsumer = formulaConsumer;
        this.representsAttribute = false;
    }

    /**
     * First, ensure {@link BuffType#representsAttribute()} returns {@code true}.
     *
     * @return Attribute-type representation
     */
    public @Nonnull Attribute getAttribute() {
        return Objects.requireNonNull(attribute, "attribute");
    }

    /**
     * First, ensure {@link BuffType#representsAttribute()} returns {@code false}.
     *
     * @return Custom-type multiplier consumer representation
     */
    public @Nonnull BiConsumer<LivingEntity, String> getCustomFormulaConsumer() {
        return Objects.requireNonNull(formulaConsumer, "formulaConsumer");
    }

    /**
     * @return whether the Buff represents a stanard Attribute or a custom LM implementation
     */
    public boolean representsAttribute() {
        return representsAttribute;
    }
}
