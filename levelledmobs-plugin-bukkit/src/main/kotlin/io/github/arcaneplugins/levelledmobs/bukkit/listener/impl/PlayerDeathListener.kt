package io.github.arcaneplugins.levelledmobs.bukkit.listener.impl

import io.github.arcaneplugins.arcaneframework.support.SupportChecker
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil
import io.github.arcaneplugins.levelledmobs.bukkit.config.translations.Message
import io.github.arcaneplugins.levelledmobs.bukkit.listener.ListenerWrapper
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler.replacePapiAndContextPlaceholders
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler.runFunctionsWithTriggers
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.TranslatableComponent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent

class PlayerDeathListener : ListenerWrapper(true) {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun handle(event: PlayerDeathEvent){
        val player = event.player
        val context = Context().withPlayer(player)

        attemptAddEntityContext(event, context)
        handleDeathMessage(event, context)

        runFunctionsWithTriggers(context, mutableListOf("on-player-death"))
    }

    private fun attemptAddEntityContext(
        event: PlayerDeathEvent,
        context: Context
    ){
        val player = event.player
        val ed = player.lastDamageCause as? EntityDamageByEntityEvent ?: return

        context.withEntity(ed.damager)
    }

    private fun handleDeathMessage(
        event: PlayerDeathEvent,
        context: Context
    ){
        // LM4 requires PaperMC (or any derivative) to adjust death messages.
        if (!SupportChecker.PAPERMC_OR_DERIVATIVE) return

        // If another plugin borks the death message, then we can't adjust it.
        if (event.deathMessage() !is TranslatableComponent) return
        val tcomp = event.deathMessage() as TranslatableComponent

        // Retrieve required entity context
        if (context.livingEntity == null) return

        val args = tcomp.args()
        var index = -1
        var mobKey: String? = null

        for (i in 0..args.size) {
            val c = args[i]
            if (c !is TranslatableComponent) continue

            // this is when the mob was holding a weapon
            if (c.key() == "chat.square_brackets") continue

            index = i
            mobKey = c.key()
        }

        if (mobKey == null) return

        val deathLabelFormula = EntityDataUtil.getDeathLabelFormula(context.livingEntity!!, true) ?: return

        val deathLabel = Message.formatMd(
            mutableListOf(
                replacePapiAndContextPlaceholders(
                    deathLabelFormula // we need to make sure that this placeholder is not replaced because
                        // we want to retain the translatable component from the original message.
                        .replace("%entity-name%", "%entity-name-TMP%"),
                    context
                )
            )
        )

        val mobKeyFinalToMakeCompilerHappy: String = mobKey

        args[index] = deathLabel.replaceText { builder: TextReplacementConfig.Builder ->
            builder.match("%entity-name-TMP%")
                .replacement(Component.translatable(mobKeyFinalToMakeCompilerHappy))
        }

        event.deathMessage(tcomp.args(args))
    }
}