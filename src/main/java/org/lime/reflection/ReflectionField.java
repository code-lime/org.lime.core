package org.lime.reflection;

import org.lime.system.execute.ICallable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public record ReflectionField<T>(Field field) {
    public static <T> ReflectionField<T> of(Field field) {
        return new ReflectionField<>(field);
    }

    public static <T> ReflectionField<T> of(Class<?> tClass, String name) {
        return of(Reflection.get(tClass, name));
    }

    public static <T> ReflectionField<T> ofMojang(Class<?> tClass, String mojang_name) {
        return of(Reflection.getFirst(tClass, m -> Reflection.name(m).equals(mojang_name))
                .orElseThrow(() -> new IllegalArgumentException(new NoSuchFieldException(mojang_name))));
    }

    public ReflectionField<T> nonFinal() {
        Reflection.nonFinal(field);
        return this;
    }

    public ReflectionField<T> access() {
        Reflection.access(field);
        return this;
    }

    public void set(Object instance, T value) {
        try {
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public T get(Object instance) {
        try {
            return (T) field.get(instance);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public ICallable getter() {
        return Lambda.getter(field);
    }
    public <J>J getter(Class<J> tClass) {
        return Lambda.getter(field, tClass);
    }
    public <J>J getter(Class<J> tClass, Method invoke) {
        return Lambda.getter(field, tClass, invoke);
    }

    public ICallable setter() {
        return Lambda.setter(field);
    }
    public <J>J setter(Class<J> tClass) {
        return Lambda.setter(field, tClass);
    }
    public <J>J setter(Class<J> tClass, Method invoke) {
        return Lambda.setter(field, tClass, invoke);
    }
}
