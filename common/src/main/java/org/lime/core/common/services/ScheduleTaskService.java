package org.lime.core.common.services;

import org.lime.core.common.utils.ScheduleTask;
import org.lime.core.common.utils.TimerBuilder;
import org.lime.core.common.utils.execute.Action0;

public interface ScheduleTaskService {
    ScheduleTask runNextTick(Action0 callback, boolean isSync);
    ScheduleTask runWait(Action0 callback, boolean isSync, long wait);
    ScheduleTask runLoop(Action0 callback, boolean isSync, long loop);
    ScheduleTask runLoop(Action0 callback, boolean isSync, long wait, long loop);

    default TimerBuilder builder() {
        return TimerBuilder.create(this);
    }
}
