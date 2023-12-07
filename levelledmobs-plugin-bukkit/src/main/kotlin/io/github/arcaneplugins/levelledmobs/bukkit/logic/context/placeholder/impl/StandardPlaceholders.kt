package io.github.arcaneplugins.levelledmobs.bukkit.logic.context.placeholder.impl

import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.placeholder.ContextPlaceholder
import io.github.arcaneplugins.levelledmobs.bukkit.util.EnumUtils.formatEnumConstant
import io.github.arcaneplugins.levelledmobs.bukkit.util.StringReplacer
import io.github.arcaneplugins.levelledmobs.bukkit.util.StringUtils.replaceIfExists
import io.github.arcaneplugins.levelledmobs.bukkit.util.math.MathUtils.round2dp
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity

class StandardPlaceholders : ContextPlaceholder {

    @Suppress("DEPRECATION")
    override fun replace(
        from: String,
        context: Context
    ): String {
        // `from` String Replacer (fromSr)
        val fromSr = StringReplacer(from)

        val player = context.player
        if (player != null) {
            fromSr.replaceIfExists("%player-name%") { player.name }
            fromSr.replaceIfExists("%player-displayname%") { player.displayName }
        }

        if (context.entity != null) {
            fromSr.replaceIfExists("%entity-type%") { context.entity!!.type.name }
            fromSr.replaceIfExists("%entity-type-formatted%") {
                formatEnumConstant(context.entity!!.type)
            }
        }

        if (context.livingEntity != null) {
            val lent: LivingEntity = context.livingEntity!!

            with(fromSr) {
                replaceIfExists("%entity-name%") { lent.customName }
                replaceIfExists("%entity-type%") { lent.type.name }
                replaceIfExists("%entity-biome%") { lent.location.block.biome.name }
                replaceIfExists("%entity-health%") { round2dp(lent.health).toString() }
                replaceIfExists("%entity-health-rounded%") { round2dp(lent.health).toInt() }
                replaceIfExists("%entity-max-health%") { round2dp(lent.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value).toString() }
                replaceIfExists("%entity-max-health-rounded%") { round2dp(lent.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value).toInt() }
                replaceIfExists("%entity-level%") { EntityDataUtil.getLevel(lent, false).toString() }
                replaceIfExists("%entity-min-level%") { EntityDataUtil.getMinLevel(lent, false).toString() }
                replaceIfExists("%entity-max-level%") { EntityDataUtil.getMaxLevel(lent, false).toString() }
                replaceIfExists("%entity-level-ratio%") { EntityDataUtil.getLevelRatio(lent, false).toString() }
                replaceIfExists("%entity-prefix%") { "" } //TODO continue implementing; band-aid
                replaceIfExists("%entity-suffix%") { "" } //TODO continue implementing; band-aid
            }

            val father: LivingEntity? = EntityDataUtil.getFather(lent, false)
            if(father != null) {
                with(fromSr) {
                    replaceIfExists("%father-name%") { father.customName }
                    replaceIfExists("%father-type%") { father.type.name }
                    replaceIfExists("%father-type-formatted%") { formatEnumConstant(father.type) }
                    replaceIfExists("%father-biome%") { father.location.block.biome.name }
                    replaceIfExists("%father-health%") { round2dp(father.health).toString() }
                    replaceIfExists("%father-health-rounded%") { father.health.toInt() }
                    replaceIfExists("%father-level%") { EntityDataUtil.getLevel(father, false).toString() }
                    replaceIfExists("%father-min-level%") { EntityDataUtil.getMinLevel(father, false).toString() }
                    replaceIfExists("%father-max-level%") { EntityDataUtil.getMaxLevel(father, false).toString() }
                    replaceIfExists("%father-level-ratio%") { EntityDataUtil.getLevelRatio(father, false).toString() }
                    replaceIfExists("%father-prefix%") { "" } //TODO continue implementing; band-aid
                    replaceIfExists("%father-suffix%") { "" } //TODO continue implementing; band-aid
                }
            }

            val mother: LivingEntity? = EntityDataUtil.getMother(lent, false)
            if(mother != null) {
                with(fromSr) {
                    replaceIfExists("%mother-name%") { mother.customName }
                    replaceIfExists("%mother-type%") { mother.type.name }
                    replaceIfExists("%mother-type-formatted%") { formatEnumConstant(mother.type) }
                    replaceIfExists("%mother-biome%") { mother.location.block.biome.name }
                    replaceIfExists("%mother-health%") { round2dp(mother.health).toString() }
                    replaceIfExists("%mother-health-rounded%") { mother.health.toInt() }
                    replaceIfExists("%mother-level%") { EntityDataUtil.getLevel(mother, false).toString() }
                    replaceIfExists("%mother-min-level%") { EntityDataUtil.getMinLevel(mother, false).toString() }
                    replaceIfExists("%mother-max-level%") { EntityDataUtil.getMaxLevel(mother, false).toString() }
                    replaceIfExists("%mother-level-ratio%") { EntityDataUtil.getLevelRatio(mother, false).toString() }
                    replaceIfExists("%mother-prefix%") { "" } //TODO continue implementing; band-aid
                    replaceIfExists("%mother-suffix%") { "" } //TODO continue implementing; band-aid
                }
            }
        }

        return fromSr.text
    }

//    private fun getEntityName(
//        entityType: EntityType?,
//        entity: Entity?
//    ): String{
//        val translationHandler: TranslationHandler = LevelledMobs.lmInstance
//            .config.translationHandler
//        return if (entityType != null) {
//            translationHandler.getEntityName(entityType)
//        } else {
//            assert(entity != null)
//            translationHandler.getEntityName(entity!!)
//        }
//    }
// TODO: why is this commented out?
}