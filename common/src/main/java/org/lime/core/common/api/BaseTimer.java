package org.lime.core.common.api;

import org.lime.core.common.invokable.BaseInvokable;
import org.lime.core.common.api.tasks.ScheduleTask;
import org.lime.core.common.api.tasks.ScheduleTaskService;
import org.lime.core.common.system.execute.Action0;
import org.lime.core.common.system.execute.Action1;
import org.lime.core.common.system.execute.Func0;

public interface BaseTimer extends BaseLogger {
    TimerBuilder $timer();
    ScheduleTaskService taskService();

    default ScheduleTask $nextTick(Action0 callback) {
        return Timers.runTaskLater(callback, this, 0);
    }
    default ScheduleTask $onceNoCheck(Action0 callback, double sec) {
        return Timers.runTaskLater(callback, this, (long)(sec * 20));
    }
    default ScheduleTask $once(Action0 callback, double sec) {
        return Timers.runTaskLater(callback, this, (long)(sec * 20));
    }
    default ScheduleTask $onceTicks(Action0 callback, long ticks) {
        return Timers.runTaskLater(callback, this, ticks);
    }
    default ScheduleTask $repeat(Action0 callback, double sec) {
        return Timers.runTaskTimer(callback, this, (long)(sec * 20), (long)(sec * 20));
    }
    default ScheduleTask $repeatTicks(Action0 callback, long ticks) {
        return Timers.runTaskTimer(callback, this, ticks, ticks);
    }
    default ScheduleTask $repeat(Action0 callback, double wait, double sec) {
        return Timers.runTaskTimer(callback, this, (long)(wait * 20), (long)(sec * 20));
    }
    default ScheduleTask $repeatTicks(Action0 callback, long wait, long ticks) {
        return Timers.runTaskTimer(callback, this, wait, ticks);
    }
    default <T>void $repeat(T[] array, Action1<T> callbackPart, Action0 callbackEnd, double sec, int inOneStep) {
        repeatPart(array, array.length, 0, callbackPart, callbackEnd, sec, inOneStep);
    }
    private <T>void repeatPart(T[] array, int length, int index, Action1<T> callback, Action0 callbackEnd, double sec, int inOneStep) {
        if (index >= length) {
            callbackEnd.invoke();
            return;
        }
        $once(() -> {
            int maxIndex = index + inOneStep;
            repeatPart(array, length, maxIndex, callback, callbackEnd, sec, inOneStep);
            maxIndex = Math.min(maxIndex, length);
            for (int i = index; i < maxIndex; i++)
                callback.invoke(array[i]);
        }, sec);
    }
    default ScheduleTask $invokeAsync(Action0 async, Action0 nextSync) {
        if (!isEnabled()) {
            $logOP("Can't run timer. Plugin is disable");
            return null;
        }
        ScheduleTaskService scheduler = taskService();
        return scheduler.runNextTick(() -> {
            async.invoke();
            if (nextSync == null) return;
            scheduler.runNextTick(nextSync, true);
        }, false);
    }
    default <T>ScheduleTask $invokeAsync(Func0<T> async, Action1<T> nextSync) {
        if (!isEnabled()) {
            $logOP("Can't run timer. Plugin is disable");
            return null;
        }
        ScheduleTaskService scheduler = taskService();
        return scheduler.runNextTick(() -> {
            T obj = async.invoke();
            if (nextSync == null) return;
            scheduler.runNextTick(() -> nextSync.invoke(obj), true);
        }, false);
    }
    default void $invokeSync(Action0 sync) {
        if (!isEnabled()) {
            $logOP("Can't run timer. Plugin is disable");
            return;
        }
        taskService().runNextTick(sync, true);
    }
    void $invokable(BaseInvokable invokable);
}
