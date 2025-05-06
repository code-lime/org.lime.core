package org.lime.core.paper.tasks;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.lime.core.common.api.tasks.ScheduleTask;
import org.lime.core.common.api.tasks.ScheduleTaskService;
import org.lime.core.common.system.execute.Action0;

public class BukkitScheduleTaskService
        implements ScheduleTaskService {
    private final JavaPlugin plugin;
    private final BukkitScheduler scheduler;

    public BukkitScheduleTaskService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = Bukkit.getScheduler();
    }

    @Override
    public ScheduleTask runNextTick(Action0 callback, boolean isSync) {
        return ScheduleBukkitTask.of(isSync
                ? this.scheduler.runTask(plugin, callback)
                : this.scheduler.runTaskAsynchronously(plugin, callback));
    }
    public ScheduleTask runWait(Action0 callback, boolean isSync, long wait) {
        return ScheduleBukkitTask.of(isSync
                ? this.scheduler.runTaskLater(plugin, callback, wait)
                : this.scheduler.runTaskLaterAsynchronously(plugin, callback, wait));
    }
    public ScheduleTask runLoop(Action0 callback, boolean isSync, long loop) {
        return ScheduleBukkitTask.of(isSync
                ? this.scheduler.runTaskTimer(plugin, callback, 0, loop)
                : this.scheduler.runTaskTimerAsynchronously(plugin, callback, 0, loop));
    }
    public ScheduleTask runLoop(Action0 callback, boolean isSync, long wait, long loop) {
        return ScheduleBukkitTask.of(isSync
                ? this.scheduler.runTaskTimer(plugin, callback, wait, loop)
                : this.scheduler.runTaskTimerAsynchronously(plugin, callback, wait, loop));
    }
}
