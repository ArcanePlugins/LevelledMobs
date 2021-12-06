package me.lokka30.levelledmobs.misc;

import me.lokka30.levelledmobs.LevelledMobs;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Provides function for APIs that are deprecated in Paper but required for
 * use in Spigot
 *
 * @author stumper66
 * @since 3.3.0
 */
@SuppressWarnings("deprecation")
public class SpigotUtils {
    public static void sendHyperlink(final @NotNull CommandSender sender, final String message, final String url){

        final TextComponent component = new TextComponent(message);
        component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(url)));
        sender.sendMessage(component);
    }

    public static void updateItemMetaLore(final @NotNull ItemMeta meta, final @Nullable List<String> lore){
        meta.setLore(lore);
    }

    public static void updateItemDisplayName(final @NotNull ItemMeta meta, final @Nullable String displayName){
        meta.setDisplayName(displayName);
    }

    @NotNull
    public static String getPlayerDisplayName(final @Nullable Player player){
        if (player == null) return "";
        return player.getDisplayName();
    }

    @Nullable
    public static LivingEntityWrapper getPlayersKiller(@NotNull final PlayerDeathEvent event, final LevelledMobs main){
        if (event.getDeathMessage() == null) return null;

        final EntityDamageEvent entityDamageEvent = event.getEntity().getLastDamageCause();
        if (entityDamageEvent == null || entityDamageEvent.isCancelled() || !(entityDamageEvent instanceof EntityDamageByEntityEvent))
            return null;

        final Entity damager = ((EntityDamageByEntityEvent) entityDamageEvent).getDamager();
        final LivingEntity killer;

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

        event.setDeathMessage(Utils.replaceEx(event.getDeathMessage(), killer.getName(), deathMessage));
        return lmKiller;
    }
}
