package org.lime.plugin;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.lime.timings.lib.TimerTimings;

public class Timers {
    public enum TimerType {
        StaticCore,
        TimerBuilder
    }

    public interface IRunnable extends Runnable { }

    public static synchronized BukkitTask runTaskLater(IRunnable callback, ITimer plugin, long delay, TimerType type) {
        if (!plugin.isEnabled()) {
            plugin._logOP("Can't run timer. Plugin is disable");
            return null;
        }
        return TimerTimings.of(Bukkit.getScheduler().runTaskLater(plugin, callback, delay), type);
    }
    public static synchronized BukkitTask runTaskTimer(IRunnable callback, ITimer plugin, long wait, long delay, TimerType type) {
        if (!plugin.isEnabled()) {
            plugin._logOP("Can't run timer. Plugin is disable");
            return null;
        }
        return TimerTimings.of(Bukkit.getScheduler().runTaskTimer(plugin, callback, wait, delay), type);
    }
    public static synchronized BukkitTask runTaskLaterAsynchronously(IRunnable callback, ITimer plugin, long delay, TimerType type) {
        if (!plugin.isEnabled()) {
            plugin._logOP("Can't run timer. Plugin is disable");
            return null;
        }
        return TimerTimings.of(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, callback, delay), type);
    }
    public static synchronized BukkitTask runTaskTimerAsynchronously(IRunnable callback, ITimer plugin, long wait, long delay, TimerType type) {
        if (!plugin.isEnabled()) {
            plugin._logOP("Can't run timer. Plugin is disable");
            return null;
        }
        return TimerTimings.of(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, callback, wait, delay), type);
    }
}
