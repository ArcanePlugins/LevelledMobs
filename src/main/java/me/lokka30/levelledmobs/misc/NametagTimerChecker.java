package me.lokka30.levelledmobs.misc;


import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

/**
 * Used to mobs have a nametag cooldown timer where the nametag stays
 * always visible for a configurable amount of time
 *
 * @author stumper66
 * @since 3.2.0
 */
public class NametagTimerChecker {

    public NametagTimerChecker(final LevelledMobs main) {
        this.main = main;
    }

    private final LevelledMobs main;

    public void checkNametags(){
        final List<LivingEntity> entitiesToRemove = new LinkedList<>();

        synchronized (main.nametagTimer_Lock){
            for (final Player player : main.nametagCooldownQueue.keySet()){
                for (final LivingEntity livingEntity : main.nametagCooldownQueue.get(player).keySet()){
                    if (!livingEntity.isValid()) continue;

                    final Duration timeDuration = Duration.between(main.nametagCooldownQueue.get(player).get(livingEntity), Instant.now());
                    if (timeDuration.toMillis() >= 8000)
                        entitiesToRemove.add(livingEntity);
                }

                for (final LivingEntity livingEntity : entitiesToRemove) {
                    main.nametagCooldownQueue.get(player).remove(livingEntity);

                    final LivingEntityWrapper lmEntity = LivingEntityWrapper.getInstance(livingEntity, main);
                    main.levelManager.updateNametag(lmEntity, main.levelManager.getNametag(lmEntity, false), List.of(player));
                    lmEntity.free();
                }

                entitiesToRemove.clear();
            }
        }
    }
}
