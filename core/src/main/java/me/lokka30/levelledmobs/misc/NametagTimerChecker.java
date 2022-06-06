package me.lokka30.levelledmobs.misc;


import me.lokka30.levelledmobs.rules.NametagVisibilityEnum;
import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.WeakHashMap;

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
        this.playersQueue = new LinkedList<>();
        this.nametagCooldownQueue = new HashMap<>();
        this.entityTargetMap = new WeakHashMap<>();
        this.cooldownTimes = new WeakHashMap<>();
    }

    private final LevelledMobs main;
    private final Map<Player, WeakHashMap<LivingEntity, Instant>> nametagCooldownQueue;
    public final WeakHashMap<LivingEntity, Long> cooldownTimes;
    public final WeakHashMap<LivingEntity, Player> entityTargetMap;
    private final Queue<PlayerQueueItem> playersQueue;
    public final static Object nametagTimer_Lock = new Object();
    public final static Object entityTarget_Lock = new Object();
    private final static Object playerQueue_Lock = new Object();

    public void addPlayerToQueue(final @NotNull PlayerQueueItem item) {
        synchronized (playerQueue_Lock) {
            playersQueue.offer(item);
        }
    }

    public Map<Player, WeakHashMap<LivingEntity, Instant>> getNametagCooldownQueue(){
        return this.nametagCooldownQueue;
    }

    public void checkNametags(){
        final List<LivingEntity> entitiesToRemove = new LinkedList<>();

        synchronized (nametagTimer_Lock){
            synchronized (playerQueue_Lock){
                while (!playersQueue.isEmpty()){
                    final PlayerQueueItem item = playersQueue.poll();
                    if (item == null) continue;

                    if (item.isPlayerJoin)
                        this.nametagCooldownQueue.put(item.player, new WeakHashMap<>());
                    else
                        this.nametagCooldownQueue.remove(item.player);
                }
            }

            for (final Map.Entry<Player, WeakHashMap<LivingEntity, Instant>> coolDown : nametagCooldownQueue.entrySet()){
                final Player player = coolDown.getKey();
                for (final LivingEntity livingEntity : coolDown.getValue().keySet()){
                    if (!livingEntity.isValid()) continue;

                    final Duration timeDuration = Duration.between(coolDown.getValue().get(livingEntity), Instant.now());
                    final long cooldownTime = cooldownTimes.get(livingEntity);
                    if (timeDuration.toMillis() >= cooldownTime) {
                        // if using LoS targeting check if it's still within LoS and don't remove if so.
                        final LivingEntityWrapper lmEntity = LivingEntityWrapper.getInstance(livingEntity, main);
                        final boolean usesLoS = main.rulesManager.getRule_CreatureNametagVisbility(lmEntity).contains(NametagVisibilityEnum.TARGETED);
                        if (usesLoS && livingEntity.hasLineOfSight(player)){
                            coolDown.getValue().put(livingEntity, Instant.now());
                        }
                        else
                            entitiesToRemove.add(livingEntity);

                        lmEntity.free();
                    }
                }

                for (final LivingEntity livingEntity : entitiesToRemove) {
                    coolDown.getValue().remove(livingEntity);

                    final LivingEntityWrapper lmEntity = LivingEntityWrapper.getInstance(livingEntity, main);
                    main.levelManager.updateNametag(lmEntity, main.levelManager.getNametag(lmEntity, false), List.of(player));
                    lmEntity.free();
                }

                entitiesToRemove.clear();
            }
        }
    }
}
