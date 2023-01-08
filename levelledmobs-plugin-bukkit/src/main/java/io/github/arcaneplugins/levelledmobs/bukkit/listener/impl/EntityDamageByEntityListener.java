package io.github.arcaneplugins.levelledmobs.bukkit.listener.impl;

import de.themoep.minedown.adventure.MineDown;
import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugCategory;
import io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.listener.ListenerWrapper;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.setbuffs.Buff;
import io.github.arcaneplugins.levelledmobs.bukkit.util.EnumUtils;
import io.github.arcaneplugins.levelledmobs.bukkit.util.PlayerUtils;
import io.github.arcaneplugins.levelledmobs.bukkit.util.PlayerUtils.FoundItemInHandResult;
import java.text.DecimalFormat;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class EntityDamageByEntityListener extends ListenerWrapper {

    /**
     * Create a new listener.
     */
    public EntityDamageByEntityListener() {
        super(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
        final Entity defender = event.getEntity();
        final Entity attacker = event.getDamager(); //TODO see TODO below. need an attacker context.

        handleShieldBreaker(event);
        handleEntityInspector(event);

        LogicHandler.runFunctionsWithTriggers(
            new Context().withEntity(defender), "on-entity-damage-by-entity"
            //TODO also have 'attacker' context
        );
    }

    private void handleEntityInspector(
        final EntityDamageByEntityEvent event
    ) {
        if (!DebugHandler.isCategoryEnabled(DebugCategory.ENTITY_INSPECTOR) ||
            !(event.getEntity() instanceof final LivingEntity enInspected) ||
            !(event.getDamager() instanceof final Player plInspector) ||
            !plInspector.hasPermission("levelledmobs.debug") ||
            !EntityDataUtil.isLevelled(enInspected, true)
        ) {
            return;
        }

        final Context context = new Context()
            .withEntity(enInspected)
            .withLocation(enInspected.getLocation())
            .withEntityType(enInspected.getType())
            .withWorld(enInspected.getWorld())
            .withPlayer(plInspector);

        final Consumer<String> messenger = (message) -> plInspector.sendMessage(
            MineDown.parse(LogicHandler.replacePapiAndContextPlaceholders(message, context))
        );

        final DecimalFormat to4dp = new DecimalFormat("#,##0.0###");

        new BukkitRunnable() {
            @Override
            public void run() {
                messenger.accept(
                    "&8┌ &f&lLM:&7 Inspecting &bLvl.%entity-level% %entity-type-formatted%"
                );
                messenger.accept("&8 • &7Name:&f %entity-name%");
                messenger.accept("&8 • &7Health:&f %entity-health-rounded%&8/&f%entity-max-health-rounded% HP");
                messenger.accept(
                    "&8 • &7Min Level: &f%s&7; Max Level: &f%s&7; Ratio: &f%s".formatted(
                        EntityDataUtil.getMinLevel(enInspected, false),
                        EntityDataUtil.getMaxLevel(enInspected, false),
                        to4dp.format(EntityDataUtil.getLevelRatio(enInspected, false))
                    )
                );

                messenger.accept("&8 • &7Attributes:");
                for(final Attribute a : Attribute.values()) {
                    final AttributeInstance ai = enInspected.getAttribute(a);
                    if(ai == null) continue;

                    final StringBuilder sb = new StringBuilder(
                        "&8   • &7" + EnumUtils.formatEnumConstant(a) + ": &f" +
                            to4dp.format(ai.getValue()) + "&7"
                    );

                    if(ai.getValue() != ai.getBaseValue())
                        sb.append(" (from &f")
                            .append(to4dp.format(ai.getBaseValue()))
                            .append("&7)");

                    if(ai.getModifiers().stream()
                        .anyMatch(mod -> mod.getName().startsWith(Buff.ATTRIBUTE_MODIFIER_PREFIX))
                    ) {
                        sb.append(" [✔LM](color=dark_aqua format=italic ")
                            .append("show_text=Modified by LevelledMobs)");
                    } else {
                        sb.append(" [❌LM](color=dark_gray format=italic ")
                            .append("show_text=Not modified by LevelledMobs)");
                    }

                    messenger.accept(sb.toString());
                }

                messenger.accept("&8└ ... (end of information) ...");
            }
        }.runTaskLater(LevelledMobs.getInstance(), 1);
    }

    private void handleShieldBreaker(
        final @Nonnull EntityDamageByEntityEvent event
    ) {
        /*
        To handle the shield breaker, the following conditions must be met:
            1. The defending entity is a Player
            2. The attacking entity is a LivingEntity
            3. The attacking entity is levelled
         */
        if(!(event.getEntity() instanceof final Player plDefender &&
            event.getDamager() instanceof final LivingEntity enAttacker &&
            EntityDataUtil.isLevelled(enAttacker, true)
        )) return;

        final FoundItemInHandResult shieldSearch = PlayerUtils.findItemStackInEitherHand(
            plDefender,
            itemStack -> itemStack != null && itemStack.getType() == Material.SHIELD
        );

        if(shieldSearch == null) return;

        final String formula = EntityDataUtil
            .getShieldBreakerMultiplierFormula(enAttacker, true);

        if(formula == null) return;

        @SuppressWarnings("unused") //TODO remove
        final double multiplier = LogicHandler.evaluateExpression(
            LogicHandler.replacePapiAndContextPlaceholders(
                formula,
                new Context()
                    .withEntity(plDefender)
                    //TODO also have attacker context. Context#withAttacker(enAttacker)
            )
        );

        /*
        darn ... unfortunately the bukkit API does not allow us to get the entity who caused
        the player's item to be damaged in PlayerItemDamageEvent. so shield breaking won't be
        a feature until that changes. I'll leave the code here in case it becomes possible
        in the future.

        Yes, I have thought about using a bit of hacky code to get around it, but I'd rather not
        since it could cause weird side-effects. For instance, applying a 1-tick-expiry
        temporary metadata value to `plDefender` with the `multiplier` value, and then referencing
        that in the `PlayerItemDamageEvent`.
        */
    }
}
