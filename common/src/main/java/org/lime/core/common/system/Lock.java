package org.lime.core.common.system;

import org.lime.core.common.system.execute.Action0;
import org.lime.core.common.system.execute.Func0;

import java.util.concurrent.locks.ReentrantLock;

public class Lock {
    private final ReentrantLock _lock;

    public Lock() {
        _lock = new ReentrantLock();
    }

    public static Lock create() {
        return new Lock();
    }

    public Action0 lock() {
        _lock.lock();
        return _lock::unlock;
    }

    public void invoke(Action0 invoke) {
        try (Action0 a = lock()) {
            invoke.invoke();
        }
    }

    public <T> T invoke(Func0<T> invoke) {
        try (Action0 a = lock()) {
            return invoke.invoke();
        }
    }
}
