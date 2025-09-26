package org.lime.core.common.reflection;

import org.lime.core.common.utils.execute.Callable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public record ReflectionField<T>(Field target)
        implements ReflectionAccessible<Field, ReflectionField<T>>, ReflectionMember<Field, ReflectionField<T>> {
    public static <T> ReflectionField<T> of(Field field) {
        return new ReflectionField<>(field);
    }
    public static <T> ReflectionField<T> of(Class<?> tClass, String name) {
        return of(Reflection.get(tClass, name));
    }
    public static <T> ReflectionField<T> ofMojang(Class<?> tClass, String mojangName) {
        return of(Reflection.getFirst(tClass, m -> Reflection.name(m).equals(mojangName))
                .orElseThrow(() -> new IllegalArgumentException(new NoSuchFieldException(mojangName))));
    }

    @Override
    public ReflectionField<T> self() {
        return this;
    }

    public ReflectionField<T> nonFinal() {
        Reflection.nonFinal(target);
        return this;
    }

    public void set(Object instance, T value) {
        try {
            target.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public T get(Object instance) {
        try {
            return (T) target.get(instance);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public Callable getter() {
        return Lambda.getter(target);
    }
    public <J>J getter(Class<J> tClass) {
        return Lambda.getter(target, tClass);
    }
    public <J>J getter(Class<J> tClass, Method invoke) {
        return Lambda.getter(target, tClass, invoke);
    }

    public Callable setter() {
        return Lambda.setter(target);
    }
    public <J>J setter(Class<J> tClass) {
        return Lambda.setter(target, tClass);
    }
    public <J>J setter(Class<J> tClass, Method invoke) {
        return Lambda.setter(target, tClass, invoke);
    }
}
