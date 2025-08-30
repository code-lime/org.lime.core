package org.lime.core.common.reflection;

public interface ReflectionBase<T, Self extends ReflectionBase<T, Self>> {
    Self self();
    T target();
}
