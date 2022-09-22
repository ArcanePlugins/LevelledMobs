package me.lokka30.levelledmobs.bukkit.listener;

import de.themoep.minedown.MineDown;
import java.util.function.Consumer;
import me.lokka30.levelledmobs.bukkit.LevelledMobs;
import me.lokka30.levelledmobs.bukkit.api.data.EntityDataUtil;
import me.lokka30.levelledmobs.bukkit.debug.DebugCategory;
import me.lokka30.levelledmobs.bukkit.debug.DebugHandler;
import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class EntityDamageByEntityListener extends ListenerWrapper {

    /**
     * Create a new listener.
     */
    public EntityDamageByEntityListener() {
        super(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
        handleEntityInspector(event);
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

        final Consumer<String> messenger = (message) -> inspector.spigot().sendMessage(
            MineDown.parse(
                LevelledMobs.getInstance().getLogicHandler().getContextPlaceholderHandler()
                    .replace(message, context)
            )
        );

        messenger.accept("&8&m-&8{&f&l LM:&7 Inspecting " +
            "&bLvl.%entity-level% %entity-type-formatted% " +
            "&8(&7%entity-health-rounded%&8/&7%entity-max-health-rounded%&c♥&8) &8}&m-"
        );
        messenger.accept("&9EntityName:&f %entity-name%");
        messenger.accept("&8... (end of information) ...");
    }
}
