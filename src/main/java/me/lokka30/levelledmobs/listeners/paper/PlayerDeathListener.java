package me.lokka30.levelledmobs.listeners.paper;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.result.NametagResult;
import me.lokka30.levelledmobs.util.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.HoverEvent;
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

    public PlayerDeathListener(final LevelledMobs main) {
        this.main = main;
    }

    private final LevelledMobs main;
    private boolean shouldCancelEvent;
    private PlayerDeathEvent event;

    public boolean onPlayerDeathEvent(final @NotNull PlayerDeathEvent event) {
        this.event = event;
        this.shouldCancelEvent = false;
        if (event.deathMessage() == null) {
            return true;
        }
        if (!(event.deathMessage() instanceof TranslatableComponent)) {
            return false;
        }

        final LivingEntityWrapper lmEntity = getPlayersKiller(event);

        if (lmEntity == null) {
            if (main.placeholderApiIntegration != null) {
                main.placeholderApiIntegration.putPlayerOrMobDeath(event.getEntity(), null, true);
            }
            if (this.shouldCancelEvent) event.setCancelled(true);
            return true;
        }

        if (main.placeholderApiIntegration != null) {
            main.placeholderApiIntegration.putPlayerOrMobDeath(event.getEntity(), lmEntity, true);
        }
        lmEntity.free();

        if (this.shouldCancelEvent) event.setCancelled(true);
        return true;
    }

    @Nullable private LivingEntityWrapper getPlayersKiller(@NotNull final PlayerDeathEvent event) {
        final EntityDamageEvent entityDamageEvent = event.getEntity().getLastDamageCause();
        if (entityDamageEvent == null || entityDamageEvent.isCancelled()
            || !(entityDamageEvent instanceof EntityDamageByEntityEvent)) {
            return null;
        }

        final Entity damager = ((EntityDamageByEntityEvent) entityDamageEvent).getDamager();
        LivingEntity killer = null;

        if (damager instanceof final Projectile projectile) {
            if (projectile.getShooter() instanceof LivingEntity) {
                killer = (LivingEntity) projectile.getShooter();
            }
        } else if (damager instanceof LivingEntity) {
            killer = (LivingEntity) damager;
        }

        if (killer == null || Utils.isNullOrEmpty(killer.getName()) || killer instanceof Player) {
            return null;
        }

        final LivingEntityWrapper lmKiller = LivingEntityWrapper.getInstance(killer, main);
        if (!lmKiller.isLevelled()) {
            return lmKiller;
        }

        final NametagResult mobNametag = main.levelManager.getNametag(lmKiller, true, true);
        if (mobNametag.getNametag() != null && mobNametag.getNametag().isEmpty()){
            this.shouldCancelEvent = true;
            return lmKiller;
        }

        if (Utils.isNullOrEmpty(mobNametag.getNametagNonNull()) || "disabled".equalsIgnoreCase(mobNametag.getNametagNonNull())) {
            return lmKiller;
        }

        updateDeathMessage(mobNametag);

        return lmKiller;
    }

    private void updateDeathMessage(final NametagResult nametagResult) {
        final TranslatableComponent tc = (TranslatableComponent) event.deathMessage();
        if (tc == null) {
            return;
        }

        final String playerKilled = extractPlayerName(tc);
        if (playerKilled == null) {
            return;
        }

        String mobKey = null;
        for (final Component c : tc.args()){
            if (c instanceof TranslatableComponent)
                mobKey = ((TranslatableComponent) c).key();
        }

        if (mobKey == null) return;
        final String mobName = nametagResult.getNametagNonNull();
        final String playerName = event.getPlayer().getName();
        final int displayNameIndex = mobName.indexOf("{DisplayName}");

        if (displayNameIndex < 0){
            event.deathMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(mobName.replace("%player%", playerName)));
            return;
        }

        // this component holds the component of the mob name and will show the translated name on clients
        final Component mobNameComponent = nametagResult.overriddenName == null ?
                Component.translatable(mobKey) :
                LegacyComponentSerializer.legacyAmpersand().deserialize(nametagResult.overriddenName);

        Component newCom;

        // TODO: is there a better way to do the following?
        final boolean hasLeftText = displayNameIndex > 0;
        final boolean hasRightText = mobName.length() > displayNameIndex + 13;
        Component leftText = hasLeftText ?
                LegacyComponentSerializer.legacyAmpersand().deserialize(
                mobName.substring(0, displayNameIndex).replace("%player%", playerName)) : null;
        Component rightText = hasRightText ? LegacyComponentSerializer.legacyAmpersand().deserialize(
                mobName.substring(displayNameIndex + 13).replace("%player%", playerName)) : null;

        if (hasLeftText && hasRightText){
            // creature-death-nametag: 'something here %displayname% something there'

            newCom = nametagResult.hadDeathMessage ?
                    leftText.append(mobNameComponent)
                            .append(rightText):
                    Component.translatable(tc.key(),
                            Component.text(playerKilled),
                            leftText.append(mobNameComponent)
                    ).append(rightText);

        }
        else if (hasLeftText){
            // creature-death-nametag: 'something here %displayname%'
            newCom = nametagResult.hadDeathMessage ?
                    leftText.append(mobNameComponent) :
                    Component.translatable(tc.key(),
                            Component.text(playerKilled),
                            leftText.append(mobNameComponent));
        }
        else if (hasRightText){
            // creature-death-nametag: '%displayname% something there'
            newCom = Component.translatable(tc.key(),
                    Component.text(playerKilled),
                    mobNameComponent
            ).append(rightText);
        }
        else {
            // creature-death-nametag: '%displayname%'
            newCom = Component.translatable(tc.key(),
                    Component.text(playerKilled),
                    mobNameComponent
            );
        }

        event.deathMessage(newCom);
    }

    @Nullable private String extractPlayerName(final @NotNull TranslatableComponent tc) {
        String playerKilled = null;

        for (final net.kyori.adventure.text.Component com : tc.args()) {
            if (com instanceof final TextComponent tc2) {
                playerKilled = tc2.content();

                if (playerKilled.isEmpty() && tc2.hoverEvent() != null) {
                    // in rare cases the above method returns a empty string
                    // we'll extract the player name from the hover event
                    final HoverEvent<?> he = tc2.hoverEvent();
                    if (he == null || !(he.value() instanceof final HoverEvent.ShowEntity se)) {
                        return null;
                    }

                    if (se.name() instanceof final TextComponent tc3) {
                        playerKilled = tc3.content();
                    }
                }
            }
        }

        return playerKilled == null || playerKilled.isEmpty() ?
            null : playerKilled;
    }
}
