package org.lime.core.common.services;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.inject.Inject;
import org.lime.core.common.utils.ScheduleTask;
import org.lime.core.common.utils.execute.Action0;
import org.lime.core.common.utils.execute.Action1;
import org.lime.core.common.utils.execute.Func0;
import org.lime.core.common.api.Service;

public class TimerUtility
        implements Service {
    @Inject ScheduleTaskService taskService;

    @CanIgnoreReturnValue
    public ScheduleTask nextTick(Action0 callback) {
        return taskService.runWait(callback, true, 0);
    }
    @CanIgnoreReturnValue
    public ScheduleTask onceNoCheck(Action0 callback, double sec) {
        return taskService.runWait(callback, true, (long)(sec * 20));
    }
    @CanIgnoreReturnValue
    public ScheduleTask once(Action0 callback, double sec) {
        return taskService.runWait(callback, true, (long)(sec * 20));
    }
    @CanIgnoreReturnValue
    public ScheduleTask onceTicks(Action0 callback, long ticks) {
        return taskService.runWait(callback, true, ticks);
    }
    @CanIgnoreReturnValue
    public ScheduleTask repeat(Action0 callback, double sec) {
        return taskService.runLoop(callback, true, (long)(sec * 20), (long)(sec * 20));
    }
    @CanIgnoreReturnValue
    public ScheduleTask repeatTicks(Action0 callback, long ticks) {
        return taskService.runLoop(callback, true, ticks, ticks);
    }
    @CanIgnoreReturnValue
    public ScheduleTask repeat(Action0 callback, double wait, double sec) {
        return taskService.runLoop(callback, true, (long)(wait * 20), (long)(sec * 20));
    }
    @CanIgnoreReturnValue
    public ScheduleTask repeatTicks(Action0 callback, long wait, long ticks) {
        return taskService.runLoop(callback, true, wait, ticks);
    }
    public <T>void repeat(T[] array, Action1<T> callbackPart, Action0 callbackEnd, double sec, int inOneStep) {
        repeatPart(array, array.length, 0, callbackPart, callbackEnd, sec, inOneStep);
    }
    private <T>void repeatPart(T[] array, int length, int index, Action1<T> callback, Action0 callbackEnd, double sec, int inOneStep) {
        if (index >= length) {
            callbackEnd.invoke();
            return;
        }
        once(() -> {
            int maxIndex = index + inOneStep;
            repeatPart(array, length, maxIndex, callback, callbackEnd, sec, inOneStep);
            maxIndex = Math.min(maxIndex, length);
            for (int i = index; i < maxIndex; i++)
                callback.invoke(array[i]);
        }, sec);
    }
    @CanIgnoreReturnValue
    public ScheduleTask invokeAsync(Action0 async, Action0 nextSync) {
        return taskService.runNextTick(() -> {
            async.invoke();
            if (nextSync == null) return;
            taskService.runNextTick(nextSync, true);
        }, false);
    }
    @CanIgnoreReturnValue
    public <T>ScheduleTask invokeAsync(Func0<T> async, Action1<T> nextSync) {
        return taskService.runNextTick(() -> {
            T obj = async.invoke();
            if (nextSync == null) return;
            taskService.runNextTick(() -> nextSync.invoke(obj), true);
        }, false);
    }
    @CanIgnoreReturnValue
    public void invokeSync(Action0 sync) {
        taskService.runNextTick(sync, true);
    }
}
