package org.lime.core.common.api.tasks;

import org.lime.core.common.system.execute.Action0;

public interface ScheduleTaskService {
    ScheduleTask runNextTick(Action0 callback, boolean isSync);
    ScheduleTask runWait(Action0 callback, boolean isSync, long wait);
    ScheduleTask runLoop(Action0 callback, boolean isSync, long loop);
    ScheduleTask runLoop(Action0 callback, boolean isSync, long wait, long loop);
}
