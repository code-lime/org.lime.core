package org.lime.system.execute;

import java.util.function.Supplier;

public interface Func0<TResult> extends ICallable, Supplier<TResult> {
    TResult invoke();

    @Override
    default Object call(Object[] args) {
        return invoke();
    }

    @Override
    default TResult get() {
        return invoke();
    }
}
