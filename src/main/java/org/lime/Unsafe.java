package org.lime;

import io.papermc.paper.util.MappingEnvironment;
import net.neoforged.srgutils.IMappingFile;
import org.objectweb.asm.Type;

import java.io.Closeable;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

public class Unsafe {
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

    private record MemberInfo(String name, String desc, boolean isMethod) {}
    private static Closeable loadDeobf() throws Throwable {
        InputStream mappingsInputStream = MappingEnvironment.mappingsStreamIfPresent();
        if (mappingsInputStream == null) return () -> {};
        IMappingFile tree = IMappingFile.load(mappingsInputStream);
        for (IMappingFile.IClass classMapping : tree.getClasses()) {
            Map<MemberInfo, String> mojang_to_mapped_members = new HashMap<>();
            Map<MemberInfo, String> mapped_to_mojang_members = new HashMap<>();
            /*MOJANG+YARN - ORIGINAL*/
            /*SPIGOT - MAPPED*/
            for (IMappingFile.IMethod member : classMapping.getMethods()) {
                mojang_to_mapped_members.put(new MemberInfo(member.getOriginal(), member.getMappedDescriptor(), true), member.getMapped());
                mapped_to_mojang_members.put(new MemberInfo(member.getMapped(), member.getMappedDescriptor(), true), member.getOriginal());
            }
            for (IMappingFile.IField member : classMapping.getFields()) {
                mojang_to_mapped_members.put(new MemberInfo(member.getOriginal(), member.getMappedDescriptor(), false), member.getMapped());
                mapped_to_mojang_members.put(new MemberInfo(member.getMapped(), member.getMappedDescriptor(), false), member.getOriginal());
            }
            String class_key = classMapping.getMapped().replace('/', '.');
            mojang_to_mapped.put(class_key, mojang_to_mapped_members);
            mapped_to_mojang.put(class_key, mapped_to_mojang_members);
        }
        return mappingsInputStream;
    }
    private static final Map<String, Map<MemberInfo, String>> mojang_to_mapped = new HashMap<>();
    private static final Map<String, Map<MemberInfo, String>> mapped_to_mojang = new HashMap<>();
    private static final List<Class<?>> dat = new ArrayList<>();

    public static String ofMojang(Class<?> tClass, String name, String desc, boolean isMethod) {
        Map<MemberInfo, String> mapping = mojang_to_mapped.get(tClass.getName());
        if (mapping == null) return name;
        if (dat.contains(tClass)) {
            System.out.println("Class " + tClass.getName() + " with found " + (isMethod ? "method" : "field") + " " + name + desc + "\n" + mapping.entrySet().stream().map(v -> v.getKey().toString() + ": " + v.getValue()).collect(Collectors.joining(",")));
            dat.add(tClass);
        }
        String src_name = mapping.get(new MemberInfo(name, desc, isMethod));
        if (src_name == null) src_name = name;
        return src_name;
    }
    public static String ofMojang(Class<?> tClass, String name, Type desc, boolean isMethod) {
        return ofMojang(tClass, name, desc.getDescriptor(), isMethod);
    }

    public static Optional<String> ofMapped(Class<?> tClass, String name, String desc, boolean isMethod) {
        Map<MemberInfo, String> mapped = mapped_to_mojang.get(tClass.getName());
        if (mapped == null) return Optional.empty();
        return Optional.ofNullable(mapped.get(new MemberInfo(name, desc, isMethod)));
    }
    public static Optional<String> ofMapped(Class<?> tClass, String name, Type desc, boolean isMethod) {
        return ofMapped(tClass, name, desc.getDescriptor(), isMethod);
    }

    static final sun.misc.Unsafe unsafe;
    static {
        try (Closeable ignored = loadDeobf()) {
            Field sif = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            sif.setAccessible(true);
            unsafe = (sun.misc.Unsafe)sif.get(null);
        } catch (Throwable e) {
            throw new IllegalArgumentException(e);
        }
    }
}












