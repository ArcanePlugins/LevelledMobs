package io.github.arcaneplugins.levelledmobs.nametag

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.nametag.ComponentUtils.getEmptyComponent
import io.github.arcaneplugins.levelledmobs.result.NametagResult
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.LivingEntity

/**
 * Holds logic used to send nametags using Kyori
 * which is usually only found on Paper servers
 *
 * @author stumper66
 * @since 3.9.3
 */
object KyoriNametags {
    private val def = LevelledMobs.instance.definitions

    fun generateComponent(
        livingEntity: LivingEntity,
        nametagResult: NametagResult
    ): Any {
        val nametag = nametagResult.nametagNonNull
        val mobKey = livingEntity.type.translationKey()

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
                .deserialize(nametagResult.overriddenName!!)
        }

        // replace placeholders and set the new death message
        val result = if (def.getUseLegacySerializer()) {
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
            def.mm!!
                .deserialize(nametag)
                .replaceText(
                    TextReplacementConfig.builder()
                        .matchLiteral("{DisplayName}")
                        .replacement(mobNameComponent).build()
                )
        }

        // PaperAdventure.asVanilla(kyoriComponent)
        try {
            return def.methodAsVanilla!!.invoke(def.clazzPaperAdventure, result)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return getEmptyComponent()
    }

    fun generateDeathMessage(
        mobKey: String,
        nametagResult: NametagResult
    ): Component {
        val lmEntity = LivingEntityWrapper.getInstance(nametagResult.killerMob!!)
        val nametag: String = LevelledMobs.instance.levelManager.replaceStringPlaceholders(
            nametagResult.customDeathMessage!!, lmEntity, false, null, true
        )
        lmEntity.free()

        // this component holds the component of the mob name and will show the translated name on clients
        val mobNameComponent: Component = if (nametagResult.overriddenName == null) {
            Component.translatable(mobKey)
        } else {
            LegacyComponentSerializer
                .legacyAmpersand()
                .deserialize(nametagResult.overriddenName!!)
        }

        // replace placeholders and set the new death message
        val result = if (def.getUseLegacySerializer()) {
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
            def.mm!!
                .deserialize(nametag)
                .replaceText(
                    TextReplacementConfig.builder()
                        .matchLiteral("{DisplayName}")
                        .replacement(mobNameComponent).build()
                )
        }

        return result
    }
}