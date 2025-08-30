package org.lime.core.paper.tasks;

import com.google.inject.Inject;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.lime.core.common.utils.ScheduleTask;
import org.lime.core.common.services.ScheduleTaskService;
import org.lime.core.common.utils.system.execute.Action0;

import java.util.logging.Logger;

public class BukkitScheduleTaskService
        implements ScheduleTaskService {
    @Inject Logger logger;
    @Inject Plugin plugin;
    @Inject BukkitScheduler scheduler;

    private ScheduleTask disabled(Action0 callback) {
        logger.warning("Timer " + callback + " can't run. Plugin owner " + plugin.getName() + " is disabled");
        return ScheduleTask.disabled();
    }

    @Override
    public ScheduleTask runNextTick(Action0 callback, boolean isSync) {
        return plugin.isEnabled()
                ? ScheduleBukkitTask.of(isSync
                        ? this.scheduler.runTask(plugin, callback)
                        : this.scheduler.runTaskAsynchronously(plugin, callback))
                : disabled(callback);
    }
    @Override
    public ScheduleTask runWait(Action0 callback, boolean isSync, long wait) {
        return plugin.isEnabled()
                ? ScheduleBukkitTask.of(isSync
                        ? this.scheduler.runTaskLater(plugin, callback, wait)
                        : this.scheduler.runTaskLaterAsynchronously(plugin, callback, wait))
                : disabled(callback);
    }
    @Override
    public ScheduleTask runLoop(Action0 callback, boolean isSync, long loop) {
        return plugin.isEnabled()
                ? ScheduleBukkitTask.of(isSync
                        ? this.scheduler.runTaskTimer(plugin, callback, 0, loop)
                        : this.scheduler.runTaskTimerAsynchronously(plugin, callback, 0, loop))
                : disabled(callback);
    }
    @Override
    public ScheduleTask runLoop(Action0 callback, boolean isSync, long wait, long loop) {
        return plugin.isEnabled()
                ? ScheduleBukkitTask.of(isSync
                        ? this.scheduler.runTaskTimer(plugin, callback, wait, loop)
                        : this.scheduler.runTaskTimerAsynchronously(plugin, callback, wait, loop))
                : disabled(callback);
    }
}
