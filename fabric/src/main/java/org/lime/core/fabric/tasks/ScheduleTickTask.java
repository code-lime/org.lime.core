package org.lime.core.fabric.tasks;

import org.jetbrains.annotations.Nullable;
import org.lime.core.common.utils.ScheduleTask;
import org.lime.core.common.utils.execute.Action0;
import org.lime.core.common.utils.tuple.LockTuple1;
import org.lime.core.common.utils.tuple.Tuple;

import java.util.concurrent.atomic.AtomicLong;

public abstract class ScheduleTickTask implements ScheduleTask {
    private final int id;
    private final boolean isSync;
    private final Action0 callback;

    private final LockTuple1<Boolean> isCancelled = Tuple.lock(false);

    public ScheduleTickTask(int id, boolean isSync, Action0 callback) {
        this.id = id;
        this.isSync = isSync;
        this.callback = callback;
    }

    @Override
    public int getTaskId() {
        return this.id;
    }
    @Override
    public boolean isSync() {
        return this.isSync;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled.get0();
    }
    @Override
    public void cancel() {
        isCancelled.set0(true);
    }

    protected enum TickStatus {
        Tick(true, false),
        EndTick(true, true),
        Wait(false, false),
        Remove(false, true);

        public final boolean isTick;
        public final boolean isRemove;

        TickStatus(boolean isTick, boolean isRemove) {
            this.isTick = isTick;
            this.isRemove = isRemove;
        }
    }
    protected abstract TickStatus tickStatus();

    public boolean isTickRemove() {
        if (isCancelled()) return true;
        TickStatus status = tickStatus();
        if (status.isTick) callback.invoke();
        return status.isRemove;
    }

    public static ScheduleTickTask wait(int id, boolean isSync, long waitDelay, Action0 callback) {
        if (waitDelay < 0)
            throw new IllegalArgumentException("waitDelay=" + waitDelay + " can't be unless zero");
        return new ScheduleTickTask(id, isSync, callback) {
            private final AtomicLong tickCounter = new AtomicLong(-waitDelay);

            @Override
            protected TickStatus tickStatus() {
                long tick = tickCounter.getAndIncrement();
                if (tick < 0) return TickStatus.Wait;
                return TickStatus.EndTick;
            }
        };
    }
    public static ScheduleTickTask loop(int id, boolean isSync, @Nullable Long waitDelay, long loopDelay, Action0 callback) {
        if (waitDelay != null && waitDelay < 0)
            throw new IllegalArgumentException("waitDelay=" + waitDelay + " can't be unless zero");
        if (loopDelay < 0)
            throw new IllegalArgumentException("loopDelay=" + loopDelay + " can't be unless zero");
        return new ScheduleTickTask(id, isSync, callback) {
            private final AtomicLong tickCounter = new AtomicLong(waitDelay == null ? 0 : -waitDelay);

            @Override
            protected TickStatus tickStatus() {
                long tick = tickCounter.getAndIncrement();
                if (tick < 0) return TickStatus.Wait;
                else if (tick == 0) {
                    if (waitDelay != null || loopDelay == 0)
                        return TickStatus.Tick;
                }
                else if (loopDelay == 0 || tick % loopDelay == 0)
                    return TickStatus.Tick;
                return TickStatus.Wait;
            }
        };
    }
}
