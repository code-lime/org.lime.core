package org.lime.plugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.lime.invokable.IInvokable;
import org.lime.system.execute.Action0;
import org.lime.system.execute.Action1;
import org.lime.system.execute.Func0;

public interface ITimer extends ILogger, Plugin {
    TimerBuilder _timer();

    default BukkitTask _nextTick(Timers.IRunnable callback) {
        return Timers.runTaskLater(callback, this, 0, Timers.TimerType.StaticCore);
    }
    default BukkitTask _onceNoCheck(Timers.IRunnable callback, double sec) {
        return Timers.runTaskLater(callback, this, (long)(sec * 20), Timers.TimerType.StaticCore);
    }
    default BukkitTask _once(Timers.IRunnable callback, double sec) {
        return Timers.runTaskLater(callback, this, (long)(sec * 20), Timers.TimerType.StaticCore);
    }
    default BukkitTask _onceTicks(Timers.IRunnable callback, long ticks) {
        return Timers.runTaskLater(callback, this, ticks, Timers.TimerType.StaticCore);
    }
    default BukkitTask _repeat(Timers.IRunnable callback, double sec) {
        return Timers.runTaskTimer(callback, this, (long)(sec * 20), (long)(sec * 20), Timers.TimerType.StaticCore);
    }
    default BukkitTask _repeatTicks(Timers.IRunnable callback, long ticks) {
        return Timers.runTaskTimer(callback, this, ticks, ticks, Timers.TimerType.StaticCore);
    }
    default BukkitTask _repeat(Timers.IRunnable callback, double wait, double sec) {
        return Timers.runTaskTimer(callback, this, (long)(wait * 20), (long)(sec * 20), Timers.TimerType.StaticCore);
    }
    default BukkitTask _repeatTicks(Timers.IRunnable callback, long wait, long ticks) {
        return Timers.runTaskTimer(callback, this, wait, ticks, Timers.TimerType.StaticCore);
    }
    default <T>void _repeat(T[] array, Action1<T> callback_part, Action0 callback_end, double sec, int inOneStep) {
        _ionce(array, array.length, 0, callback_part, callback_end, sec, inOneStep);
    }
    private <T>void _ionce(T[] array, int length, int index, Action1<T> callback, Action0 callback_end, double sec, int inOneStep) {
        if (index >= length) {
            callback_end.invoke();
            return;
        }
        _once(() -> {
            int maxIndex = index + inOneStep;
            _ionce(array, length, maxIndex, callback, callback_end, sec, inOneStep);
            maxIndex = Math.min(maxIndex, length);
            for (int i = index; i < maxIndex; i++)
                callback.invoke(array[i]);
        }, sec);
    }
    default BukkitTask _invokeAsync(Action0 async, Timers.IRunnable nextSync) {
        if (!isEnabled()) {
            _logOP("Can't run timer. Plugin is disable");
            return null;
        }
        BukkitScheduler scheduler = Bukkit.getScheduler();
        return scheduler.runTaskAsynchronously(this, () -> {
            async.invoke();
            if (nextSync == null) return;
            scheduler.scheduleSyncDelayedTask(this, nextSync);
        });
    }
    default <T>BukkitTask _invokeAsync(Func0<T> async, Action1<T> nextSync) {
        if (!isEnabled()) {
            _logOP("Can't run timer. Plugin is disable");
            return null;
        }
        BukkitScheduler scheduler = Bukkit.getScheduler();
        return scheduler.runTaskAsynchronously(this, () -> {
            T obj = async.invoke();
            if (nextSync == null) return;
            scheduler.scheduleSyncDelayedTask(this, () -> nextSync.invoke(obj));
        });
    }
    default void _invokeSync(Timers.IRunnable sync) {
        if (!isEnabled()) {
            _logOP("Can't run timer. Plugin is disable");
            return;
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, sync);
    }
    void _invokable(IInvokable invokable);
}
