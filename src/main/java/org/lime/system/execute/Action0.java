package org.lime.system.execute;

import org.lime.plugin.Timers;

import java.lang.reflect.Method;

public interface Action0 extends Timers.IRunnable, AutoCloseable, ICallable {
    void invoke();
    @Override default void run() { invoke(); }
    @Override default void close() { invoke(); }
    @Override default Object call(Object[] args) { invoke(); return null; }
    default Action0 andThen(Action0 after) {
        return () -> {
            invoke();
            after.invoke();
        };
    }
    static Action0 of(Method method) {
        return () -> Execute.invoke(method, null, new Object[0]);
    }
}
