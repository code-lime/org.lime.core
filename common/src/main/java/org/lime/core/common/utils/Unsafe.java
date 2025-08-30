package org.lime.core.common.utils;

import org.lime.core.common.services.UnsafeMappingsUtility;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class Unsafe {
    public static UnsafeMappingsUtility MAPPINGS;

    @SuppressWarnings("unchecked")
    public static <T>T createInstance(Class<T> tClass) {
        try {
            return (T)unsafe.allocateInstance(tClass);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static final VarHandle MODIFIERS;
    static {
        try {
            MODIFIERS = MethodHandles.privateLookupIn(Field.class, MethodHandles.lookup())
                    .findVarHandle(Field.class, "modifiers", int.class);
        } catch (IllegalAccessException | NoSuchFieldException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
    public static Field nonFinal(Field field) {
        int mods = field.getModifiers();
        if (Modifier.isFinal(mods)) MODIFIERS.set(field, mods & ~Modifier.FINAL);
        return field;
    }

    public static String ofMojang(Class<?> tClass, String name, String desc, boolean isMethod) {
        return MAPPINGS.ofMojang(tClass, name, desc, isMethod);
    }
    public static String ofMojang(Class<?> tClass, String name, Type desc, boolean isMethod) {
        return MAPPINGS.ofMojang(tClass, name, desc, isMethod);
    }

    public static Optional<String> ofMapped(Class<?> tClass, String name, String desc, boolean isMethod) {
        return MAPPINGS.ofMapped(tClass, name, desc, isMethod);
    }
    public static Optional<String> ofMapped(Class<?> tClass, String name, Type desc, boolean isMethod) {
        return MAPPINGS.ofMapped(tClass, name, desc, isMethod);
    }

    static final sun.misc.Unsafe unsafe;
    public static sun.misc.Unsafe unsafe() {
        return unsafe;
    }
    static {
        try {
            Field sif = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            sif.setAccessible(true);
            unsafe = (sun.misc.Unsafe)sif.get(null);
        } catch (Throwable e) {
            throw new IllegalArgumentException(e);
        }
    }
}












