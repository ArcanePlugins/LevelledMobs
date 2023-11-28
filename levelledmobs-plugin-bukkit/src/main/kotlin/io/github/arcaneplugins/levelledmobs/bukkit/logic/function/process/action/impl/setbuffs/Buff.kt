package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.setbuffs

import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil
import io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugCategory
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler.evaluateExpression
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler.replacePapiAndContextPlaceholders
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.debug
import io.github.arcaneplugins.levelledmobs.bukkit.util.modal.impl.ModalBuffTypeSet
import io.github.arcaneplugins.levelledmobs.bukkit.util.modal.impl.ModalEntityTypeSet
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.LivingEntity
import org.spongepowered.configurate.CommentedConfigurationNode

class Buff() {
    var enabled: Boolean = false
    var affectedEntities: ModalEntityTypeSet? = null
    var buffTypes: ModalBuffTypeSet? = null
    var multiplierFormula: String? = null
    var adjustCurrentHealth: Boolean = false

    constructor(
        enabled: Boolean,
        affectedEntities: ModalEntityTypeSet?,
        buffTypes: ModalBuffTypeSet?,
        multiplierFormula: String?,
        adjustCurrentHealth: Boolean
    ) : this(){
        this.enabled = enabled
        this.affectedEntities = affectedEntities
        this.buffTypes = buffTypes
        this.multiplierFormula = multiplierFormula
        this.adjustCurrentHealth = adjustCurrentHealth
    }

    constructor(
        node: CommentedConfigurationNode
    ) : this() {
        debug(DebugCategory.BUFFS) { "Initializing buff @ " + node.path() }

        this.enabled = node.node("enabled").getBoolean(true)

        if (enabled) {
            this.affectedEntities = ModalEntityTypeSet.parseNode(node.node("affected-entities"))
            this.buffTypes = ModalBuffTypeSet.fromCfgSection(node.node("types"))
            this.multiplierFormula = node.node("multiplier-formula").getString("1.0")
            this.adjustCurrentHealth = node.node("adjust-current-health").getBoolean(true)
        }
        else{
            this.adjustCurrentHealth = true
        }
    }

    companion object{
        val ATTRIBUTE_MODIFIER_PREFIX = "levelledmobs:multiplier."
    }

    fun apply(
        context: Context,
        entity: LivingEntity
    ){
        debug(DebugCategory.BUFFS) { "Applying buffs to " + entity.type + "?= " + enabled }
        if (!enabled) return

        require(EntityDataUtil.isLevelled(entity, true)) { "SetBuffsAction requires a levelled mob context" }

        if (!affectedEntities!!.contains(entity.type)) {
            debug(DebugCategory.BUFFS) {entity.type.toString() + " is not targeted by this buff; returning"}
            return
        }

        for (buffType in buffTypes!!.items) {
            debug(DebugCategory.BUFFS) { "Applying buff type $buffType. Attribute?= ${buffType.representsAttribute}" }

            if(buffType.representsAttribute) {
                // Add attribute buff (NOT custom LM implementation) to entity
                val multiplier = evaluateExpression(
                    replacePapiAndContextPlaceholders(multiplierFormula, context)
                )

                debug(DebugCategory.BUFFS) { "Evaluated multiplier = $multiplier" }
                val attr = buffType.attribute
                val attrInst = entity.getAttribute(attr!!) ?: continue

                val currentVal = attrInst.value
                val isAdjustingHealth = buffType == BuffType.MAX_HEALTH &&
                        adjustCurrentHealth
                debug(DebugCategory.BUFFS) { "Adjusting health: $isAdjustingHealth" }
                var healthRatio = 0.0

                if (isAdjustingHealth) {
                    healthRatio = entity.health / currentVal
                }

                /*
                Bukkit doesn't have Operation.MULTIPLY, so we have to implement it ourselves.
                The formula Bukkit sees is: CurrentValue + (Multiplier * CurrentValue) - CurrentValue
                That simplifies to: Multiplier * CurrentValue
                That's how we essentially make Operation.MULTIPLIER out of Operation.ADD_NUMBER.
                 */

                attrInst.addModifier(
                    AttributeModifier(
                        ATTRIBUTE_MODIFIER_PREFIX + attr.toString().lowercase(),
                        multiplier * currentVal - currentVal,
                        AttributeModifier.Operation.ADD_NUMBER
                    )
                )

                if (isAdjustingHealth) {
                    entity.health = healthRatio * attrInst.value
                }
            }
            else{
                // Set non-attribute buff (custom LM implementation) to entity
                buffType.formulaConsumer!!.accept(entity, multiplierFormula!!)
            }
        }
    }
}