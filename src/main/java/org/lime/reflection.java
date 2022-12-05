package org.lime;

import com.google.common.collect.Streams;
import com.google.gson.JsonObject;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class reflection {
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
    public static <T extends Field>T nonFinal(T field) {
        int mods = field.getModifiers();
        if (Modifier.isFinal(mods)) MODIFIERS.set(field, mods & ~Modifier.FINAL);
        return field;
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
    public static Optional<Method> getFirst(Class<?> tClass, system.Func1<Method, Boolean> filter, Class<?>... args) {
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
    public static Optional<Field> getFirst(Class<?> tClass, system.Func1<Field, Boolean> filter) {
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
    public static <T>T getField(Class<?> tClass, String field, Object obj) {
        try { return (T)access(tClass.getDeclaredField(field)).get(obj); }
        catch (Exception e) { throw new IllegalArgumentException(e); }
    }
    public static void setField(Class<?> tClass, String field, Object obj, Object value) {
        try { access(tClass.getDeclaredField(field)).set(obj, value); }
        catch (Exception e) { throw new IllegalArgumentException(e); }
    }
    public static <T>T invokeMethod(Class<?> tClass, String method, Class<?>[] targs, Object obj, Object[] args) {
        try { return (T)access(tClass.getDeclaredMethod(method, targs)).invoke(obj, args); }
        catch (Exception e) { throw new IllegalArgumentException(e); }
    }
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

    public static class method implements system.ICallable {
        public final Method method;

        public method(Method method) { this.method = method; }

        public static method of(Method method) { return new method(method); }
        public static method of(Class<?> tClass, String name, Class<?>... args) { return of(reflection.get(tClass, name, args)); }

        private static String methodToString(Method method, boolean mojang) {
            return methodToString(method.getDeclaringClass(), mojang ? name(method) : method.getName(), method.getParameterTypes());
        }
        private static String methodToString(Class<?> tClass, String name, Class<?>[] argTypes) {
            return tClass.getName() + '.' + name + ((argTypes == null || argTypes.length == 0) ? "()" : Arrays.stream(argTypes)
                    .map(c -> c == null ? "null" : c.getName())
                    .collect(Collectors.joining(",", "(", ")")));
        }
        public static method ofMojang(Class<?> tClass, String mojang_name, Class<?>... args) {
            return of(getFirst(tClass, m -> name(m).equals(mojang_name), args)
                    .orElseThrow(() -> new IllegalArgumentException(new NoSuchMethodException(methodToString(tClass, mojang_name, args)))));
        }

        public boolean isStatic() { return Modifier.isStatic(method.getModifiers()); }
        public Object call(Object instance, Object[] args) {
            try { return method.invoke(instance, args); }
            catch (IllegalAccessException | InvocationTargetException e) { throw new IllegalArgumentException(e); }
        }
        @Override public Object call(Object[] args) {
            return isStatic()
                    ? call(null, args)
                    : call(args[0], Arrays.copyOfRange(args, 1, args.length));
        }

        public <T extends system.ICallable>T build(Class<T> tClass) { return build(tClass, "invoke"); }
        public <T extends system.ICallable>T build(Class<T> tClass, String invokable) { return createProxy(tClass, invokable); }
    }
    public static class constructor<T> implements system.ICallable {
        public final Constructor<T> constructor;

        public constructor(Constructor<T> constructor) { this.constructor = constructor; }

        public static <T>constructor<T> of(Constructor<T> method) { return new constructor(method); }
        public static <T>constructor<T> of(Class<T> tClass, Class<?>... args) { return of(reflection.constructor(tClass, args)); }

        public T newInstance(Object... args) {
            try { return constructor.newInstance(args); }
            catch (IllegalAccessException | InvocationTargetException | InstantiationException e) { throw new IllegalArgumentException(e); }
        }
        @Override public Object call(Object[] args) {
            return newInstance(args);
        }

        public <T extends system.ICallable>T build(Class<T> tClass) { return build(tClass, "invoke"); }
        public <T extends system.ICallable>T build(Class<T> tClass, String invokable) { return createProxy(tClass, invokable); }
    }
    public static class field<T> {
        public final Field field;

        public field(Field field) { this.field = field; }
        public static <T>field<T> of(Field field) { return new field<>(field); }
        public static <T>field<T> of(Class<?> tClass, String name) { return of(reflection.get(tClass, name)); }

        public static <T>field<T> ofMojang(Class<?> tClass, String mojang_name) {
            return of(getFirst(tClass, m -> name(m).equals(mojang_name))
                    .orElseThrow(() -> new IllegalArgumentException(new NoSuchFieldException(mojang_name))));
        }

        public field<T> nonFinal() {
            reflection.nonFinal(field);
            return this;
        }
        public void set(Object instance, T value) {
            try { field.set(instance, value); }
            catch (IllegalAccessException e) { throw new IllegalArgumentException(e); }
        }
        public T get(Object instance) {
            try { return (T)field.get(instance); }
            catch (IllegalAccessException e) { throw new IllegalArgumentException(e); }
        }
    }

    public static class dynamic<T> {
        public final T value;
        public final Class<T> tClass;

        public dynamic(T value, Class<T> tClass) {
            this.value = value;
            this.tClass = tClass;
        }

        public static <T>dynamic<T> of(T value, Class<T> tClass) { return new dynamic<>(value, tClass); }
        public static <T>dynamic<T> ofValue(T value) { return of(value, value == null ? null : (Class<T>)value.getClass()); }
        public static <T>dynamic<T> ofStatic(T value, Class<T> tClass) { return of(null, tClass); }

        public <I>dynamic<I> cast(Class<I> tClass) { return of((I)value, tClass); }

        public <I>dynamic<I> invoke(String name, dynamic<?>... args) {
            int length = args.length;
            Class<?>[] classes = new Class[length];
            Object[] values = new Object[length];
            for (int i = 0; i < length; i++) {
                dynamic<?> arg = args[i];
                classes[i] = arg.tClass;
                values[i] = arg.value;
            }
            return (dynamic<I>) ofValue(method.of(tClass, name, classes).call(value, values));
        }
        public <I>dynamic<I> invokeMojang(String name, dynamic<?>... args) {
            int length = args.length;
            Class<?>[] classes = new Class[length];
            Object[] values = new Object[length];
            for (int i = 0; i < length; i++) {
                dynamic<?> arg = args[i];
                classes[i] = arg.tClass;
                values[i] = arg.value;
            }
            return (dynamic<I>) ofValue(method.ofMojang(tClass, name, classes).call(value, values));
        }

        public <I>dynamic<I> get(String name) {
            return (dynamic<I>) ofValue(field.of(tClass, name).get(value));
        }
        public <I>dynamic<I> getMojang(String name) {
            return (dynamic<I>) ofValue(field.ofMojang(tClass, name).get(value));
        }

        public List<String> fields(boolean mojang) {
            List<String> list = new ArrayList<>();
            try { for (Field field : tClass.getDeclaredFields()) list.add(mojang ? name(field) : field.getName()); }
            catch (Exception e) { throw new IllegalArgumentException(e); }
            return list;
        }
        public List<String> methods(boolean mojang) {
            List<String> list = new ArrayList<>();
            try { for (Method method : tClass.getDeclaredMethods()) list.add(reflection.method.methodToString(method, mojang)); }
            catch (Exception e) { throw new IllegalArgumentException(e); }
            return list;
        }

        public void set(String name, Object value) {
            field.of(tClass, name).nonFinal().set(this.value, value instanceof dynamic<?> dynamic ? dynamic : value);
        }
        public void setMojang(String name, Object value) {
            field.ofMojang(tClass, name).nonFinal().set(this.value, value instanceof dynamic<?> dynamic ? dynamic : value);
        }

        @Override public String toString() {
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
    /*private static List<Class<?>> superClasses(Class<?> clazz) {
        List<Class<?>> classes = new ArrayList<>();
        if (clazz == null) return classes;
        classes.add(clazz);
        classes.addAll(superClasses(clazz.getSuperclass()));
        for (Class<?> _clazz : clazz.getInterfaces()) classes.addAll(superClasses(_clazz));
        return classes;
    }
    private static class minecraft_version {
        public static final minecraft_version EMPTY = new minecraft_version();

        public final String version;
        public final String spigot_id;
        public final HashMap<String, class_info> classes = new HashMap<>();

        private static class class_info {
            public final HashMap<String, String> fields = new HashMap<>();
            public final HashMap<String, String> methods = new HashMap<>();
            public class_info(JsonObject json) {
                if (json.has("fields")) json.getAsJsonObject("fields").entrySet().forEach(kv -> fields.put(kv.getKey(), kv.getValue().getAsString()));
                if (json.has("methods")) json.getAsJsonObject("methods").entrySet().forEach(kv -> methods.put(kv.getKey(), kv.getValue().getAsString()));
            }
        }

        public minecraft_version(JsonObject json) {
            version = json.get("version").getAsString();
            spigot_id = json.get("spigot_id").getAsString();
            json.getAsJsonObject("classes").entrySet().forEach(kv -> classes.put(kv.getKey(), new class_info(kv.getValue().getAsJsonObject())));
        }
        public minecraft_version() {
            version = "none";
            spigot_id = "none";
        }
    }
    private static minecraft_version version = minecraft_version.EMPTY;

    public static String name(Field field) {
        minecraft_version.class_info _class = version.classes.getOrDefault(field.getDeclaringClass().getName().replace('.','/'), null);
        String field_name = field.getName();
        return _class == null ? field_name : _class.fields.getOrDefault(field_name, field_name);
    }
    public static String name(Method method) {
        String method_name = method.getName();
        String method_key = method_name + org.lime.reflection.signature(method);
        for (Class<?> clazz : superClasses(method.getDeclaringClass())) {
            minecraft_version.class_info _class = version.classes.getOrDefault(clazz.getName().replace('.','/'), null);
            if (_class == null) continue;
            String method_value = _class.methods.getOrDefault(method_key, null);
            if (method_value == null) continue;
            return method_value;
        }
        return method_name;
    }*/
}

























