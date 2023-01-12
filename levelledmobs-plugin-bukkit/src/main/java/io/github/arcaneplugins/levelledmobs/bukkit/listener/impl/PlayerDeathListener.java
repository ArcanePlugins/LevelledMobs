package io.github.arcaneplugins.levelledmobs.bukkit.listener.impl;

import io.github.arcaneplugins.arcaneframework.support.SupportChecker;
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.config.translations.Message;
import io.github.arcaneplugins.levelledmobs.bukkit.listener.ListenerWrapper;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Listens to player death events on the server. Provides features such as a LmFunction trigger and
 * custom death messages.
 */
public class PlayerDeathListener extends ListenerWrapper {

    public PlayerDeathListener() {
        super(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void handle(final @Nonnull PlayerDeathEvent event) {
        final Player player = event.getPlayer();
        final Context context = new Context().withPlayer(player);

        attemptAddEntityContext(event, context);
        handleDeathMessage(event, context);

        LogicHandler.runFunctionsWithTriggers(context, "on-player-death");
    }

    private void attemptAddEntityContext(
        final @NotNull PlayerDeathEvent event,
        final @NotNull Context context
    ) {
        final Player player = event.getPlayer();
        final EntityDamageEvent ed = player.getLastDamageCause();

        if(ed == null) return;
        if(!(ed instanceof final EntityDamageByEntityEvent ede)) return;

        context.withEntity(ede.getDamager());
    }

    private void handleDeathMessage(
        final @NotNull PlayerDeathEvent event,
        final @NotNull Context context
    ) {
        // LM4 requires PaperMC (or any derivative) to adjust death messages.
        if(!SupportChecker.PAPERMC_OR_DERIVATIVE) return;

        // If another plugin borks the death message, then we can't adjust it.
        if(!(event.deathMessage() instanceof final TranslatableComponent tcomp)) return;

        // Retrieve required entity context
        if(context.getEntity() == null) return;
        if(!(context.getEntity() instanceof final LivingEntity damager)) return;

        final List<Component> args = new LinkedList<>(tcomp.args());
        int index = -1;
        String mobKey = null;

        for(int i = 0; i < args.size(); i++) {
            final Component c = args.get(i);
            if (!(c instanceof final TranslatableComponent tc2)) continue;

            // this is when the mob was holding a weapon
            if(tc2.key().equals("chat.square_brackets")) continue;

            index = i;
            mobKey = tc2.key();
        }

        if(mobKey == null) return;

        final String deathLabelFormula =
            EntityDataUtil.getDeathLabelFormula(damager, true);

        if(deathLabelFormula == null) return;

        final Component deathLabel = Message.formatMd(
            new String[]{
                LogicHandler.replacePapiAndContextPlaceholders(
                    deathLabelFormula
                        // we need to make sure that this placeholder is not replaced because
                        // we want to retain the translatable component from the original message.
                        .replace("%entity-name%", "%entity-name-TMP%"),
                    context
                )
            }
        );

        final String mobKeyFinalToMakeCompilerHappy = mobKey;

        args.set(
            index,
            deathLabel.replaceText(builder -> builder.match("%entity-name-TMP%")
                .replacement(Component.translatable(mobKeyFinalToMakeCompilerHappy))
            )
        );

        event.deathMessage(tcomp.args(args));
    }
}
