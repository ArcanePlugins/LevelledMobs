package me.lokka30.levelledmobs.wrappers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

public class SchedulerResult {
    public SchedulerResult(final @Nullable ScheduledTask foliaTask){
        this.foliaTask = foliaTask;
        this.regularTask = null;
        this.isRunningFolia = true;
    }

    public SchedulerResult(final @Nullable BukkitTask regularTask){
        this.regularTask = regularTask;
        this.foliaTask = null;
        this.isRunningFolia = false;
    }

    public final @Nullable ScheduledTask foliaTask;
    public final @Nullable BukkitTask regularTask;
    public final boolean isRunningFolia;

    public void cancelTask(){
        if (isRunningFolia){
            if (foliaTask != null) foliaTask.cancel();
        }
        else{
            if (regularTask != null) regularTask.cancel();
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isCancelled(){
        if (isRunningFolia){
            if (foliaTask != null)
                return foliaTask.isCancelled();
            else
                return false;
        }
        else{
            if (regularTask != null)
                return regularTask.isCancelled();
            else
                return false;
        }
    }
}
