package org.lime.system;

import org.lime._system;
import org.lime.system.execute.Func0;

public class Lazy<T> {
    private T value = null;
    private boolean created = false;
    private final Func0<T> creator;

    public Lazy(Func0<T> creator) {
        this.creator = creator;
    }

    public T value() {
        if (!created) {
            value = creator.invoke();
            created = true;
        }
        return value;
    }

    public static <T> Lazy<T> of(Func0<T> creator) {
        return new Lazy<>(creator);
    }

    @Override
    public String toString() {
        return String.valueOf(value());
    }
}
