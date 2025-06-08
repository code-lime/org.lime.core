package org.lime.core.velocity.tasks;

import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.TaskStatus;
import org.lime.core.common.api.tasks.ScheduleTask;

public record ScheduleVelocityTask(
        int taskId,
        ScheduledTask task)
        implements ScheduleTask {
    @Override
    public int getTaskId() {
        return taskId;
    }
    @Override
    public boolean isSync() {
        return true;
    }
    @Override
    public boolean isCancelled() {
        return task.status() == TaskStatus.CANCELLED;
    }
    @Override
    public void cancel() {
        task.cancel();
    }
    public static ScheduleVelocityTask of(int taskId, ScheduledTask task) {
        return new ScheduleVelocityTask(taskId, task);
    }
}
