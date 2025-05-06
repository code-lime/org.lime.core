package org.lime.core.common.system;

import org.lime.core.common.system.execute.Func0;

public class Lazy<T> {
    private T value;
    private boolean created;
    private final Func0<T> creator;

    public Lazy(T value) {
        this.creator = () -> value;
        this.value = value;
        this.created = true;
    }
    public Lazy(Func0<T> creator) {
        this.creator = creator;
        this.value = null;
        this.created = false;
    }

    public T value() {
        if (!created) {
            value = creator.invoke();
            created = true;
        }
        return value;
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
