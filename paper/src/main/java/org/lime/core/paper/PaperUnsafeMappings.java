package org.lime.core.paper;

import io.papermc.paper.util.MappingEnvironment;
import net.neoforged.srgutils.IMappingFile;
import org.lime.core.common.UnsafeMappings;
import org.objectweb.asm.Type;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class PaperUnsafeMappings implements UnsafeMappings {
    public static final PaperUnsafeMappings INSTANCE = new PaperUnsafeMappings();
    public static PaperUnsafeMappings instance() {
        return INSTANCE;
    }

    private record MemberInfo(String name, String desc, boolean isMethod) {}
    private static void loadDeobf() throws Throwable {
        try (InputStream mappingsInputStream = MappingEnvironment.mappingsStreamIfPresent()) {
            if (mappingsInputStream == null)
                return;

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
        }
    }
    private static final Map<String, Map<MemberInfo, String>> mojang_to_mapped = new HashMap<>();
    private static final Map<String, Map<MemberInfo, String>> mapped_to_mojang = new HashMap<>();
    private static final List<Class<?>> dat = new ArrayList<>();

    @Override
    public String ofMojang(Class<?> tClass, String name, String desc, boolean isMethod) {
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
    @Override
    public String ofMojang(Class<?> tClass, String name, Type desc, boolean isMethod) {
        return ofMojang(tClass, name, desc.getDescriptor(), isMethod);
    }

    @Override
    public Optional<String> ofMapped(Class<?> tClass, String name, String desc, boolean isMethod) {
        Map<MemberInfo, String> mapped = mapped_to_mojang.get(tClass.getName());
        if (mapped == null) return Optional.empty();
        return Optional.ofNullable(mapped.get(new MemberInfo(name, desc, isMethod)));
    }
    @Override
    public Optional<String> ofMapped(Class<?> tClass, String name, Type desc, boolean isMethod) {
        return ofMapped(tClass, name, desc.getDescriptor(), isMethod);
    }

    static {
        try {
            loadDeobf();
        } catch (Throwable e) {
            throw new IllegalArgumentException(e);
        }
    }
}
