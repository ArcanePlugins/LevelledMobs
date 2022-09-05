package me.lokka30.levelledmobs.listeners.paper;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.NametagUpdateResult;
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

    public boolean onPlayerDeathEvent(final @NotNull PlayerDeathEvent event) {
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
            return true;
        }

        if (main.placeholderApiIntegration != null) {
            main.placeholderApiIntegration.putPlayerOrMobDeath(event.getEntity(), lmEntity, true);
        }
        lmEntity.free();

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

        final NametagUpdateResult mobNametag = main.levelManager.getNametag(lmKiller, true, true);
        if (Utils.isNullOrEmpty(mobNametag.getNametagNonNull()) || "disabled".equalsIgnoreCase(mobNametag.getNametagNonNull())) {
            return lmKiller;
        }

        updateDeathMessage(event, mobNametag);

        return lmKiller;
    }

    private void updateDeathMessage(@NotNull final PlayerDeathEvent event, final NametagUpdateResult nametagUpdateResult) {
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
        final String mobName = nametagUpdateResult.getNametagNonNull();

        final int displayNameIndex = mobName.indexOf("{DisplayName}");
        if (displayNameIndex < 0) return;

        final Component mobNameComponent = nametagUpdateResult.overriddenName == null ?
                Component.translatable(mobKey) :
                LegacyComponentSerializer.legacyAmpersand().deserialize(nametagUpdateResult.overriddenName);

        Component newCom;

        // TODO: is there a better way to do the following?
        final boolean hasLeftText = displayNameIndex > 0;
        final boolean hasRightText = mobName.length() > displayNameIndex + 13;

        if (hasLeftText && hasRightText){

            // creature-death-nametag: 'something here %displayname% something there'
            final Component leftText = LegacyComponentSerializer.legacyAmpersand().deserialize(mobName.substring(0, displayNameIndex));
            final Component rightText = LegacyComponentSerializer.legacyAmpersand().deserialize(mobName.substring(displayNameIndex + 13));

            newCom = Component.translatable(tc.key(),
                    Component.text(playerKilled),
                    leftText.append(mobNameComponent)
            ).append(rightText);
        }
        else if (hasLeftText){
            // creature-death-nametag: 'something here %displayname%'
            final Component leftText = LegacyComponentSerializer.legacyAmpersand().deserialize(mobName.substring(0, displayNameIndex));
            newCom = Component.translatable(tc.key(),
                    Component.text(playerKilled),
                    leftText.append(mobNameComponent)
            );
        }
        else if (hasRightText){
            // creature-death-nametag: '%displayname% something there'
            final Component rightText = LegacyComponentSerializer.legacyAmpersand().deserialize(mobName.substring(displayNameIndex + 13));
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
