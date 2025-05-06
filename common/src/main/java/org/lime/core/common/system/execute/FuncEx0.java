package org.lime.core.common.system.execute;

import java.util.Optional;

public interface FuncEx0<TResult> extends Callable {
    TResult invoke() throws Throwable;

    default Func0<TResult> throwable() {
        return () -> {
            try {
                return this.invoke();
            } catch (Throwable e) {
                throw new IllegalArgumentException(e);
            }
        };
    }

    default Func0<Optional<TResult>> optional() {
        return () -> {
            try {
                return Optional.ofNullable(this.invoke());
            } catch (Throwable e) {
                return Optional.empty();
            }
        };
    }
    @Override default Object call(Object[] args) { return throwable().call(args); }
}
