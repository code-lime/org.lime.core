package org.lime.core.common.utils.execute;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Consumer;

public interface Action1<T0> extends Consumer<T0>, Callable {
    void invoke(T0 arg0);
    @Override default void accept(T0 t0) { invoke(t0); }
    @Override default Object call(Object[] args) { invoke((T0) args[0]); return null; }
    default Action1<T0> andThen(Action1<? super T0> after) {
        return (T0 t) -> {
            invoke(t);
            after.invoke(t);
        };
    }
    static <T0> Action1<T0> of(Method method) {
        return Modifier.isStatic(method.getModifiers()) ?
                t0 -> Execute.invoke(method, null, new Object[]{t0}) :
                t0 -> Execute.invoke(method, t0, new Object[]{});
    }
}
