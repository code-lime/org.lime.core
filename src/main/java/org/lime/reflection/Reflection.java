package org.lime.reflection;

import com.google.common.collect.Streams;
import org.lime.system.execute.Func1;
import org.lime.unsafe;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.stream.Stream;

@SuppressWarnings("removal")
public class Reflection {
    private static final VarHandle MODIFIERS;
    private static final VarHandle SIGNATURE;
    static {
        try {
            MODIFIERS = MethodHandles.privateLookupIn(Field.class, MethodHandles.lookup())
                    .findVarHandle(Field.class, "modifiers", int.class);
            SIGNATURE = MethodHandles.privateLookupIn(Method.class, MethodHandles.lookup())
                    .findVarHandle(Method.class, "signature", String.class);
        } catch (IllegalAccessException | NoSuchFieldException ex) {
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
        //if (Modifier.isFinal(mods)) {
        try {
            /*
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            */
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
        //}
        //if (Modifier.isFinal(mods)) MODIFIERS.set(field, mods & ~Modifier.FINAL);
        //return field;
    }
    public static <T extends AccessibleObject>T access(T val) {
        if (System.getSecurityManager() == null) {
            val.setAccessible(true);
            return val;
        }
        else return AccessController.doPrivileged((PrivilegedAction<T>) () -> {
            val.setAccessible(true);
            return val;
        });
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

    public static void init() {
        /*if (core.instance._existConfig("mappings")) {
            try {
                version = new minecraft_version(system.json.parse(core.instance._readAllConfig("mappings")).getAsJsonObject());
                core.instance._logOP("Loaded mappings:\n - " + version.version + " (" + version.spigot_id + ")");
            } catch (Exception e) {
                core.instance._logStackTrace(e);
            }
        }*/
    }

    public static String name(Field field) {
        return superClasses(field.getDeclaringClass())
                .flatMap(tClass -> unsafe.ofMapped(tClass, field.getName(), Type.getType(field.getType()), false).stream())
                .findFirst()
                .orElseGet(field::getName);
    }
    public static String name(Method method) {
        return superClasses(method.getDeclaringClass())
                .flatMap(tClass -> unsafe.ofMapped(tClass, method.getName(), Type.getType(method), true).stream())
                .findFirst()
                .orElseGet(method::getName);
    }
    private static Stream<Class<?>> superClasses(Class<?> clazz) {
        return clazz == null
                ? Stream.empty()
                : Streams.concat(Stream.of(clazz), superClasses(clazz.getSuperclass()), Arrays.stream(clazz.getInterfaces()).flatMap(v -> superClasses(v)));
    }
}

























