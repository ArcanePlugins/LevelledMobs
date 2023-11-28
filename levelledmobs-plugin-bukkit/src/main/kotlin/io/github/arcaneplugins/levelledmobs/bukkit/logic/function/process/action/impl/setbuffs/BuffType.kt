package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.setbuffs

import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil.setCreeperBlastRadiusMultiplierFormula
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil.setExpDropMultiplierFormula
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil.setItemDropMultiplier
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil.setShieldBreakerMultiplierFormula
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler.evaluateExpression
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler.replacePapiAndContextPlaceholders
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Creeper
import org.bukkit.entity.LivingEntity
import java.util.function.BiConsumer

enum class BuffType {
    ARMOR_TOUGHNESS(Attribute.GENERIC_ARMOR_TOUGHNESS),
    ATTACK_DAMAGE(Attribute.GENERIC_ATTACK_DAMAGE),
    ATTACK_KNOCKBACK(Attribute.GENERIC_ATTACK_KNOCKBACK),
    CREEPER_BLAST_DAMAGE(
        BiConsumer { lent: LivingEntity, formula: String ->
            if (lent !is Creeper) return@BiConsumer
            val creeper = lent

            setCreeperBlastRadiusMultiplierFormula(lent, formula, true)
            val multiplier = evaluateExpression(
                replacePapiAndContextPlaceholders(
                    formula, Context(creeper)
                )
            )
            creeper.explosionRadius = (creeper.explosionRadius * multiplier).toInt()
        }
    ),
    EXP_DROP(
        BiConsumer { lent: LivingEntity, formula: String ->
            setExpDropMultiplierFormula(lent, formula, true)
        }
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
        BiConsumer { lent: LivingEntity, formula: String ->
            setItemDropMultiplier(lent, formula, true)
        }
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
        BiConsumer { lent: LivingEntity, formula: String -> }
    ),
    //Not Implemented; see GitHub Issue
    SHIELD_BREAKER(
        BiConsumer { lent: LivingEntity, formula: String ->
            setShieldBreakerMultiplierFormula(lent, formula, true)
        }
    ),
    ZOMBIE_SPAWN_REINFORCEMENTS(
        Attribute.ZOMBIE_SPAWN_REINFORCEMENTS
    );

    val attribute: Attribute?
    val formulaConsumer: BiConsumer<LivingEntity, String>?
    val representsAttribute: Boolean

    constructor(attribute: Attribute){
        this.attribute = attribute
        this.formulaConsumer = null
        this.representsAttribute = true
    }

    constructor(
        formulaConsumer: BiConsumer<LivingEntity, String>
    ){
        this.attribute = null
        this.formulaConsumer = formulaConsumer
        this.representsAttribute = false
    }

}