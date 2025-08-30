package org.lime.core.common.utils.system;

import org.lime.core.common.utils.system.execute.Func0;
import org.lime.core.common.utils.system.tuple.LockTuple1;
import org.lime.core.common.utils.system.tuple.Tuple;

public class Lazy<T> {
    private T value;
    private final LockTuple1<Boolean> created;
    private final Func0<T> creator;

    public Lazy(T value) {
        this.creator = () -> value;
        this.value = value;
        this.created = Tuple.lock(true);
    }
    public Lazy(Func0<T> creator) {
        this.creator = creator;
        this.value = null;
        this.created = Tuple.lock(false);
    }

    public T value() {
        return created.call(v -> {
            if (!v.val0) {
                value = creator.invoke();
                v.val0 = true;
            }
            return value;
        });
    }

    public static <T> Lazy<T> of(T value) {
        return new Lazy<>(value);
    }
    public static <T> Lazy<T> of(Func0<T> creator) {
        return new Lazy<>(creator);
    }

    @Override
    public String toString() {
        return String.valueOf(value());
    }
}
