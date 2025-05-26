package org.lime.core.fabric.tasks;

import org.lime.core.common.api.BaseLogger;
import org.lime.core.common.api.tasks.ScheduleTask;
import org.lime.core.common.api.tasks.ScheduleTaskService;
import org.lime.core.common.system.execute.Action0;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class FabricScheduleTaskService implements ScheduleTaskService, Closeable {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final AtomicInteger taskIdentifier = new AtomicInteger();

    private final ConcurrentHashMap<Integer, ScheduleTickTask> syncTasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, ScheduleTickTask> asyncTasks = new ConcurrentHashMap<>();

    private final BaseLogger logger;

    public FabricScheduleTaskService(BaseLogger logger) {
        this.logger = logger;
        scheduler.scheduleAtFixedRate(() -> tick(asyncTasks), 0, 50, TimeUnit.MILLISECONDS);
    }

    @Override
    public ScheduleTask runNextTick(Action0 callback, boolean isSync) {
        return runWait(callback, isSync, 0);
    }
    @Override
    public ScheduleTask runWait(Action0 callback, boolean isSync, long wait) {
        return registerTask(ScheduleTickTask.wait(taskIdentifier.incrementAndGet(), isSync, wait, callback));
    }
    @Override
    public ScheduleTask runLoop(Action0 callback, boolean isSync, long loop) {
        return registerTask(ScheduleTickTask.loop(taskIdentifier.incrementAndGet(), isSync, null, loop, callback));
    }
    @Override
    public ScheduleTask runLoop(Action0 callback, boolean isSync, long wait, long loop) {
        return registerTask(ScheduleTickTask.loop(taskIdentifier.incrementAndGet(), isSync, wait, loop, callback));
    }

    private ScheduleTask registerTask(ScheduleTickTask task) {
        (task.isSync() ? syncTasks : asyncTasks).put(task.getTaskId(), task);
        return task;
    }

    private void tick(ConcurrentHashMap<Integer, ScheduleTickTask> tasks) {
        tasks.values().removeIf(v -> {
            try {
                return v.isTickRemove();
            } catch (Exception e) {
                logger.$log("Error tick: " + v.getTaskId());
                logger.$logStackTrace(e);
                return false;
            }
        });
    }
    public void serverTick() {
        tick(syncTasks);
    }

    @Override
    public void close() throws IOException {
        try {
            if (scheduler instanceof AutoCloseable)
                ((AutoCloseable)scheduler).close();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
