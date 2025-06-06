package net.minecraft.unsafe;

import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.*;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

public class Native {
    private static final VarHandle MODIFIERS;
    private static final VarHandle SIGNATURE;
    private static final MethodHandle ACCESSIBLE;
    static {
        try {
            MODIFIERS = MethodHandles.privateLookupIn(Field.class, MethodHandles.lookup())
                    .findVarHandle(Field.class, "modifiers", int.class);
            SIGNATURE = MethodHandles.privateLookupIn(Method.class, MethodHandles.lookup())
                    .findVarHandle(Method.class, "signature", String.class);
            ACCESSIBLE = MethodHandles.privateLookupIn(AccessibleObject.class, MethodHandles.lookup())
                    .findVirtual(AccessibleObject.class, "setAccessible0", MethodType.methodType(boolean.class, boolean.class));
        } catch (IllegalAccessException | NoSuchFieldException | NoSuchMethodException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
    public static String signature(Method method){
        String sig = (String)SIGNATURE.get(method);
        if(sig != null) return sig;

        StringBuilder sb = new StringBuilder("(");
        for(Class<?> c : method.getParameterTypes())
            sb.append((sig = Array.newInstance(c, 0).toString()), 1, sig.indexOf('@'));
        return sb.append(')')
                .append(method.getReturnType() == void.class
                        ? "V"
                        : (sig = Array.newInstance(method.getReturnType(), 0).toString()).substring(1, sig.indexOf('@'))
                )
                .toString()
                .replace('.', '/');
    }
    public static Field nonFinal(Field field) {
        int mods = field.getModifiers();
        try {
            Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
            getDeclaredFields0.setAccessible(true);
            Field[] fields = (Field[]) getDeclaredFields0.invoke(Field.class, false);
            Field modifiers = null;
            for (Field each : fields) {
                if ("modifiers".equals(each.getName())) {
                    modifiers = each;
                    break;
                }
            }
            assert modifiers != null;
            modifiers.setAccessible(true);
            mods &= ~Modifier.FINAL;
            mods |= Modifier.PUBLIC;
            modifiers.setInt(field, mods);
            return field;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
    public static <T extends AccessibleObject>T access(T val) {
        try {
            ACCESSIBLE.invoke(val, true);
            return val;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private interface ThrowableExecute<T> {
        T invoke() throws Throwable;

        static <T>T of(ThrowableExecute<T> lambda) {
            try {
                return lambda.invoke();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final Method getDeclaredFields0Method = ThrowableExecute.of(() ->
            access(Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class)));
    public static Field[] declaredFields(Class<?> tClass) {
        try {
            return (Field[]) getDeclaredFields0Method.invoke(tClass, false);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    public static @Nullable Field declaredField(Class<?> tClass, String name) {
        Field[] fields = declaredFields(tClass);
        for (Field field : fields)
            if (name.equals(field.getName()))
                return field;
        return null;
    }

    private static final Field allowedModesField = Objects.requireNonNull(access(declaredField(MethodHandles.Lookup.class, "allowedModes")));
    public static MethodHandles.Lookup allowedModes(MethodHandles.Lookup lookup) {
        int mods = lookup.lookupModes();
        mods |= MethodHandles.Lookup.PRIVATE;
        mods |= MethodHandles.Lookup.MODULE;
        try {
            allowedModesField.setInt(lookup, mods);
            return lookup;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final Function<Class<?>, MethodHandles.Lookup> lookupOfMethod;
    static {
        Function<Class<?>, MethodHandles.Lookup> lookup;
        try {
            Method method = access(MethodHandles.class.getDeclaredMethod("lookup", Class.class));
            lookup = tClass -> {
                try {
                    return (MethodHandles.Lookup) method.invoke(null, tClass);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        } catch (NoSuchMethodException e) {
            try {
                Constructor<MethodHandles.Lookup> constructor = access(MethodHandles.Lookup.class.getDeclaredConstructor(Class.class));
                lookup = tClass -> {
                    try {
                        return (MethodHandles.Lookup) constructor.newInstance(tClass);
                    } catch (Exception _e) {
                        throw new RuntimeException(_e);
                    }
                };
            } catch (Exception _e) {
                throw new RuntimeException(_e);
            }
        }
        lookupOfMethod = lookup;
    }

    public static MethodHandles.Lookup lookup(Class<?> tClass) {
        return lookupOfMethod.apply(tClass);
    }
}
