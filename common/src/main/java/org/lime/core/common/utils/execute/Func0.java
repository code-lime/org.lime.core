package org.lime.core.common.utils.execute;

import java.util.function.Supplier;

public interface Func0<TResult> extends Callable, Supplier<TResult> {
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
