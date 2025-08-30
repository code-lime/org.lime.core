package org.lime.core.common.reflection;

import com.google.common.collect.Streams;
import net.minecraft.unsafe.Native;
import org.lime.core.common.utils.system.execute.Func1;
import org.lime.core.common.utils.Unsafe;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Stream;

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

    public static <T>Constructor<T> constructor(Class<T> tClass, Class<?>... args) {
        try { return access(tClass.getDeclaredConstructor(args)); }
        catch (Exception e) { throw new IllegalArgumentException(e); }
    }
    public static Method get(Class<?> tClass, String name, Class<?>... args) {
        try { return access(tClass.getDeclaredMethod(name, args)); }
        catch (Exception e) { throw new IllegalArgumentException(e); }
    }
    public static Optional<Method> getFirst(Class<?> tClass, Func1<Method, Boolean> filter, Class<?>... args) {
        try {
            int args_length = args.length;
            for (Method method : tClass.getDeclaredMethods()) {
                if (method.getParameterCount() != args_length) continue;
                try { tClass.getDeclaredMethod(method.getName(), args); } catch (NoSuchMethodException ignored) { continue; }
                if (filter.invoke(method)) return Optional.of(access(method));
            }
            return Optional.empty();
        }
        catch (Exception e) { throw new IllegalArgumentException(e); }
    }
    public static Field get(Class<?> tClass, String name) {
        try { return access(tClass.getDeclaredField(name)); }
        catch (Exception e) { throw new IllegalArgumentException(e); }
    }
    public static Optional<Field> getFirst(Class<?> tClass, Func1<Field, Boolean> filter) {
        try {
            for (Field field : tClass.getDeclaredFields()) {
                if (filter.invoke(field)) return Optional.of(access(field));
            }
            return Optional.empty();
        }
        catch (Exception e) { throw new IllegalArgumentException(e); }
    }
    public static boolean hasField(Class<?> tClass, String field) {
        try { tClass.getDeclaredField(field); return true; }
        catch (NoSuchFieldException e) { return false; }
        catch (Exception e) { throw new IllegalArgumentException(e); }
    }
    @SuppressWarnings("unchecked")
    public static <T>T getField(Class<?> tClass, String field, Object obj) {
        try { return (T)access(tClass.getDeclaredField(field)).get(obj); }
        catch (Exception e) { throw new IllegalArgumentException(e); }
    }
    public static void setField(Class<?> tClass, String field, Object obj, Object value) {
        try { access(tClass.getDeclaredField(field)).set(obj, value); }
        catch (Exception e) { throw new IllegalArgumentException(e); }
    }
    @SuppressWarnings("unchecked")
    public static <T>T invokeMethod(Class<?> tClass, String method, Class<?>[] targs, Object obj, Object[] args) {
        try { return (T)access(tClass.getDeclaredMethod(method, targs)).invoke(obj, args); }
        catch (Exception e) { throw new IllegalArgumentException(e); }
    }
    @SuppressWarnings("unchecked")
    public static <T>T invokeMethod(Class<?> tClass, Class<?> tret, Class<?>[] targs, Object obj, Object[] args) {
        try {
            int length = targs.length;
            for (Method method : tClass.getDeclaredMethods()) {
                if (method.getReturnType() != tret) continue;
                Class<?>[] ttargs = method.getParameterTypes();
                if (ttargs.length != length) continue;
                boolean ok = true;
                for (int i = 0; i < length; i++) {
                    if (ttargs[i] == targs[i]) continue;
                    ok = false;
                    break;
                }
                if (!ok) continue;
                return (T)access(method).invoke(obj, args);
            }
            throw new NoSuchMethodException();
        }
        catch (Exception e) { throw new IllegalArgumentException(e); }
    }

    public static String name(Field field) {
        return superClasses(field.getDeclaringClass())
                .flatMap(tClass -> Unsafe.ofMapped(tClass, field.getName(), Type.getType(field.getType()), false).stream())
                .findFirst()
                .orElseGet(field::getName);
    }
    public static String name(Method method) {
        return superClasses(method.getDeclaringClass())
                .flatMap(tClass -> Unsafe.ofMapped(tClass, method.getName(), Type.getType(method), true).stream())
                .findFirst()
                .orElseGet(method::getName);
    }
    private static Stream<Class<?>> superClasses(Class<?> clazz) {
        return clazz == null
                ? Stream.empty()
                : Streams.concat(Stream.of(clazz), superClasses(clazz.getSuperclass()), Arrays.stream(clazz.getInterfaces()).flatMap(v -> superClasses(v)));
    }
}

























