package org.lime.core.common.reflection;

import net.minecraft.unsafe.Native;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.utils.Lazy;
import org.lime.core.common.utils.execute.Func1;
import org.lime.core.common.utils.Unsafe;
import org.lime.core.common.utils.execute.Func2;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Reflection {
    public static String signature(Method method) {
        return Native.signature(method);
    }
    public static Field nonFinal(Field field) {
        return Native.nonFinal(field);
    }
    public static <T extends AccessibleObject>T access(T val) {
        return Native.access(val);
    }

    public static Field[] declaredFields(Class<?> tClass) {
        return Native.declaredFields(tClass);
    }
    public static Field declaredField(Class<?> tClass, String name) {
        return Native.declaredField(tClass, name);
    }

    public static MethodHandles.Lookup allowedModes(MethodHandles.Lookup lookup) {
        return Native.allowedModes(lookup);
    }
    public static MethodHandles.Lookup lookup(Class<?> tClass) {
        return Native.lookup(tClass);
    }

    private static final ConcurrentHashMap<Class<? extends Annotation>, Boolean> annotationRuntimeState = new ConcurrentHashMap<>();
    public static boolean isRuntimeAnnotation(Class<? extends Annotation> annotationClass) {
        return annotationRuntimeState.computeIfAbsent(annotationClass, v -> {
            Retention retention = v.getAnnotation(Retention.class);
            return retention != null && retention.value() == RetentionPolicy.RUNTIME;
        });
    }
    public static void validateRuntimeAnnotation(Class<? extends Annotation> annotationClass) throws IllegalArgumentException {
        if (isRuntimeAnnotation(annotationClass))
            return;
        throw new IllegalArgumentException("Annotation class " + annotationClass.getName() + " is not valid. RetentionPolicy not is RUNTIME");
    }

    public static Class<?> findClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static final Class<?> memberUtils = findClass("org.apache.commons.lang3.reflect.MemberUtils");
    private static final Lazy<Func2<Method, Class<?>[], Boolean>> isMatchingMethod = Lazy.of(() -> ReflectionMethod.of(memberUtils, "isMatchingMethod", Method.class, Class[].class)
            .lambda(Func2.class));
    private static final Lazy<Func2<Constructor<?>, Class<?>[], Boolean>> isMatchingConstructor = Lazy.of(() ->ReflectionMethod.of(memberUtils, "isMatchingConstructor", Constructor.class, Class[].class)
            .lambda(Func2.class));

    public static boolean matchMethod(final Method method, final Class<?>[] parameterTypes) {
        return isMatchingMethod.value().invoke(method, parameterTypes);
    }
    public static boolean matchConstructor(final Constructor<?> constructor, final Class<?>[] parameterTypes) {
        return isMatchingConstructor.value().invoke(constructor, parameterTypes);
    }

    private interface Find<J, T> {
        @Nullable T call(J callbackClass) throws ReflectiveOperationException;
    }

    private static <T>Optional<T> findRecursive(
            Class<?> tClass,
            boolean includeInterfaces,
            Find<Class<?>, T> classCallback) {
        for (var frame : ClassInfo.of(tClass).classes(includeInterfaces)) {
            try {
                var result = classCallback.call(frame);
                if (result != null)
                    return Optional.of(result);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return Optional.empty();
    }
    private static <T>Optional<T> findRecursive(
            Class<?> tClass,
            boolean includeInterfaces,
            Find<Class<?>, Iterable<T>> classCallbackIterables,
            Find<T, T> classCallback) {
        for (var frame : ClassInfo.of(tClass).classes(includeInterfaces)) {
            try {
                var iterables = classCallbackIterables.call(frame);
                if (iterables == null)
                    continue;
                for (var item : iterables) {
                    var result = classCallback.call(item);
                    if (result != null)
                        return Optional.of(result);
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return Optional.empty();
    }

    public static <T>Constructor<T> constructor(Class<T> tClass, Class<?>... args) {
        return constructorOptional(tClass, args)
                .orElseThrow(() -> new IllegalArgumentException("Constructor not found: " + ReflectionMethod.methodToString(tClass, "<init>", args)));
    }
    public static <T>Optional<Constructor<T>> constructorOptional(Class<T> tClass, Class<?>... args) {
        return ClassInfo.of(tClass)
                .constructors()
                .stream()
                .filter(v -> matchConstructor(v, args))
                .map(Optional::of)
                .reduce((a, b) -> Optional.empty())
                .orElse(Optional.empty())
                .map(Reflection::access);
    }

    public static Method get(Class<?> tClass, String name, Class<?>... args) {
        return getOptional(tClass, name, args)
                .orElseThrow(() -> new IllegalArgumentException("Method not found: " + ReflectionMethod.methodToString(tClass, name, args)));
    }
    public static Optional<Method> getOptional(Class<?> tClass, String name, Class<?>... args) {
        return findRecursive(tClass, true, v -> v.getDeclaredMethod(name, args)).map(Reflection::access);
    }
    public static Optional<Method> getFirst(Class<?> tClass, Func1<Method, Boolean> filter, Class<?>... args) {
        return ClassInfo.of(tClass)
                .methods()
                .stream()
                .filter(v -> matchMethod(v, args))
                .filter(filter::invoke)
                .map(Optional::of)
                .reduce((a, b) -> Optional.empty())
                .orElse(Optional.empty())
                .map(Reflection::access);
    }

    public static Field get(Class<?> tClass, String name) {
        return getOptional(tClass, name)
                .orElseThrow(() -> new IllegalArgumentException("Method not found: " + ReflectionField.fieldToString(tClass, name)));
    }
    public static Optional<Field> getOptional(Class<?> tClass, String name) {
        return findRecursive(tClass, false, v -> v.getDeclaredField(name)).map(Reflection::access);
    }
    public static Optional<Field> getFirst(Class<?> tClass, Func1<Field, Boolean> filter) {
        return findRecursive(tClass, false, v -> {
            for (Field field : v.getDeclaredFields()) {
                if (filter.invoke(field))
                    return field;
            }
            return null;
        }).map(Reflection::access);
    }

    public static boolean hasField(Class<?> tClass, String field) {
        return getOptional(tClass, field).isPresent();
    }
    @SuppressWarnings("unchecked")
    public static <T>T getField(Class<?> tClass, String field, @Nullable Object obj) {
        try { return (T)get(tClass, field).get(obj); }
        catch (Exception e) { throw new IllegalArgumentException(e); }
    }
    public static void setField(Class<?> tClass, String field, @Nullable Object obj, Object value) {
        try { get(tClass, field).set(obj, value); }
        catch (Exception e) { throw new IllegalArgumentException(e); }
    }

    @SuppressWarnings("unchecked")
    public static <T>T invokeMethod(Class<?> tClass, String method, Class<?>[] targs, Object obj, Object[] args) {
        try {
            return (T)get(tClass, method, targs).invoke(obj, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
    @SuppressWarnings("unchecked")
    public static <T>T invokeMethod(Class<?> tClass, Class<?> tret, Class<?>[] targs, Object obj, Object[] args) {
        int length = targs.length;
            var resultMethod = findRecursive(tClass, true, v -> {
                for (Method method : v.getDeclaredMethods()) {
                    if (method.getReturnType() != tret) continue;
                    Class<?>[] ttargs = method.getParameterTypes();
                    if (ttargs.length != length) continue;
                    try {
                        return v.getDeclaredMethod(method.getName(), targs);
                    } catch (NoSuchMethodException ignored) {
                    }
                }
                return null;
            })
                    .map(Reflection::access)
                    .orElseThrow(() -> new IllegalArgumentException("Method not found: " + ReflectionMethod.methodToString(tClass, "*", targs)));
        try {
            return (T)resultMethod.invoke(obj, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static String name(Field field) {
        return ClassInfo.of(field.getDeclaringClass())
                .superWithInterfaceClasses()
                .stream()
                .flatMap(tClass -> Unsafe.ofMapped(tClass, field.getName(), Type.getType(field.getType()), false).stream())
                .findFirst()
                .orElseGet(field::getName);
    }
    public static String name(Method method) {
        return ClassInfo.of(method.getDeclaringClass())
                .superWithInterfaceClasses()
                .stream()
                .flatMap(tClass -> Unsafe.ofMapped(tClass, method.getName(), Type.getType(method), true).stream())
                .findFirst()
                .orElseGet(method::getName);
    }

    public static String argsToString(@Nullable Class<?> @Nullable [] argTypes) {
        if (argTypes == null || argTypes.length == 0)
            return "()";
        return Arrays.stream(argTypes)
                .map(c -> c == null ? "null" : c.getName())
                .collect(Collectors.joining(",", "(", ")"));
    }

    @SuppressWarnings("unchecked")
    public static <E extends Throwable> RuntimeException sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }
}

























