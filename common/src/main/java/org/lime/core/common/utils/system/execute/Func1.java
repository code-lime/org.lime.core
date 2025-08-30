package org.lime.core.common.utils.system.execute;

import java.util.function.Function;

public interface Func1<T0, TResult> extends Function<T0, TResult>, Callable {
    TResult invoke(T0 arg0);

    @Override
    default TResult apply(T0 t0) {
        return invoke(t0);
    }

    @Override
    default Object call(Object[] args) {
        return invoke((T0) args[0]);
    }

    default Func1<T0, TResult> and(Func1<T0, TResult> after, Func2<TResult, TResult, TResult> combine) {
        return (T0 t) -> combine.invoke(invoke(t), after.invoke(t));
    }
}
