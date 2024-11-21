package org.lime.reflection;

import org.lime.system.execute.ICallable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public record ReflectionConstructor<T>(Constructor<T> constructor) implements ICallable {
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

    @Override
    public Object call(Object[] args) {
        return newInstance(args);
    }

    public <J extends ICallable> J build(Class<J> tClass) {
        return build(tClass, "invoke");
    }

    public <J extends ICallable> J build(Class<J> tClass, String invokable) {
        return createProxy(tClass, invokable);
    }
}
