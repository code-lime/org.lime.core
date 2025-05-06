package org.lime.core.common.api.tasks;

public interface ScheduleTask {
    int getTaskId();
    boolean isSync();
    boolean isCancelled();
    void cancel();
}
