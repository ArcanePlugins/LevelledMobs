package me.lokka30.levelledmobs.misc;


import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.rules.NametagVisibilityEnum;
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
        this.cooldownTimes = new WeakHashMap<>();
    }

    private final LevelledMobs main;
    public final Map<Player, WeakHashMap<LivingEntity, Instant>> nametagCooldownQueue;
    public final WeakHashMap<LivingEntity, Integer> cooldownTimes;
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
                    final int cooldownTime = cooldownTimes.get(livingEntity);
                    if (timeDuration.toMillis() >= cooldownTime) {
                        // if using LoS targeting check if it's still within LoS and don't remove if so.
                        final LivingEntityWrapper lmEntity = LivingEntityWrapper.getInstance(livingEntity, main);
                        final boolean usesLoS = main.rulesManager.getRule_CreatureNametagVisbility(lmEntity).contains(NametagVisibilityEnum.TARGETED);
                        if (usesLoS && livingEntity.hasLineOfSight(player)){
                            nametagCooldownQueue.get(player).put(livingEntity, Instant.now());
                        }
                        else
                            entitiesToRemove.add(livingEntity);

                        lmEntity.free();
                    }
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
