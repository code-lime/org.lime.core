package org.lime.core.common.reflection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.utils.execute.Callable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public record ReflectionConstructor<T>(Constructor<T> target)
        implements ReflectionAccessible<Constructor<T>, ReflectionConstructor<T>>, ReflectionMember<Constructor<T>, ReflectionConstructor<T>> {
    public static <T> ReflectionConstructor<T> of(Constructor<T> method) {
        return new ReflectionConstructor<>(method);
    }

    public static <T> ReflectionConstructor<T> of(Class<T> tClass, Class<?>... args) {
        return of(Reflection.constructor(tClass, args));
    }

    public static String constructorToString(Constructor<?> constructor) {
        return constructorToString(constructor.getDeclaringClass(), constructor.getParameterTypes());
    }
    public static String constructorToString(Class<?> tClass, Class<?> @Nullable [] argTypes) {
        return tClass.getName() + ".<ctor>" + Reflection.argsToString(argTypes);
    }

    @Override
    public ReflectionConstructor<T> self() {
        return this;
    }

    public T newInstance(Object... args) {
        try {
            return target.newInstance(args);
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public Object call(Object[] args) {
        return newInstance(args);
    }

    public Callable lambda() {
        return Lambda.lambda(target);
    }
    public <J>J lambda(Class<J> tClass) {
        return Lambda.lambda(target, tClass);
    }
    public <J>J lambda(Class<J> tClass, Method invoke) {
        return Lambda.lambda(target, tClass, invoke);
    }

    @Override
    public @NotNull String toString() {
        return constructorToString(target);
    }
}
