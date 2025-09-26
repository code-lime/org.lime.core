package org.lime.core.velocity.tasks;

import com.velocitypowered.api.scheduler.Scheduler;
import org.lime.core.common.utils.ScheduleTask;
import org.lime.core.common.services.ScheduleTaskService;
import org.lime.core.common.utils.execute.Action0;
import org.lime.core.velocity.BaseVelocityPlugin;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class VelocityScheduleTaskService
        implements ScheduleTaskService {
    private final BaseVelocityPlugin plugin;
    private final Scheduler scheduler;
    private final AtomicInteger taskIdIterator = new AtomicInteger(0);

    public VelocityScheduleTaskService(BaseVelocityPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = plugin.server.getScheduler();
    }

    @Override
    public ScheduleTask runNextTick(Action0 callback, boolean isSync) {
        return ScheduleVelocityTask.of(taskIdIterator.incrementAndGet(), this.scheduler
                .buildTask(plugin, callback)
                .delay(50, TimeUnit.MILLISECONDS)
                .schedule());
    }
    public ScheduleTask runWait(Action0 callback, boolean isSync, long wait) {
        return ScheduleVelocityTask.of(taskIdIterator.incrementAndGet(), this.scheduler
                .buildTask(plugin, callback)
                .delay(wait * 50, TimeUnit.MILLISECONDS)
                .schedule());
    }
    public ScheduleTask runLoop(Action0 callback, boolean isSync, long loop) {
        return ScheduleVelocityTask.of(taskIdIterator.incrementAndGet(), this.scheduler
                .buildTask(plugin, callback)
                .repeat(loop * 50, TimeUnit.MILLISECONDS)
                .schedule());
    }
    public ScheduleTask runLoop(Action0 callback, boolean isSync, long wait, long loop) {
        return ScheduleVelocityTask.of(taskIdIterator.incrementAndGet(), this.scheduler
                .buildTask(plugin, callback)
                .delay(wait * 50, TimeUnit.MILLISECONDS)
                .repeat(loop * 50, TimeUnit.MILLISECONDS)
                .schedule());
    }
}
