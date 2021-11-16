package me.lokka30.levelledmobs.listeners.paper;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerDeathListener {
    public PlayerDeathListener(final LevelledMobs main){
        this.main = main;
    }

    private final LevelledMobs main;

    public boolean onPlayerDeathEvent(final @NotNull PlayerDeathEvent event){
        if (event.deathMessage() == null) return true;
        if (!(event.deathMessage() instanceof TranslatableComponent)) return false;

        final LivingEntityWrapper lmEntity = getPlayersKiller(event);

        if (lmEntity == null){
            if (main.placeholderApiIntegration != null)
                main.placeholderApiIntegration.putPlayerOrMobDeath(event.getEntity(), null);
            return true;
        }

        if (main.placeholderApiIntegration != null)
            main.placeholderApiIntegration.putPlayerOrMobDeath(event.getEntity(), lmEntity);
        lmEntity.free();

        return true;
    }

    @Nullable
    private LivingEntityWrapper getPlayersKiller(@NotNull final PlayerDeathEvent event){
        final EntityDamageEvent entityDamageEvent = event.getEntity().getLastDamageCause();
        if (entityDamageEvent == null || entityDamageEvent.isCancelled() || !(entityDamageEvent instanceof EntityDamageByEntityEvent))
            return null;

        final Entity damager = ((EntityDamageByEntityEvent) entityDamageEvent).getDamager();
        LivingEntity killer;

        if (damager instanceof Projectile)
            killer = (LivingEntity) ((Projectile) damager).getShooter();
        else if (!(damager instanceof LivingEntity))
            return null;
        else
            killer = (LivingEntity) damager;

        if (killer == null || Utils.isNullOrEmpty(killer.getName()) || killer instanceof Player) return null;

        final LivingEntityWrapper lmKiller = LivingEntityWrapper.getInstance(killer, main);
        if (!lmKiller.isLevelled())
            return lmKiller;

        final String deathMessage = main.levelManager.getNametag(lmKiller, true);
        if (Utils.isNullOrEmpty(deathMessage) || "disabled".equalsIgnoreCase(deathMessage))
            return lmKiller;

        updateDeathMessage(event, deathMessage);

        return lmKiller;
    }

    private void updateDeathMessage(@NotNull final PlayerDeathEvent event, final String mobName){
        final TranslatableComponent tc = (TranslatableComponent) event.deathMessage();
        if (tc == null) return;
        String playerKilled = null;

        for (final net.kyori.adventure.text.Component com : tc.args()){
            if (com instanceof TextComponent)
                playerKilled = ((TextComponent) com).content();
        }

        if (playerKilled == null) return;

        final TextComponent tcMobName = LegacyComponentSerializer.legacySection().deserialize(mobName);
        final Component newCom = Component.text().content(playerKilled).build()
                .append(Component.translatable().key(tc.key()).build())
                .append(tcMobName);

        event.deathMessage(newCom);
    }
}
