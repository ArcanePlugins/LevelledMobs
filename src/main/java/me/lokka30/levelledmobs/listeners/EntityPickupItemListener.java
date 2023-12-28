package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.PickedUpEquipment;
import me.lokka30.levelledmobs.wrappers.LivingEntityWrapper;
import me.lokka30.levelledmobs.wrappers.SchedulerWrapper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class EntityPickupItemListener implements Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPickupItemEvent(final @NotNull EntityPickupItemEvent event){
        // sorry guys this is a Paper only feature
        if (!LevelledMobs.getInstance().getVerInfo().getIsRunningPaper()) return;
        if (event.getEntity() instanceof Player) return;

        final LivingEntityWrapper lmEntity = LivingEntityWrapper.getInstance(
                event.getEntity(), LevelledMobs.getInstance());

        if (!lmEntity.isLevelled() || lmEntity.getLivingEntity().getEquipment() == null){
            lmEntity.free();
            return;
        }

        // if you don't clone the item then it will change to air in the next function
        final ItemStack itemStack = event.getItem().getItemStack().clone();
        final PickedUpEquipment pickedUpEquipment = new PickedUpEquipment(lmEntity);
        final SchedulerWrapper wrapper = new SchedulerWrapper(lmEntity.getLivingEntity(),
                () -> pickedUpEquipment.checkEquipment(itemStack));
        wrapper.runDelayed(1L);
    }
}