package io.github.arcaneplugins.levelledmobs.bukkit.task;

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import io.github.arcaneplugins.levelledmobs.bukkit.util.math.TimeUtils;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public class TaskHandler {

    private TaskHandler() throws IllegalAccessException {
        throw new IllegalAccessException("Illegal instantiation of utility class");
    }

    private static BukkitTask mobScanTask = null;

    public static void startTasks() {
        Log.inf("Starting tasks");
        startMobScanTask();
    }

    public static void stopTasks() {
        Log.inf("Stopping tasks");
        if(mobScanTask != null) {
            mobScanTask.cancel();
            mobScanTask = null;
        }
    }

    private static void startMobScanTask() {
        if(getMobScanTask() != null) {
            getMobScanTask().cancel();
            mobScanTask = null;
        }

        final CommentedConfigurationNode scanTaskNode =
            LevelledMobs.getInstance().getConfigHandler().getSettingsCfg()
                .getRoot().node("advanced", "scan-task");

        if(!scanTaskNode.node("enabled").getBoolean(false)) return;

        final Runnable runnable = () -> {
            for(final World world : Bukkit.getWorlds()) {
                for(final Entity entity : world.getEntities()) {
                    LogicHandler.runFunctionsWithTriggers(
                        new Context().withEntity(entity).withWorld(world),
                        "on-entity-scan"
                    );
                }
            }
        };

        final CommentedConfigurationNode periodNode = scanTaskNode.node("period");
        long period = 20 * 7; // 7 seconds
        if(!periodNode.virtual()) {
            try {
                period = TimeUtils.parseTimeToTicks(
                    Objects.requireNonNull(
                        periodNode.get(Object.class),
                        "period"
                    )
                );
            } catch (final SerializationException ex) {
                throw new RuntimeException(ex);
            }
        }

        mobScanTask = Bukkit.getScheduler().runTaskTimer(
            LevelledMobs.getInstance(),
            runnable,
            0L, // Delay
            period
        );
    }

    private static @Nullable BukkitTask getMobScanTask() {
        return mobScanTask;
    }

}
