package io.github.arcaneplugins.levelledmobs.bukkit.logic.context.placeholder.impl

import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.placeholder.ContextPlaceholder
import io.github.arcaneplugins.levelledmobs.bukkit.util.EnumUtils.formatEnumConstant
import io.github.arcaneplugins.levelledmobs.bukkit.util.StringReplacer
import io.github.arcaneplugins.levelledmobs.bukkit.util.math.MathUtils.round2dp
import org.bukkit.attribute.Attribute

class StandardPlaceholders: ContextPlaceholder {
    @Suppress("DEPRECATION")
    override fun replace(
        from: String,
        context: Context
    ): String {
        val str = StringReplacer(from)

        val player = context.player
        if (player != null) {
            str.replaceIfExists( "%player-name%") { player.name }
            str.replaceIfExists( "%player-displayname%") { player.displayName }
        }

        if (context.entity != null) {
            str.replaceIfExists( "%entity-type%") { context.entity!!.type.name }
            str.replaceIfExists( "%entity-type-formatted%") {
                formatEnumConstant(context.entity!!.type)
            }
        }

        if (context.livingEntity != null ){
            val lent = context.livingEntity!!
            val father = EntityDataUtil.getFather(lent, false)
            val mother = EntityDataUtil.getMother(lent, false)

            str.replaceIfExists( "%entity-name%") { lent.customName }
            str.replaceIfExists( "%entity-type%") { lent.type.name }
            str.replaceIfExists( "%entity-biome%") { lent.location.block.biome.name }
            str.replaceIfExists( "%entity-health%") { round2dp(lent.health).toString() }
            str.replaceIfExists( "%entity-health-rounded%") { lent.health.toInt() }
            str.replaceIfExists( "%entity-max-health%") { round2dp(lent.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value).toString() }
            str.replaceIfExists( "%entity-level%") { EntityDataUtil.getLevel(lent, false).toString() }
            str.replaceIfExists( "%entity-min-level%") { EntityDataUtil.getMinLevel(lent, false).toString() }
            str.replaceIfExists( "%entity-max-level%") { EntityDataUtil.getMaxLevel(lent, false).toString() }
            str.replaceIfExists( "%entity-level-ratio%") { EntityDataUtil.getLevelRatio(lent, false).toString() }
            str.replaceIfExists( "%entity-prefix%") { "" } //TODO continue implementing; band-aid
            str.replaceIfExists( "%entity-suffix%") { "" } //TODO continue implementing; band-aid

            str.replaceIfExists( "%father-name%") { father!!.customName }
            str.replaceIfExists( "%father-type%") { father!!.type.name }
            str.replaceIfExists( "%father-type-formatted%") { formatEnumConstant(father!!.type) }
            str.replaceIfExists( "%father-biome%") { father!!.location.block.biome.name }
            str.replaceIfExists( "%father-health%") { round2dp(father!!.health).toString() }
            str.replaceIfExists( "%father-health-rounded%") { father!!.health.toInt() }
            str.replaceIfExists( "%father-level%") { EntityDataUtil.getLevel(father!!, false).toString() }
            str.replaceIfExists( "%father-min-level%") { EntityDataUtil.getMinLevel(father!!, false).toString() }
            str.replaceIfExists( "%father-max-level%") { EntityDataUtil.getMaxLevel(father!!, false).toString() }
            str.replaceIfExists( "%father-level-ratio%") { EntityDataUtil.getLevelRatio(father!!, false).toString() }
            str.replaceIfExists( "%father-prefix%") { "" } //TODO continue implementing; band-aid
            str.replaceIfExists( "%father-suffix%") { "" } //TODO continue implementing; band-aid

            str.replaceIfExists( "%mother-name%") { mother!!.customName }
            str.replaceIfExists( "%mother-type%") { mother!!.type.name }
            str.replaceIfExists( "%mother-type-formatted%") { formatEnumConstant(mother!!.type) }
            str.replaceIfExists( "%mother-biome%") { mother!!.location.block.biome.name }
            str.replaceIfExists( "%mother-health%") { round2dp(mother!!.health).toString() }
            str.replaceIfExists( "%mother-health-rounded%") { mother!!.health.toInt() }
            str.replaceIfExists( "%mother-level%") { EntityDataUtil.getLevel(mother!!, false).toString() }
            str.replaceIfExists( "%mother-min-level%") { EntityDataUtil.getMinLevel(mother!!, false).toString() }
            str.replaceIfExists( "%mother-max-level%") { EntityDataUtil.getMaxLevel(mother!!, false).toString() }
            str.replaceIfExists( "%mother-level-ratio%") { EntityDataUtil.getLevelRatio(mother!!, false).toString() }
            str.replaceIfExists( "%mother-prefix%") { "" } //TODO continue implementing; band-aid
            str.replaceIfExists( "%mother-suffix%") { "" } //TODO continue implementing; band-aid
        }

        return str.text
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
}