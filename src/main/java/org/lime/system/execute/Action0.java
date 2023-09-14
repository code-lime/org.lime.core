package org.lime.system.execute;

import org.lime._system;
import org.lime.plugin.Timers;

import java.lang.reflect.Method;

public interface Action0 extends Timers.IRunnable, AutoCloseable, ICallable {
    void invoke();

    @Override
    default void run() {
        invoke();
    }

    @Override
    default void close() {
        invoke();
    }

    @Override
    default Object call(Object[] args) {
        invoke();
        return null;
    }

    static Action0 of(Method method) {
        return () -> Execute.invoke(method, null, new Object[0]);
    }
}
