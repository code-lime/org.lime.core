package org.lime.core.common.utils;

import org.lime.core.common.services.ScheduleTaskService;
import org.lime.core.common.utils.system.execute.Action0;
import org.lime.core.common.utils.system.execute.Action1;
import org.lime.core.common.utils.system.execute.Execute;
import org.lime.core.common.utils.system.execute.Func1;
import org.lime.core.common.utils.system.tuple.Tuple;
import org.lime.core.common.utils.system.tuple.Tuple1;

import java.time.Duration;

public record TimerBuilder(
        ScheduleTaskService instance,
        Action0 callback,
        Action0 next,
        long waitTicks,
        long loopTicks,
        boolean async) {
    public static TimerBuilder create(ScheduleTaskService instance) {
        return new TimerBuilder(instance, Execute.actionEmpty(), Execute.actionEmpty(), 0, 0, false);
    }

    public TimerBuilder withCallback(Action0 callback) {
        return new TimerBuilder(instance, callback, next, waitTicks, loopTicks, async);
    }
    public TimerBuilder withCallbackMs(Action1<Long> callback) {
        Tuple1<Long> buff = Tuple.of(System.currentTimeMillis());
        return withCallback(() -> {
            long now = System.currentTimeMillis();
            callback.invoke(now - buff.val0);
            buff.val0 = now;
        });
    }
    public TimerBuilder withNext(Action0 next) {
        return new TimerBuilder(instance, callback, next, waitTicks, loopTicks, async);
    }

    public TimerBuilder withWait(Duration wait) {
        return withWaitTicks(wait.toMillis() / 50);
    }
    public TimerBuilder withLoop(Duration loop) {
        return withLoopTicks(loop.toMillis() / 50);
    }
    public TimerBuilder withWaitTicks(long wait) {
        return new TimerBuilder(instance, callback, next, wait, loopTicks, async);
    }
    public TimerBuilder withLoopTicks(long loop) {
        return new TimerBuilder(instance, callback, next, waitTicks, loop, async);
    }

    public TimerBuilder withAsync(boolean async) {
        return new TimerBuilder(instance, callback, next, waitTicks, loopTicks, async);
    }
    public TimerBuilder setAsync() {
        return withAsync(true);
    }
    public TimerBuilder setSync() {
        return withAsync(false);
    }

    public ScheduleTask execute() {
        return Type.execute(this);
    }

    private enum Type {
        SYNC_ONCE(v -> !v.async && v.loopTicks == -1, v -> v.instance.runWait(v.callback, true, v.waitTicks)),
        ASYNC_ONCE(v -> v.async && v.loopTicks == -1, v -> v.instance.runWait(v.callback, false, v.waitTicks)),
        SYNC_REPEAT(v -> !v.async && v.loopTicks != -1, v -> v.instance.runLoop(v.callback, true, v.waitTicks, v.loopTicks)),
        ASYNC_REPEAT(v -> v.async && v.loopTicks != -1, v -> v.instance.runLoop(v.callback, false, v.waitTicks, v.loopTicks));

        private final Func1<TimerBuilder, Boolean> predicate;
        private final Func1<TimerBuilder, ScheduleTask> executor;

        Type(Func1<TimerBuilder, Boolean> predicate, Func1<TimerBuilder, ScheduleTask> executor) {
            this.predicate = predicate;
            this.executor = executor;
        }

        public static ScheduleTask execute(TimerBuilder timer) {
            for (Type t : Type.values()) {
                if (t.predicate.invoke(timer))
                    return t.executor.invoke(timer);
            }
            throw new IllegalArgumentException("timer type error");
        }
    }
}
