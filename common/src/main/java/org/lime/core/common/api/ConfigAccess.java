package org.lime.core.common.api;

public interface ConfigAccess<T> {
    T value();
    void save(T value);

    default boolean update() {
        return false;
    }
}
