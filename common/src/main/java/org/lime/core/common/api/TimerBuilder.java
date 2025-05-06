package org.lime.core.common.api;

import org.lime.core.common.api.tasks.ScheduleTask;
import org.lime.core.common.system.execute.*;
import org.lime.core.common.system.tuple.*;

public class TimerBuilder {
    private final BaseTimer instance;
    private final Action0 callback;
    private final Action0 next;
    private final long wait;
    private final long loop;
    private final boolean async;

    private TimerBuilder(BaseTimer instance, Action0 callback, Action0 next, long wait, long loop, boolean async) {
        this.instance = instance;
        this.callback = callback;
        this.next = next;
        this.wait = Math.max(wait, 1);
        this.loop = Math.max(loop, 1);
        this.async = async;
    }

    public static TimerBuilder create(BaseTimer instance) { return new TimerBuilder(instance, null, null, 1, 1, false); }

    public TimerBuilder withCallback(Action0 callback) { return new TimerBuilder(instance, callback, next, wait, loop, async); }
    public TimerBuilder withCallback(Action1<Double> callback) {
        Tuple1<Long> buff = Tuple.of(System.currentTimeMillis());
        return withCallback(() -> {
            long now = System.currentTimeMillis();
            callback.invoke((now - buff.val0) / (loop * 50.0));
            buff.val0 = now;
        });
    }
    public TimerBuilder withCallbackTicks(Action1<Long> callback) {
        Tuple1<Long> buff = Tuple.of(System.currentTimeMillis());
        return withCallback(() -> {
            long now = System.currentTimeMillis();
            callback.invoke(now - buff.val0);
            buff.val0 = now;
        });
    }
    public TimerBuilder withNext(Action0 next) { return new TimerBuilder(instance, callback, next, wait, loop, async); }
    public TimerBuilder withWait(double wait) { return withWaitTicks((long) (wait * 20)); }
    public TimerBuilder withLoop(double loop) { return withLoopTicks((long) (loop * 20)); }
    public TimerBuilder withWaitTicks(long wait) { return new TimerBuilder(instance, callback, next, wait, loop, async); }
    public TimerBuilder withLoopTicks(long loop) { return new TimerBuilder(instance, callback, next, wait, loop, async); }
    public TimerBuilder withAsync(boolean async) { return new TimerBuilder(instance, callback, next, wait, loop, async); }
    public TimerBuilder setAsync() { return this.withAsync(true); }
    public TimerBuilder setSync() { return this.withAsync(false); }

    public ScheduleTask run() { return Type.run(this); }

    private enum Type {
        SYNC_ONCE(v -> !v.async && v.loop == -1, v -> Timers.runTaskLater(v.callback, v.instance, v.wait)),
        ASYNC_ONCE(v -> v.async && v.loop == -1, v -> Timers.runTaskLaterAsynchronously(v.callback, v.instance, v.wait)),
        SYNC_REPEAT(v -> !v.async && v.loop != -1, v -> Timers.runTaskTimer(v.callback, v.instance, v.wait, v.loop)),
        ASYNC_REPEAT(v -> v.async && v.loop != -1, v -> Timers.runTaskTimerAsynchronously(v.callback, v.instance, v.wait, v.loop));

        private final Func1<TimerBuilder, Boolean> isDo;
        private final Func1<TimerBuilder, ScheduleTask> run;

        Type(Func1<TimerBuilder, Boolean> isDo, Func1<TimerBuilder, ScheduleTask> run) {
            this.isDo = isDo;
            this.run = run;
        }

        public static ScheduleTask run(TimerBuilder timer) {
            if (!timer.instance.isEnabled()) {
                timer.instance.$logOP("Can't run timer. Plugin is disable");
                return null;
            }
            for (Type t : Type.values()) {
                if (t.isDo.invoke(timer))
                    return t.run.invoke(timer);
            }
            throw new IllegalArgumentException("timer type error");
        }
    }
}
