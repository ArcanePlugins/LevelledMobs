package me.lokka30.levelledmobs.wrappers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * This class is used for when code needs to be executed in a specific thread context.
 * It allows for Folia servers to have code executed in the correct scheduler while
 * providing compatibility for Paper / Spigot servers without needing to use
 * different methods per server type
 *
 * @author stumper66
 * @since 3.11.0
 */
public class SchedulerWrapper {
    public SchedulerWrapper(){
        this.main = LevelledMobs.getInstance();
    }

    public SchedulerWrapper(final Runnable runnable){
        this.main = LevelledMobs.getInstance();
        this.runnable = runnable;
        this.entity = null;
    }

    public SchedulerWrapper(final @Nullable Entity entity){
        // if an entity is providing and the server is folia, then the entity scheduler will be used
        this.main = LevelledMobs.getInstance();
        this.entity = entity;
    }

    public SchedulerWrapper(final @Nullable Entity entity, final Runnable runnable){
        // if an entity is providing and the server is folia, then the entity scheduler will be used
        this.main = LevelledMobs.getInstance();
        this.entity = entity;
        this.runnable = runnable;
    }

    private final LevelledMobs main;
    public Runnable runnable;
    public @Nullable Entity entity;
    // on folia servers if you want to use the regional scheduler
    // then provide a location before executing run()
    public @Nullable Location locationForRegionScheduler;
    public boolean runDirectlyInFolia;
    public boolean runDirectlyInBukkit;

    public void run(){
        if (main.getVerInfo().getIsRunningFolia()){
            if (runDirectlyInFolia){
                runnable.run();
                return;
            }

            final Consumer<ScheduledTask> task = scheduledTask -> runnable.run();

            if (entity != null){
                entity.getScheduler().run(main, task, null);
            }
            else{
                if (locationForRegionScheduler != null){
                    org.bukkit.Bukkit.getRegionScheduler().run(main, locationForRegionScheduler, task);
                }
                else{
                    org.bukkit.Bukkit.getAsyncScheduler().runNow(main, task);
                }
            }
        }
        else{
            if (runDirectlyInBukkit){
                runnable.run();
                return;
            }

            // if you provided an entity in the constructor, it is assumed the main thread needs to be used
            // since accessing entities asynchronously will usually result in an error
            if (entity != null){
                Bukkit.getScheduler().runTask(main, runnable);
            }
            else{
                Bukkit.getScheduler().runTaskAsynchronously(main, runnable);
            }
        }
    }

    public SchedulerResult runTaskTimerAsynchronously(final long delayMS, final long periodMS){
        if (main.getVerInfo().getIsRunningFolia()){
            final Consumer<ScheduledTask> task = scheduledTask -> runnable.run();
            final ScheduledTask scheduledTask = org.bukkit.Bukkit.getAsyncScheduler().runAtFixedRate(
                    main, task, delayMS, periodMS, TimeUnit.MILLISECONDS);

            return new SchedulerResult(scheduledTask);
        }
        else{
            // convert milliseconds into approximent ticks
            // 1 tick = ~ 50ms
            long convertedDelay = delayMS / 50L;
            long convertedPeriod = periodMS / 50L;

            final BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                    main, runnable, convertedDelay, convertedPeriod);

            return new SchedulerResult(bukkitTask);
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public SchedulerResult runDelayed(final long delayInTicks){
        if (main.getVerInfo().getIsRunningFolia()){
            final Consumer<ScheduledTask> task = scheduledTask -> runnable.run();

            ScheduledTask scheduledTask;
            if (this.entity != null) {
                scheduledTask = this.entity.getScheduler().runDelayed(main, task, null, delayInTicks);
            }
            else{
                final long milliseconds = delayInTicks * 50L;
                scheduledTask = org.bukkit.Bukkit.getAsyncScheduler().runDelayed(
                        main, task, milliseconds, TimeUnit.MILLISECONDS);
            }

            return new SchedulerResult(scheduledTask);
        }
        else{
            final BukkitTask bukkitTask = Bukkit.getScheduler().runTaskLater(main, runnable, delayInTicks);

            return new SchedulerResult(bukkitTask);
        }
    }

    public boolean getWillRunDirectly(){
        if (main.getVerInfo().getIsRunningFolia()){
            return this.runDirectlyInFolia;
        }
        else{
            return this.runDirectlyInBukkit;
        }
    }
}
