package io.github.arcaneplugins.levelledmobs.plugin.bukkit.nametag

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.LevelledMobs
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.misc.NametagResult
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.LivingEntity

class KyoriNametags {

    companion object{
        fun generateComponent(
            livingEntity: LivingEntity,
            nametagResult: NametagResult
        ) : Any{
            val nametag = nametagResult.nametagNonNull
            val mobKey = livingEntity.type.translationKey()
            val def = LevelledMobs.lmInstance.definitions

            // this component holds the component of the mob name and will show the translated name on clients
            val mobNameComponent: Component = if (nametagResult.overriddenName == null) {
                if (def.useTranslationComponents) {
                    Component.translatable(mobKey)
                } else {
                    Component.text(livingEntity.name)
                }
            } else {
                LegacyComponentSerializer
                    .legacyAmpersand()
                    .deserialize(nametagResult.overriddenName)
            }

            // replace placeholders and set the new death message
            val result: Component = if (def.useLegacySerializer) {
                LegacyComponentSerializer
                    .legacyAmpersand()
                    .deserialize(nametag)
                    .replaceText(
                        TextReplacementConfig.builder()
                            .matchLiteral("{DisplayName}")
                            .replacement(mobNameComponent).build()
                    )
            } else {
                //Utils.logger.info("Using MiniMessage");
                val mm: MiniMessage = def.miniMessage as MiniMessage
                mm
                    .deserialize(nametag)
                    .replaceText(
                        TextReplacementConfig.builder()
                            .matchLiteral("{DisplayName}")
                            .replacement(mobNameComponent).build()
                    )
            }

            // PaperAdventure.asVanilla(kyoriComponent)
            return def.method_AsVanilla!!.invoke(def.clazz_PaperAdventure, result)
        }
    }
}