package io.github.arcaneplugins.levelledmobs.bukkit.listener;

import de.themoep.minedown.adventure.MineDown;
import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugCategory;
import io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import java.util.function.Consumer;
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

        handleEntityInspector(event);

        LevelledMobs.getInstance().getLogicHandler().runFunctionsWithTriggers(
            new Context().withEntity(defender), "on-entity-damage-by-entity"
            //TODO also have 'attacker' context
        );
    }

    private void handleEntityInspector(final EntityDamageByEntityEvent event) {
        if (!DebugHandler.isCategoryEnabled(DebugCategory.ENTITY_INSPECTOR) ||
            !(event.getEntity() instanceof LivingEntity inspected) ||
            !(event.getDamager() instanceof Player inspector) ||
            !inspector.hasPermission("levelledmobs.debug") ||
            !EntityDataUtil.isLevelled(inspected, false)
        ) {
            return;
        }

        final Context context = new Context()
            .withEntity(inspected)
            .withLocation(inspected.getLocation())
            .withEntityType(inspected.getType())
            .withWorld(inspected.getWorld())
            .withPlayer(inspector);

        final Consumer<String> messenger = (message) -> inspector.sendMessage(
            MineDown.parse(LogicHandler.replacePapiAndContextPlaceholders(message, context))
        );

        new BukkitRunnable() {
            @Override
            public void run() {
                messenger.accept("&8&m-&8{&f&l LM:&7 Inspecting " +
                    "&bLvl.%entity-level% %entity-type-formatted% " +
                    "&8(&7%entity-health-rounded%&8/&7%entity-max-health-rounded%&c♥&8) &8}&m-"
                );
                messenger.accept("&9EntityName:&f %entity-name%");
                messenger.accept("&8... (end of information) ...");
            }
        }.runTaskLater(LevelledMobs.getInstance(), 1);
    }
}
