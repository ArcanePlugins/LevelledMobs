package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Random;

public class LMobDeath implements Listener {

    private LevelledMobs instance = LevelledMobs.getInstance();

    @EventHandler
    public void onDeath(final EntityDeathEvent e) {
        clearNametag(e.getEntity());
        calculateDrops(e.getEntity(), e.getDrops());
    }

    //Clear their nametag on death.
    private void clearNametag(final LivingEntity ent){
        if (instance.isLevellable(ent) && instance.settings.get("fine-tuning.remove-nametag-on-death", false)) {
            ent.setCustomName(null);
        }
    }

    //Calculates the drops when a levellable creature dies.
    private void calculateDrops(final LivingEntity ent, List<ItemStack> drops){

        if(instance.isLevellable(ent)){
            //If mob is levellable, but wasn't levelled, return.
            Integer level = ent.getPersistentDataContainer().get(instance.key, PersistentDataType.INTEGER);
            if(level == null)
                return;

            //Read settings for drops.
            double dropmultiplier = instance.settings.get("fine-tuning.multipliers.item-drop", 0.25);
            int finalmultiplier = 1;

            //If multiplier * level gurantees an extra drop set 'finalmultiplier' to the amount of safe multiples.
            dropmultiplier *= level;
            finalmultiplier += (int) dropmultiplier;
            dropmultiplier -= (int)dropmultiplier;

            //Calculate if the remaining extra drop chance triggers.
            double random = new Random().nextDouble();
            if (random < dropmultiplier)
                finalmultiplier++;

            //Edit the ItemStacks to drop the calculated multiple items.
            for(int i = 0; i < drops.size(); i++) {
                ItemStack istack = drops.get(i);
                istack.setAmount(istack.getAmount() * finalmultiplier);
                drops.set(i, istack);
            }
        }
    }
}
