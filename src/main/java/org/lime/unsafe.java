package org.lime;

import io.papermc.paper.util.ObfHelper;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import org.objectweb.asm.Type;
import sun.misc.Unsafe;

import java.io.Closeable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class unsafe {
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

    private record MemberInfo(String name, String desc, boolean isMethod) { }
    private static Closeable loadDeobf() throws Throwable {
        InputStream mappingsInputStream = ObfHelper.class.getClassLoader().getResourceAsStream("META-INF/mappings/reobf.tiny");
        MemoryMappingTree tree = new MemoryMappingTree();
        if (mappingsInputStream == null) return () -> {};
        MappingReader.read(new InputStreamReader(mappingsInputStream, StandardCharsets.UTF_8), MappingFormat.TINY_2, tree);
        for (MappingTree.ClassMapping classMapping : tree.getClasses()) {
            Map<MemberInfo, String> mojang_to_mapped_members = new HashMap<>();
            Map<MemberInfo, String> mapped_to_mojang_members = new HashMap<>();
            for (MappingTree.MemberMapping member : classMapping.getMethods()) {
                mojang_to_mapped_members.put(new MemberInfo(member.getName(ObfHelper.MOJANG_PLUS_YARN_NAMESPACE), member.getDesc(ObfHelper.SPIGOT_NAMESPACE), true), member.getName(ObfHelper.SPIGOT_NAMESPACE));
                mapped_to_mojang_members.put(new MemberInfo(member.getName(ObfHelper.SPIGOT_NAMESPACE), member.getDesc(ObfHelper.SPIGOT_NAMESPACE), true), member.getName(ObfHelper.MOJANG_PLUS_YARN_NAMESPACE));
            }
            for (MappingTree.MemberMapping member : classMapping.getFields()) {
                mojang_to_mapped_members.put(new MemberInfo(member.getName(ObfHelper.MOJANG_PLUS_YARN_NAMESPACE), member.getDesc(ObfHelper.SPIGOT_NAMESPACE), false), member.getName(ObfHelper.SPIGOT_NAMESPACE));
                mapped_to_mojang_members.put(new MemberInfo(member.getName(ObfHelper.SPIGOT_NAMESPACE), member.getDesc(ObfHelper.SPIGOT_NAMESPACE), false), member.getName(ObfHelper.MOJANG_PLUS_YARN_NAMESPACE));
            }
            String class_key = classMapping.getName(ObfHelper.SPIGOT_NAMESPACE).replace('/', '.');
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

    static final Unsafe unsafe;
    static {
        try (Closeable ignored = loadDeobf()) {
            Field sif = Unsafe.class.getDeclaredField("theUnsafe");
            sif.setAccessible(true);
            unsafe = (Unsafe)sif.get(null);
        } catch (Throwable e) {
            throw new IllegalArgumentException(e);
        }
    }
}












