package org.lime.system.execute;

import java.util.Optional;

public interface FuncEx1<T0, TResult> {
    TResult invoke(T0 arg0) throws Throwable;

    default Func1<T0, TResult> throwable() {
        return (arg0) -> {
            try {
                return this.invoke(arg0);
            } catch (Throwable e) {
                throw new IllegalArgumentException(e);
            }
        };
    }

    default Func1<T0, Optional<TResult>> optional() {
        return (arg0) -> {
            try {
                return Optional.ofNullable(this.invoke(arg0));
            } catch (Throwable e) {
                return Optional.empty();
            }
        };
    }
}
