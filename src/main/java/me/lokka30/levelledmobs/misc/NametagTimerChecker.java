package me.lokka30.levelledmobs.misc;


import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.entity.LivingEntity;

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
        if (main.nametagTimerResetTime <= 0)
            return;

        final List<LivingEntity> entitiesToRemove = new LinkedList<>();

        synchronized (main.nametagTimer_Lock){
            for (final LivingEntity livingEntity : main.nametagTimer.keySet()){
                final Duration duration = Duration.between(main.nametagTimer.get(livingEntity), Instant.now());
                if (duration.toMillis() < main.nametagTimerResetTime)
                    continue;

                final LivingEntityWrapper lmEntity = LivingEntityWrapper.getInstance(livingEntity, main);
                main.levelManager.updateNametag(lmEntity);

                lmEntity.free();
                entitiesToRemove.add(livingEntity);
            }

            for (final LivingEntity livingEntity : entitiesToRemove)
                main.nametagTimer.remove(livingEntity);
        }
    }
}
