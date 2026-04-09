package org.lime.core.common.reflection;

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public record ClassInfo<T>(
        Class<T> currentClass,
        @Unmodifiable Set<Constructor<T>> constructors,
        @Unmodifiable Set<Field> fields,
        @Unmodifiable Set<Method> methods,
        @Unmodifiable Set<Class<?>> superClasses,
        @Unmodifiable Set<Class<?>> interfaceClasses,
        @Unmodifiable Set<Class<?>> superWithInterfaceClasses) {
    private static final ConcurrentHashMap<Class<?>, ClassInfo<?>> cache = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T>ClassInfo<T> of(Class<T> tClass) {
        return (ClassInfo<T>)cache.computeIfAbsent(tClass, ClassInfo::load);
    }

    private static <T>ClassInfo<T> load(Class<T> currentClass) {
        HashSet<Class<?>> classes = new HashSet<>();
        loadRecursive(currentClass, classes);
        var superClassesBuilder = ImmutableSet.<Class<?>>builder();
        var interfaceClassesBuilder = ImmutableSet.<Class<?>>builder();
        var constructorsBuilder = ImmutableSet.<Constructor<T>>builder();
        for (var ctor : currentClass.getDeclaredConstructors()) {
            //noinspection unchecked
            constructorsBuilder.add((Constructor<T>) ctor);
        }
        var fieldsBuilder = ImmutableSet.<Field>builder();
        var methodsBuilder = ImmutableSet.<Method>builder();
        classes.forEach(testClass -> {
            (testClass.isInterface() ? interfaceClassesBuilder : superClassesBuilder).add(testClass);
            fieldsBuilder.add(testClass.getDeclaredFields());
            methodsBuilder.add(testClass.getDeclaredMethods());
        });
        var superWithInterfaceClasses = ImmutableSet.copyOf(classes);
        return new ClassInfo<>(
                currentClass,
                constructorsBuilder.build(),
                fieldsBuilder.build(),
                methodsBuilder.build(),
                superClassesBuilder.build(),
                interfaceClassesBuilder.build(),
                superWithInterfaceClasses);
    }

    private static void loadRecursive(
            Class<?> currentClass,
            Set<Class<?>> classes) {
        while (currentClass != null) {
            if (!classes.add(currentClass))
                break;

            for (final Class<?> interfaceClass : currentClass.getInterfaces())
                if (classes.add(interfaceClass))
                    loadRecursive(interfaceClass, classes);

            currentClass = currentClass.getSuperclass();
        }
    }

    public @Unmodifiable Set<Class<?>> classes(boolean includeInterfaces) {
        return includeInterfaces ? superWithInterfaceClasses : superClasses;
    }

    @Override
    public @NotNull String toString() {
        return currentClass.toString();
    }
}
