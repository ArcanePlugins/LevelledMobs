package io.github.arcaneplugins.levelledmobs.listeners.paper

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.nametag.KyoriNametags
import io.github.arcaneplugins.levelledmobs.result.NametagResult
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent

/**
 * Holds Paper server specific logic for processing the
 * player death event
 *
 * @author stumper66
 * @since 3.3.0
 */
class PlayerDeathListener {
    private var shouldCancelEvent = false

    fun onPlayerDeathEvent(event: PlayerDeathEvent): Boolean {
        this.shouldCancelEvent = false
        if (event.deathMessage() == null) {
            return true
        }
        if (event.deathMessage() !is TranslatableComponent) {
            return false
        }

        val lmEntity = getPlayersKiller(event)
        val main = LevelledMobs.instance

        if (lmEntity == null) {
            if (main.placeholderApiIntegration != null) {
                main.placeholderApiIntegration!!.putPlayerOrMobDeath(event.entity, null, true)
            }
            if (this.shouldCancelEvent) event.isCancelled = true
            return true
        }

        if (main.placeholderApiIntegration != null) {
            main.placeholderApiIntegration!!.putPlayerOrMobDeath(event.entity, lmEntity, true)
        }
        lmEntity.free()

        if (this.shouldCancelEvent) event.isCancelled = true
        return true
    }

    private fun getPlayersKiller(event: PlayerDeathEvent): LivingEntityWrapper? {
        val entityDamageEvent = event.entity.lastDamageCause
        if (entityDamageEvent == null || entityDamageEvent.isCancelled
            || entityDamageEvent !is EntityDamageByEntityEvent
        ) {
            return null
        }

        val damager = entityDamageEvent.damager
        var killer: LivingEntity? = null

        if (damager is Projectile) {
            if (damager.shooter is LivingEntity) {
                killer = damager.shooter as LivingEntity?
            }
        } else if (damager is LivingEntity) {
            killer = damager
        }

        if (killer == null || killer.name.isEmpty() || killer is Player) {
            return null
        }

        val lmKiller = LivingEntityWrapper.getInstance(killer)
        if (!lmKiller.isLevelled) {
            return lmKiller
        }

        val player = if (LevelledMobs.instance.ver.minecraftVersion >= 1.17) event.player else event.entity

        lmKiller.playerForLevelling = player
        val mobNametag: NametagResult = LevelledMobs.instance.levelManager.getNametag(lmKiller,
            isDeathNametag = true,
            preserveMobName = true
        )
        if (mobNametag.nametagNonNull.isEmpty()) {
            this.shouldCancelEvent = true
            return lmKiller
        }

        if (mobNametag.isNullOrEmpty || "disabled".equals(mobNametag.nametagNonNull, ignoreCase = true)) {
            return lmKiller
        }

        updateDeathMessage(event, mobNametag)

        return lmKiller
    }

    private fun updateDeathMessage(
        event: PlayerDeathEvent,
        nametagResult: NametagResult
    ) {
        if (event.deathMessage() !is TranslatableComponent) {
            // This can happen if another plugin destructively changes the death message.
            return
        }
        val tc = event.deathMessage() as TranslatableComponent

        var mobKey: String? = null
        var itemComp: Component? = null
        for (c in tc.arguments()) {
            if (c is TranslatableComponent) {
                if ("chat.square_brackets" == c.key()) {
                    // this is when the mob was holding a weapon
                    itemComp = c
                } else {
                    mobKey = c.key()
                }
            }
        }

        if (mobKey == null) {
            return
        }
        val main = LevelledMobs.instance
        val mobName: String = nametagResult.nametagNonNull
        val displayNameIndex = mobName.indexOf("{DisplayName}")
        val cs =
            if (main.definitions.getUseLegacySerializer()) LegacyComponentSerializer.legacyAmpersand() else main.definitions.mm!!

        var newCom: Component
        if (nametagResult.hadCustomDeathMessage) {
            val replacementConfig: TextReplacementConfig = TextReplacementConfig.builder().matchLiteral("%player%")
                .replacement(buildPlayerComponent(event.entity)).build()
            newCom = cs.deserialize(mobName)
                .replaceText(replacementConfig)
            if (nametagResult.hadCustomDeathMessage) {
                val displayName: TextReplacementConfig = TextReplacementConfig.builder().matchLiteral("{DisplayName}")
                    .replacement(KyoriNametags.generateDeathMessage(mobKey, nametagResult)).build()
                newCom = newCom.replaceText(displayName)
            }
        } else if (displayNameIndex < 0) {
            // creature-death-nametag in rules.yml doesn't contain %displayname%
            // so we'll just send the whole thing as text
            newCom = Component.translatable(
                tc.key(),
                buildPlayerComponent(event.entity),
                cs.deserialize(mobName)
            )
        } else {
            val leftComp =
                if (displayNameIndex > 0) cs.deserialize(mobName.substring(0, displayNameIndex)) else Component.empty()
            val rightComp =
                if (mobName.length > displayNameIndex + 13) cs.deserialize(mobName.substring(displayNameIndex + 13)) else Component.empty()

            val mobNameComponent =
                if (nametagResult.overriddenName == null) Component.translatable(mobKey) else cs.deserialize(
                    nametagResult.overriddenName!!
                )

            newCom = if (itemComp == null) {
                // mob wasn't using any weapon
                // 2 arguments, example: "death.attack.mob": "%1$s was slain by %2$s"
                Component.translatable(
                    tc.key(),
                    buildPlayerComponent(event.entity),
                    leftComp.append(mobNameComponent)
                ).append(rightComp)
            } else {
                // mob had a weapon and it's details are stored in the itemComp component
                // 3 arguments, example: "death.attack.mob.item": "%1$s was slain by %2$s using %3$s"
                Component.translatable(
                    tc.key(),
                    buildPlayerComponent(event.entity),
                    leftComp.append(mobNameComponent),
                    itemComp
                ).append(rightComp)
            }
        }

        event.deathMessage(newCom)
    }

    private fun buildPlayerComponent(player: Player): Component {
        val playerName =
            if (LevelledMobs.instance.ver.minecraftVersion >= 1.18) player.name() else Component.text(
                player.name
            )
        val hoverEvent = HoverEvent.showEntity(
            Key.key("minecraft"), player.uniqueId, playerName
        )
        val clickEvent = ClickEvent.clickEvent(
            ClickEvent.Action.SUGGEST_COMMAND,
            "/tell " + player.name + " "
        )

        return Component.text(player.name).clickEvent(clickEvent).hoverEvent(hoverEvent)
    }
}