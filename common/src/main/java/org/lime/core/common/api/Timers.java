package org.lime.core.common.api;

import org.lime.core.common.api.tasks.ScheduleTask;
import org.lime.core.common.system.execute.Action0;

public class Timers {
    public interface IRunnable extends Runnable { }

    public static synchronized ScheduleTask runTaskLater(Action0 callback, BaseTimer instance, long delay) {
        if (!instance.isEnabled()) {
            instance.$logOP("Can't run timer. Plugin is disable");
            return null;
        }
        return instance.taskService().runWait(callback, true, delay);
    }
    public static synchronized ScheduleTask runTaskTimer(Action0 callback, BaseTimer instance, long wait, long delay) {
        if (!instance.isEnabled()) {
            instance.$logOP("Can't run timer. Plugin is disable");
            return null;
        }
        return instance.taskService().runLoop(callback, true, wait, delay);
    }
    public static synchronized ScheduleTask runTaskLaterAsynchronously(Action0 callback, BaseTimer instance, long delay) {
        if (!instance.isEnabled()) {
            instance.$logOP("Can't run timer. Plugin is disable");
            return null;
        }
        return instance.taskService().runWait(callback, false, delay);
    }
    public static synchronized ScheduleTask runTaskTimerAsynchronously(Action0 callback, BaseTimer instance, long wait, long delay) {
        if (!instance.isEnabled()) {
            instance.$logOP("Can't run timer. Plugin is disable");
            return null;
        }
        return instance.taskService().runLoop(callback, false, wait, delay);
    }
}
