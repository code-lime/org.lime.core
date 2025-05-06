package org.lime.core.common.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
public class ReflectionDynamic<T> {
    public final T value;
    public final Class<T> tClass;

    public ReflectionDynamic(T value, Class<T> tClass) {
        this.value = value;
        this.tClass = tClass;
    }

    public static <T> ReflectionDynamic<T> of(T value, Class<T> tClass) {
        return new ReflectionDynamic<>(value, tClass);
    }

    public static <T> ReflectionDynamic<T> ofValue(T value) {
        return of(value, value == null ? null : (Class<T>) value.getClass());
    }

    public static <T> ReflectionDynamic<T> ofStatic(T value, Class<T> tClass) {
        return of(null, tClass);
    }

    public static <T> ReflectionDynamic<T> ofStatic(Class<T> tClass) {
        return of(null, tClass);
    }

    public <I> ReflectionDynamic<I> cast(Class<I> tClass) {
        return of((I) value, tClass);
    }

    public <I> ReflectionDynamic<I> invoke(String name, ReflectionDynamic<?>... args) {
        int length = args.length;
        Class<?>[] classes = new Class[length];
        Object[] values = new Object[length];
        for (int i = 0; i < length; i++) {
            ReflectionDynamic<?> arg = args[i];
            classes[i] = arg.tClass;
            values[i] = arg.value;
        }
        return (ReflectionDynamic<I>) ofValue(ReflectionMethod.of(tClass, name, classes).call(value, values));
    }

    public <I> ReflectionDynamic<I> invokeMojang(String name, ReflectionDynamic<?>... args) {
        int length = args.length;
        Class<?>[] classes = new Class[length];
        Object[] values = new Object[length];
        for (int i = 0; i < length; i++) {
            ReflectionDynamic<?> arg = args[i];
            classes[i] = arg.tClass;
            values[i] = arg.value;
        }
        return (ReflectionDynamic<I>) ofValue(ReflectionMethod.ofMojang(tClass, name, classes).call(value, values));
    }

    public <I> ReflectionDynamic<I> get(String name) {
        return (ReflectionDynamic<I>) ofValue(ReflectionField.of(tClass, name).get(value));
    }

    public <I> ReflectionDynamic<I> getMojang(String name) {
        return (ReflectionDynamic<I>) ofValue(ReflectionField.ofMojang(tClass, name).get(value));
    }

    public List<String> fields(boolean mojang) {
        List<String> list = new ArrayList<>();
        try {
            for (Field field : tClass.getDeclaredFields()) list.add(mojang ? Reflection.name(field) : field.getName());
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        return list;
    }

    public List<String> methods(boolean mojang) {
        List<String> list = new ArrayList<>();
        try {
            for (Method method : tClass.getDeclaredMethods()) list.add(ReflectionMethod.methodToString(method, mojang));
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        return list;
    }

    public void set(String name, Object value) {
        ReflectionField.of(tClass, name).nonFinal().access().set(this.value, value instanceof ReflectionDynamic<?> dynamic ? dynamic : value);
    }

    public void setMojang(String name, Object value) {
        ReflectionField.ofMojang(tClass, name).nonFinal().access().set(this.value, value instanceof ReflectionDynamic<?> dynamic ? dynamic : value);
    }

    @Override
    public String toString() {
        return toString(true);
    }

    public String toString(boolean mojang) {
        return new StringBuilder("dynamic[")
                .append("class=").append(tClass)
                .append(",value=").append(value)
                .append(",fields=[").append(String.join(",", fields(mojang))).append("]")
                .append(",methods=[").append(String.join(",", methods(mojang))).append("]")
                .append("]")
                .toString();
    }

    public ReflectionMethod getMethod(String name, ReflectionDynamic<?>... args) {
        int length = args.length;
        Class<?>[] classes = new Class[length];
        Object[] values = new Object[length];
        for (int i = 0; i < length; i++) {
            ReflectionDynamic<?> arg = args[i];
            classes[i] = arg.tClass;
            values[i] = arg.value;
        }
        return ReflectionMethod.of(tClass, name, classes);
    }

    public ReflectionMethod getMojangMethod(String name, ReflectionDynamic<?>... args) {
        int length = args.length;
        Class<?>[] classes = new Class[length];
        Object[] values = new Object[length];
        for (int i = 0; i < length; i++) {
            ReflectionDynamic<?> arg = args[i];
            classes[i] = arg.tClass;
            values[i] = arg.value;
        }
        return ReflectionMethod.ofMojang(tClass, name, classes);
    }

    public <I> ReflectionField<I> getField(String name) {
        return ReflectionField.of(tClass, name);
    }

    public <I> ReflectionField<I> getMojangField(String name) {
        return ReflectionField.ofMojang(tClass, name);
    }
}
