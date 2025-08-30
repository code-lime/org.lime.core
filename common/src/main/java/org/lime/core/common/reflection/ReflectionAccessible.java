package org.lime.core.common.reflection;

import java.lang.reflect.AccessibleObject;

public interface ReflectionAccessible<T extends AccessibleObject, Self extends ReflectionAccessible<T, Self>>
        extends ReflectionBase<T, Self> {
    default Self access() {
        Reflection.access(target());
        return self();
    }
}
