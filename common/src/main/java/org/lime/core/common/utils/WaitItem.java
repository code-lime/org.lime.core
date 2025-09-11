package org.lime.core.common.utils;

import org.lime.core.common.utils.system.execute.Func1;

import java.time.Duration;

public record WaitItem<T>(T item, long time) {
    public boolean isDone() {
        return isDone(System.currentTimeMillis());
    }
    public boolean isDone(long now) {
        return time < now;
    }

    public int waitSeconds() {
        return waitSeconds(System.currentTimeMillis());
    }
    public int waitSeconds(long now) {
        return (int) (Math.max(0, time - now) / 1000);
    }

    public WaitItem<T> change(T otherItem) {
        return new WaitItem<>(otherItem, time);
    }
    public <J> WaitItem<J> map(Func1<T, J> mapItem) {
        return new WaitItem<>(mapItem.invoke(item), time);
    }

    public static <T> WaitItem<T> of(T item, Duration delay) {
        return of(item, System.currentTimeMillis(), delay);
    }
    public static <T> WaitItem<T> of(T item, long now, Duration delay) {
        return new WaitItem<>(item, now + delay.toMillis());
    }
}
