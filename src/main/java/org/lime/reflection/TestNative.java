package org.lime.reflection;

import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.server.MinecraftServer;
import org.lime.system.execute.Execute;

import javax.annotation.Nullable;
import java.lang.invoke.*;
import java.lang.reflect.*;
import java.security.ProtectionDomain;
import java.util.Set;

@SuppressWarnings("removal")
public class TestNative {
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
    public static MethodHandles.Lookup allowedModes(MethodHandles.Lookup lookup) {
        int mods = lookup.lookupModes();
        try {
            Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
            getDeclaredFields0.setAccessible(true);
            Field[] fields = (Field[]) getDeclaredFields0.invoke(MethodHandles.Lookup.class, false);
            Field modifiers = null;
            System.out.println("Fields count: " + fields.length);
            for (Field each : fields) {
                System.out.println(" - " + each);
                if ("allowedModes".equals(each.getName())) {
                    modifiers = each;
                    break;
                }
            }
            assert modifiers != null;
            modifiers = access(modifiers);// .setAccessible(true);
            mods |= MethodHandles.Lookup.PUBLIC
                    | MethodHandles.Lookup.PRIVATE
                    | MethodHandles.Lookup.PROTECTED
                    | MethodHandles.Lookup.PACKAGE
                    | MethodHandles.Lookup.MODULE
                    | MethodHandles.Lookup.ORIGINAL;
            //mods |= MethodHandles.Lookup.PROTECTED;
            //mods |= MethodHandles.Lookup.MODULE;
            modifiers.setInt(lookup, mods);
            return lookup;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
    public static <T extends Class<?>>T replaceLoader(T tClass, ClassLoader loader) {
        try {
            Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
            getDeclaredFields0.setAccessible(true);
            Field[] fields = (Field[]) getDeclaredFields0.invoke(Class.class, false);
            Field classLoader = null;
            System.out.println("Fields count: " + fields.length);
            for (Field each : fields) {
                System.out.println(" - " + each);
                if ("classLoader".equals(each.getName())) {
                    classLoader = each;
                    break;
                }
            }
            assert classLoader != null;
            access(classLoader).set(tClass, loader);
            return tClass;
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
        /*
        if (System.getSecurityManager() == null) {
            val.setAccessible(true);
            return val;
        }
        else return AccessController.doPrivileged((PrivilegedAction<T>) () -> {
            val.setAccessible(true);
            return val;
        });
        */
    }

    public static MethodHandles.Lookup lookup(Class<?> dClass) {
        try {
            /*var methods = MethodHandles.class.getDeclaredMethods();
            System.out.println("Methods count: " + methods.length);
            Method lookup = null;
            for (var each : methods) {
                System.out.println(" - " + each);
                if (each.getName().equals("lookup") && each.getParameterCount() == 1 && each.getParameterTypes()[0].equals(Class.class)) {
                    lookup = each;
                    break;
                }
            }
            assert lookup != null;*/
            Method lookup = MethodHandles.class.getDeclaredMethod("lookup", Class.class);
            return (MethodHandles.Lookup)access(lookup).invoke(null, dClass);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    public static MethodHandles.Lookup defineHiddenClass(
            MethodHandles.Lookup target,
            ClassLoader loader,
            byte[] bytes,
            boolean initialize,
            MethodHandles.Lookup.ClassOption... options) {
/*
ClassDefiner makeHiddenClassDefiner(byte[] bytes, Set<ClassOption> options, boolean accessVmAnnotations)
*/
        try {
            /*var methods = MethodHandles.Lookup.class.getDeclaredMethods();
            System.out.println("Methods count: " + methods.length);
            Method makeHiddenClassDefiner = null;
            for (var each : methods) {
                System.out.println(" - " + each);
                if (each.getName().equals("makeHiddenClassDefiner")
                        && each.getParameterCount() == 3
                        && Arrays.equals(each.getParameterTypes(), new Class<?>[] { byte[].class, Set.class, Boolean.TYPE })) {
                    makeHiddenClassDefiner = each;
                    break;
                }
            }
            assert makeHiddenClassDefiner != null;*/
            var definer = access(MethodHandles.Lookup.class.getDeclaredMethod("makeHiddenClassDefiner",
                    byte[].class, Set.class, boolean.class))
                    .invoke(target, bytes, Set.of(options), false);
            MethodHandles.Lookup lookup = (MethodHandles.Lookup)access(definer.getClass().getDeclaredField("lookup"))
                    .get(definer);
            int classFlags = (int)access(definer.getClass().getDeclaredField("classFlags"))
                    .get(definer);
            String name = (String)access(definer.getClass().getDeclaredField("name"))
                    .get(definer);
            int NESTMATE_CLASS = (int)access(MethodHandles.Lookup.ClassOption.class.getDeclaredField("flag"))
                    .get(MethodHandles.Lookup.ClassOption.NESTMATE);
            Object classData = null;
            Class<?> lookupClass = lookup.lookupClass();
            ProtectionDomain pd = loader != null
                    ? (ProtectionDomain)access(MethodHandles.Lookup.class.getDeclaredMethod("lookupClassProtectionDomain")).invoke(lookup)
                    : null;

            System.out.println("Lookup class: " + lookupClass);

            /*
            ClassLoader oldLoader = lookupClass.getClassLoader();
            Class<?> resultClass;
            try {
                replaceLoader(lookupClass, loader);
                System.out.println("Loader 1: " + loader);
                System.out.println("Loader 2: " + lookupClass.getClassLoader());
                resultClass = defineClass(loader, lookupClass, name, bytes, pd, initialize, classFlags, classData);
            } finally {
                replaceLoader(lookupClass, oldLoader);
            }
            */
            Class<?> resultClass = defineClass(loader, name, bytes, null, "__HiddenClass__");
            assert (classFlags & NESTMATE_CLASS) == 0 || resultClass.getNestHost() == lookupClass.getNestHost();
            return lookup(resultClass);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static final Class<?> sharedSecretsClass = Execute.funcEx(() ->
            Class.forName("jdk.internal.access.SharedSecrets")).throwable().invoke();
    private static final ReflectionDynamic<?> javaLangAccess = ReflectionDynamic.ofStatic(sharedSecretsClass)
            .invoke("getJavaLangAccess");

    public static Class<?> defineClass(ClassLoader loader, String name, byte[] bytes, @Nullable ProtectionDomain domain, String source) {
        return javaLangAccess
                .invoke("defineClass",
                        ReflectionDynamic.of(loader, ClassLoader.class),
                        ReflectionDynamic.of(name, String.class),
                        ReflectionDynamic.of(bytes, byte[].class),
                        ReflectionDynamic.of(domain, ProtectionDomain.class),
                        ReflectionDynamic.of(source, String.class))
                .cast(Class.class)
                .value;
    }
    public static Class<?> defineClass(ClassLoader loader, Class<?> lookupClass, String name, byte[] bytes, @Nullable ProtectionDomain domain, boolean initialize, int flags, @Nullable Object classData) {
        return javaLangAccess
                .invoke("defineClass",
                        ReflectionDynamic.of(loader, ClassLoader.class),
                        ReflectionDynamic.of(lookupClass, Class.class),
                        ReflectionDynamic.of(name, String.class),
                        ReflectionDynamic.of(bytes, byte[].class),
                        ReflectionDynamic.of(domain, ProtectionDomain.class),
                        ReflectionDynamic.of(initialize, Boolean.TYPE),
                        ReflectionDynamic.of(flags, Integer.TYPE),
                        ReflectionDynamic.of(classData, Object.class))
                .cast(Class.class)
                .value;
    }
    public static void addOpensToAllUnnamed(Class<?> tClass) {
        addOpensToAllUnnamed(tClass.getModule(), tClass.getPackageName());
    }
    public static void addOpensToAllUnnamed(Module module, String pkg) {
        javaLangAccess
                .invoke("addOpensToAllUnnamed",
                        ReflectionDynamic.of(module, Module.class),
                        ReflectionDynamic.of(pkg, String.class));
    }
}
