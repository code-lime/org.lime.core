package org.lime.core.common.utils;

public interface ScheduleTask
        extends Disposable {
    ScheduleTask DISABLED = new ScheduleTask() {
        @Override
        public int getTaskId() {
            return -1;
        }
        @Override
        public boolean isSync() {
            return true;
        }
        @Override
        public boolean isCancelled() {
            return true;
        }
        @Override
        public void cancel() {

        }
    };

    int getTaskId();
    boolean isSync();
    boolean isCancelled();
    void cancel();

    @Override
    default void close() {
        this.cancel();
    }

    static ScheduleTask disabled() {
        return DISABLED;
    }
}
