package org.lime.core.common.reflection;

import com.google.common.collect.ImmutableSet;
import com.google.inject.TypeLiteral;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
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
        LinkedHashSet<Class<?>> classes = loadParentClassesWithInterfaces(currentClass);
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

    private static LinkedHashSet<Class<?>> loadParentClassesWithInterfaces(
            Class<?> currentClass) {
        LinkedHashSet<Class<?>> classes = new LinkedHashSet<>();
        while (currentClass != null) {
            if (!classes.add(currentClass))
                break;
            currentClass = currentClass.getSuperclass();
        }
        Deque<Class<?>> stack = new ArrayDeque<>(classes);
        while (!stack.isEmpty()) {
            Class<?> cls = stack.pop();
            for (Class<?> interfaceClass : cls.getInterfaces()) {
                if (classes.add(interfaceClass))
                    stack.push(interfaceClass);
            }
        }
        return classes;
    }

    public @Unmodifiable Set<Class<?>> classes(boolean includeInterfaces) {
        return includeInterfaces ? superWithInterfaceClasses : superClasses;
    }

    @Override
    public @NotNull String toString() {
        return currentClass.toString();
    }
}
