package org.lime.reflection;

import org.lime.system.execute.ICallable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public record ReflectionConstructor<T>(Constructor<T> constructor) {
    public static <T> ReflectionConstructor<T> of(Constructor<T> method) {
        return new ReflectionConstructor<>(method);
    }

    public static <T> ReflectionConstructor<T> of(Class<T> tClass, Class<?>... args) {
        return of(Reflection.constructor(tClass, args));
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

    public ICallable lambda() {
        return Lambda.lambda(constructor);
    }
    public <J extends ICallable>J lambda(Class<J> tClass) {
        return Lambda.lambda(constructor, tClass);
    }
    public <J>J lambda(Class<J> tClass, String invokeName) {
        return Lambda.lambda(constructor, tClass, invokeName);
    }
}
