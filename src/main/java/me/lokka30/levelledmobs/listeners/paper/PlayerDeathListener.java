package me.lokka30.levelledmobs.listeners.paper;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.result.NametagResult;
import me.lokka30.levelledmobs.util.Utils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.ComponentSerializer;
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

    public boolean onPlayerDeathEvent(final @NotNull PlayerDeathEvent event) {
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

        if (mobNametag.isNullOrEmpty() || "disabled".equalsIgnoreCase(mobNametag.getNametag())) {
            return lmKiller;
        }

        updateDeathMessage(event, mobNametag);

        return lmKiller;
    }

    private void updateDeathMessage(final @NotNull PlayerDeathEvent event, final @NotNull NametagResult nametagResult) {
        if (!(event.deathMessage() instanceof final TranslatableComponent tc)) {
            // This can happen if another plugin destructively changes the death message.
            return;
        }

        String mobKey = null;
        Component itemComp = null;
        for (final Component c : tc.args()){
            if (c instanceof final TranslatableComponent tc2) {
                if ("chat.square_brackets".equals(tc2.key())) {
                    // this is when the mob was holding a weapon
                    itemComp = tc2;
                }
                else {
                    mobKey = tc2.key();
                }
            }
        }

        if (mobKey == null) {
            return;
        }
        final String mobName = nametagResult.getNametagNonNull();
        final int displayNameIndex = mobName.indexOf("{DisplayName}");
        final ComponentSerializer<Component, ?, String> cs = main.getDefinitions().getUseLegacySerializer() ?
                LegacyComponentSerializer.legacyAmpersand() :
                main.getDefinitions().mm;

        Component newCom;
        if (nametagResult.hadCustomDeathMessage){
            final TextReplacementConfig replacementConfig = TextReplacementConfig.builder().matchLiteral("%player%")
                    .replacement(buildPlayerComponent(event.getEntity())).build();
            newCom = cs.deserialize(mobName)
                    .replaceText(replacementConfig);
        }
        else if (displayNameIndex < 0){
            // creature-death-nametag in rules.yml doesn't contain %displayname%
            // so we'll just send the whole thing as text
            newCom = Component.translatable(tc.key(),
                    buildPlayerComponent(event.getEntity()),
                    cs.deserialize(mobName));
        }
        else {
            final Component leftComp = displayNameIndex > 0 ?
                    cs.deserialize(mobName.substring(0, displayNameIndex)) :
                    Component.empty();
            final Component rightComp = mobName.length() > displayNameIndex + 13 ?
                    cs.deserialize(mobName.substring(displayNameIndex + 13)) :
                    Component.empty();

            final Component mobNameComponent = nametagResult.overriddenName == null ?
                    Component.translatable(mobKey) :
                    cs.deserialize(nametagResult.overriddenName);

            if (itemComp == null) {
                // mob wasn't using any weapon
                // 2 arguments, example: "death.attack.mob": "%1$s was slain by %2$s"
                newCom = Component.translatable(tc.key(),
                        buildPlayerComponent(event.getEntity()),
                        leftComp.append(mobNameComponent)
                ).append(rightComp);
            }
            else {
                // mob had a weapon and it's details are stored in the itemComp component
                // 3 arguments, example: "death.attack.mob.item": "%1$s was slain by %2$s using %3$s"
                newCom = Component.translatable(tc.key(),
                        buildPlayerComponent(event.getEntity()),
                        leftComp.append(mobNameComponent),
                        itemComp
                ).append(rightComp);
            }
        }

        event.deathMessage(newCom);
    }

    private @NotNull Component buildPlayerComponent(final @NotNull Player player){
        final Component playerName = main.nametagQueueManager.nametagSenderHandler.versionInfo.getMinecraftVersion() >= 1.18 ?
                player.name() : Component.text(player.getName());
        final HoverEvent<HoverEvent.ShowEntity> hoverEvent = HoverEvent.showEntity(
                Key.key("minecraft"), player.getUniqueId(), playerName);
        final ClickEvent clickEvent = ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                "/tell " + player.getName() + " ");

        return Component.text(player.getName()).clickEvent(clickEvent).hoverEvent(hoverEvent);
    }
}