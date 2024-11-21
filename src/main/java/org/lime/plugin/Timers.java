package org.lime.plugin;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public class Timers {
    public interface IRunnable extends Runnable { }

    public static synchronized BukkitTask runTaskLater(IRunnable callback, ITimer plugin, long delay) {
        if (!plugin.isEnabled()) {
            plugin._logOP("Can't run timer. Plugin is disable");
            return null;
        }
        return Bukkit.getScheduler().runTaskLater(plugin, callback, delay);
    }
    public static synchronized BukkitTask runTaskTimer(IRunnable callback, ITimer plugin, long wait, long delay) {
        if (!plugin.isEnabled()) {
            plugin._logOP("Can't run timer. Plugin is disable");
            return null;
        }
        return Bukkit.getScheduler().runTaskTimer(plugin, callback, wait, delay);
    }
    public static synchronized BukkitTask runTaskLaterAsynchronously(IRunnable callback, ITimer plugin, long delay) {
        if (!plugin.isEnabled()) {
            plugin._logOP("Can't run timer. Plugin is disable");
            return null;
        }
        return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, callback, delay);
    }
    public static synchronized BukkitTask runTaskTimerAsynchronously(IRunnable callback, ITimer plugin, long wait, long delay) {
        if (!plugin.isEnabled()) {
            plugin._logOP("Can't run timer. Plugin is disable");
            return null;
        }
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, callback, wait, delay);
    }
}
