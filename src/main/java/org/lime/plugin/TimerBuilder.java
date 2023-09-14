package org.lime.plugin;

import org.bukkit.scheduler.BukkitTask;
import org.lime.system.execute.*;
import org.lime.system.toast.*;

public class TimerBuilder {
    private final ITimer plugin;
    private final Timers.IRunnable callback;
    private final Timers.IRunnable next;
    private final long wait;
    private final long loop;
    private final boolean async;

    private TimerBuilder(ITimer plugin, Timers.IRunnable callback, Timers.IRunnable next, long wait, long loop, boolean async) {
        this.plugin = plugin;
        this.callback = callback;
        this.next = next;
        this.wait = Math.max(wait, 1);
        this.loop = Math.max(loop, 1);
        this.async = async;
    }

    public static TimerBuilder create(ITimer plugin) { return new TimerBuilder(plugin, null, null, 1, 1, false); }

    public TimerBuilder withCallback(Timers.IRunnable callback) { return new TimerBuilder(plugin, callback, next, wait, loop, async); }
    public TimerBuilder withCallback(Action1<Double> callback) {
        Toast1<Long> buff = Toast.of(System.currentTimeMillis());
        return withCallback(() -> {
            long now = System.currentTimeMillis();
            callback.invoke((now - buff.val0) / (loop * 50.0));
            buff.val0 = now;
        });
    }
    public TimerBuilder withCallbackTicks(Action1<Long> callback) {
        Toast1<Long> buff = Toast.of(System.currentTimeMillis());
        return withCallback(() -> {
            long now = System.currentTimeMillis();
            callback.invoke(now - buff.val0);
            buff.val0 = now;
        });
    }
    public TimerBuilder withNext(Timers.IRunnable next) { return new TimerBuilder(plugin, callback, next, wait, loop, async); }
    public TimerBuilder withWait(double wait) { return withWaitTicks((long) (wait * 20)); }
    public TimerBuilder withLoop(double loop) { return withLoopTicks((long) (loop * 20)); }
    public TimerBuilder withWaitTicks(long wait) { return new TimerBuilder(plugin, callback, next, wait, loop, async); }
    public TimerBuilder withLoopTicks(long loop) { return new TimerBuilder(plugin, callback, next, wait, loop, async); }
    public TimerBuilder withAsync(boolean async) { return new TimerBuilder(plugin, callback, next, wait, loop, async); }
    public TimerBuilder setAsync() { return this.withAsync(true); }
    public TimerBuilder setSync() { return this.withAsync(false); }

    public BukkitTask run() { return type.run(this); }

    private enum type {
        sync_once(v -> !v.async && v.loop == -1, v -> Timers.runTaskLater(v.callback, v.plugin, v.wait, Timers.TimerType.TimerBuilder)),
        async_once(v -> v.async && v.loop == -1, v -> Timers.runTaskLaterAsynchronously(v.callback, v.plugin, v.wait, Timers.TimerType.TimerBuilder)),
        sync_repeat(v -> !v.async && v.loop != -1, v -> Timers.runTaskTimer(v.callback, v.plugin, v.wait, v.loop, Timers.TimerType.TimerBuilder)),
        async_repeat(v -> v.async && v.loop != -1, v -> Timers.runTaskTimerAsynchronously(v.callback, v.plugin, v.wait, v.loop, Timers.TimerType.TimerBuilder));

        private final Func1<TimerBuilder, Boolean> isDo;
        private final Func1<TimerBuilder, BukkitTask> run;

        type(Func1<TimerBuilder, Boolean> isDo, Func1<TimerBuilder, BukkitTask> run) {
            this.isDo = isDo;
            this.run = run;
        }

        public static BukkitTask run(TimerBuilder timer) {
            if (!timer.plugin.isEnabled()) {
                timer.plugin._logOP("Can't run timer. Plugin is disable");
                return null;
            }
            for (type t : type.values()) {
                if (t.isDo.invoke(timer))
                    return t.run.invoke(timer);
            }
            throw new IllegalArgumentException("timer type error");
        }
    }
}
