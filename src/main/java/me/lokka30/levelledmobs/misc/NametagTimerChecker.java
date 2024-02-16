package me.lokka30.levelledmobs.misc;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.WeakHashMap;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.result.NametagResult;
import me.lokka30.levelledmobs.rules.NametagVisibilityEnum;
import me.lokka30.levelledmobs.wrappers.LivingEntityWrapper;
import me.lokka30.levelledmobs.wrappers.SchedulerWrapper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Used to mobs have a nametag cooldown timer where the nametag stays always visible for a
 * configurable amount of time
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

    public Map<Player, WeakHashMap<LivingEntity, Instant>> getNametagCooldownQueue() {
        return this.nametagCooldownQueue;
    }

    public void checkNametags() {
        // in folia this is using the bukkit async scheduler context

        synchronized (nametagTimer_Lock) {
            synchronized (playerQueue_Lock) {
                while (!playersQueue.isEmpty()) {
                    final PlayerQueueItem item = playersQueue.poll();
                    if (item == null) {
                        continue;
                    }

                    if (item.isPlayerJoin) {
                        this.nametagCooldownQueue.put(item.player, new WeakHashMap<>());
                    } else {
                        this.nametagCooldownQueue.remove(item.player);
                    }
                }
            }

            if (nametagCooldownQueue.isEmpty()) return;

            for (final Map.Entry<Player, WeakHashMap<LivingEntity, Instant>> coolDown : nametagCooldownQueue.entrySet()) {
                final Player player = coolDown.getKey();
                processCooldownQueue(player, coolDown);
            }
        }
    }

    private void processCooldownQueue(final @NotNull Player player,
        final Map.@NotNull Entry<Player, WeakHashMap<LivingEntity, Instant>> coolDown){
        final List<LivingEntity> entitiesToRemove = new LinkedList<>();

        for (final LivingEntity livingEntity : coolDown.getValue().keySet()) {
            if (main.getVerInfo().getIsRunningFolia()){
                livingEntity.getScheduler().run(main, scheduledTask ->
                        processCoolDownEntity(livingEntity, entitiesToRemove, player, coolDown),
                        null);
            }
            else
                processCoolDownEntity(livingEntity, entitiesToRemove, player, coolDown);
        }

        for (final LivingEntity livingEntity : entitiesToRemove) {
            coolDown.getValue().remove(livingEntity);

            final SchedulerWrapper wrapper = new SchedulerWrapper(livingEntity, () -> updateNametag(livingEntity, player));
            wrapper.runDirectlyInBukkit = true;
            wrapper.run();
        }

        entitiesToRemove.clear();
    }

    private void processCoolDownEntity(final @NotNull LivingEntity livingEntity,
                                       final List<LivingEntity> entitiesToRemove,
                                       final @NotNull Player player,
                                       final Map.@NotNull Entry<Player, WeakHashMap<LivingEntity, Instant>> coolDown){
        if (!livingEntity.isValid()) {
            return;
        }

        final Instant startInclusive = coolDown.getValue().get(livingEntity);
        if (startInclusive == null){
            entitiesToRemove.add(livingEntity);
            return;
        }

        if (!player.getWorld().getName().equals(livingEntity.getWorld().getName())){
            entitiesToRemove.add(livingEntity);
            return;
        }

        final Duration timeDuration = Duration.between(
                startInclusive, Instant.now());
        final long cooldownTime = cooldownTimes.get(livingEntity);
        if (timeDuration.toMillis() >= cooldownTime) {
            // if using LoS targeting check if it's still within LoS and don't remove if so.
            final LivingEntityWrapper lmEntity = LivingEntityWrapper.getInstance(
                    livingEntity, main);

            final boolean usesLoS = main.rulesManager.getRuleCreatureNametagVisbility(
                    lmEntity).contains(NametagVisibilityEnum.TARGETED);

            if (usesLoS && livingEntity.hasLineOfSight(player)) {
                coolDown.getValue().put(livingEntity, Instant.now());
            } else {
                entitiesToRemove.add(livingEntity);
            }

            lmEntity.free();
        }
    }

    private void updateNametag(final @NotNull LivingEntity livingEntity, final @NotNull Player player){
        final LivingEntityWrapper lmEntity = LivingEntityWrapper.getInstance(
                livingEntity, main);

        final NametagResult nametag = main.levelManager.getNametag(lmEntity, false, true);
        main.levelManager.updateNametag(lmEntity, nametag, List.of(player));

        lmEntity.free();
    }
}
