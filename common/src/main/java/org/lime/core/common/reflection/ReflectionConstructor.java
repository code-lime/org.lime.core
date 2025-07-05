package org.lime.core.common.reflection;

import org.lime.core.common.system.execute.Callable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public record ReflectionConstructor<T>(Constructor<T> constructor) {
    public static <T> ReflectionConstructor<T> of(Constructor<T> method) {
        return new ReflectionConstructor<>(method);
    }

    public static <T> ReflectionConstructor<T> of(Class<T> tClass, Class<?>... args) {
        return of(Reflection.constructor(tClass, args));
    }

    public ReflectionConstructor<T> access() {
        Reflection.access(constructor);
        return this;
    }

    public T newInstance(Object... args) {
        try {
            return constructor.newInstance(args);
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public Object call(Object[] args) {
        return newInstance(args);
    }

    public Callable lambda() {
        return Lambda.lambda(constructor);
    }
    public <J>J lambda(Class<J> tClass) {
        return Lambda.lambda(constructor, tClass);
    }
    public <J>J lambda(Class<J> tClass, Method invoke) {
        return Lambda.lambda(constructor, tClass, invoke);
    }
}
