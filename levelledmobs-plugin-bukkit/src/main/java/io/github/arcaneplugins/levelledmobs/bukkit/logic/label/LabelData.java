package io.github.arcaneplugins.levelledmobs.bukkit.logic.label;

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

public class LabelData {
    public LabelData(){
        this.nametagCooldowns = new HashMap<>();
    }

    private final Map<Player, WeakHashMap<LivingEntity, Instant>> nametagCooldowns;

    public void processItem(final @NotNull Context context,
                            final @NotNull Component component){

        if (context.getPlayer() == null) return;

        //TODO: factor in visibility methods
        //TODO: factor in visibility duration
        final int visibleDuration = 2000; // hard coded to 2 seconds for now

        final WeakHashMap<LivingEntity, Instant> playerEntry =
                nametagCooldowns.computeIfAbsent(context.getPlayer(), k -> new WeakHashMap<>());

        final LivingEntity lent = (LivingEntity) context.getEntity();
        Objects.requireNonNull(lent, "LivingEntity context must not be null");
        playerEntry.put(lent, Instant.now());

        LevelledMobs.getInstance().getNametagSender().sendNametag(
                lent, context.getPlayer(), component);
    }
}
