package org.lime.core.paper.tasks;

import org.bukkit.scheduler.BukkitTask;
import org.lime.core.common.api.tasks.ScheduleTask;

public record ScheduleBukkitTask(BukkitTask task)
        implements ScheduleTask {
    @Override
    public int getTaskId() {
        return task.getTaskId();
    }
    @Override
    public boolean isSync() {
        return task.isSync();
    }
    @Override
    public boolean isCancelled() {
        return task.isCancelled();
    }
    @Override
    public void cancel() {
        task.cancel();
    }
    public static ScheduleBukkitTask of(BukkitTask task) {
        return new ScheduleBukkitTask(task);
    }
}
