package io.github.arcaneplugins.levelledmobs.bukkit.logic.label.impl

import io.github.arcaneplugins.levelledmobs.bukkit.util.ComponentUtils
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.LivingEntity
import io.github.arcaneplugins.levelledmobs.bukkit.util.NmsDefinitions as def

object KyoriNametags {
    fun generateComponent(
        livingEntity: LivingEntity,
        nametag: String
    ): Any{
        val mobKey = livingEntity.type.translationKey()

        // this component holds the component of the mob name and will show the translated name on clients
        // TODO: change this
        val overriddenName: String? = null
        val mobNameComponent: Component = if (overriddenName == null) {
            if (def.useTranslationComponents) {
                Component.translatable(mobKey)
            } else {
                Component.text(livingEntity.name)
            }
        } else {
            LegacyComponentSerializer
                .legacyAmpersand()
                .deserialize(overriddenName)
        }

        // replace placeholders and set the new death message
        val result: Component = if (def.useLegacySerializer) {
            LegacyComponentSerializer
                .legacyAmpersand()
                .deserialize(nametag)
                .replaceText(
                    TextReplacementConfig.builder()
                        .matchLiteral("{Entity-Name}")
                        .replacement(mobNameComponent).build()
                )
        } else {
            //Utils.logger.info("Using MiniMessage");
            def.mm!!
                .deserialize(nametag)
                .replaceText(
                    TextReplacementConfig.builder()
                        .matchLiteral("{Entity-Name}")
                        .replacement(mobNameComponent).build()
                )
        }

        // PaperAdventure.asVanilla(kyoriComponent)
        try {
            return def.method_AsVanilla!!.invoke(def.clazz_PaperAdventure, result)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return ComponentUtils.getEmptyComponent()
    }
}