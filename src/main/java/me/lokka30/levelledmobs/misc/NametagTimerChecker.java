package me.lokka30.levelledmobs.misc;


import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

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
        this.nametagCooldownQueue = new HashMap<>();
        this.entityTargetMap = new WeakHashMap<>();
    }

    private final LevelledMobs main;
    public final Map<Player, WeakHashMap<LivingEntity, Instant>> nametagCooldownQueue;
    public final WeakHashMap<LivingEntity, Player> entityTargetMap;
    public final static Object nametagTimer_Lock = new Object();
    public final static Object entityTarget_Lock = new Object();

    public void checkNametags(){
        final List<LivingEntity> entitiesToRemove = new LinkedList<>();

        synchronized (nametagTimer_Lock){
            for (final Player player : nametagCooldownQueue.keySet()){
                for (final LivingEntity livingEntity : nametagCooldownQueue.get(player).keySet()){
                    if (!livingEntity.isValid()) continue;

                    final Duration timeDuration = Duration.between(nametagCooldownQueue.get(player).get(livingEntity), Instant.now());
                    if (timeDuration.toMillis() >= 8000)
                        entitiesToRemove.add(livingEntity);
                }

                for (final LivingEntity livingEntity : entitiesToRemove) {
                    nametagCooldownQueue.get(player).remove(livingEntity);

                    final LivingEntityWrapper lmEntity = LivingEntityWrapper.getInstance(livingEntity, main);
                    main.levelManager.updateNametag(lmEntity, main.levelManager.getNametag(lmEntity, false), List.of(player));
                    lmEntity.free();
                }

                entitiesToRemove.clear();
            }
        }
    }
}
