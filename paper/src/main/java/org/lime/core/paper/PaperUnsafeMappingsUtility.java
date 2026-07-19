package org.lime.core.paper;

import org.lime.core.common.reflection.Reflection;
import org.lime.core.common.reflection.ReflectionMethod;
import org.lime.core.common.services.UnsafeMappingsUtility;
import org.lime.core.common.utils.execute.Func0;
import org.lime.core.common.utils.execute.Func1;
import org.objectweb.asm.Type;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class PaperUnsafeMappingsUtility implements UnsafeMappingsUtility.Empty {
    public static final PaperUnsafeMappingsUtility INSTANCE = new PaperUnsafeMappingsUtility();
    public static PaperUnsafeMappingsUtility instance() {
        return INSTANCE;
    }

    private record MemberInfo(String name, String desc, boolean isMethod) {}
    @SuppressWarnings("unchecked")
    private static void loadDeobf() throws Throwable {
        Class<?> mappingEnvironmentClass = Reflection
                .findClassOptional("io.papermc.paper.util.MappingEnvironment")
                .orElse(null);
        if (mappingEnvironmentClass == null)
            return;

        Class<?> mappingFileClass = Reflection.findClass("net.neoforged.srgutils.IMappingFile");
        Class<?> classMappingClass = Reflection.findClass("net.neoforged.srgutils.IMappingFile$IClass");
        Class<?> methodMappingClass = Reflection.findClass("net.neoforged.srgutils.IMappingFile$IMethod");
        Class<?> fieldMappingClass = Reflection.findClass("net.neoforged.srgutils.IMappingFile$IField");

        Func0<InputStream> mappingsStreamIfPresent = ReflectionMethod.of(mappingEnvironmentClass, "mappingsStreamIfPresent").lambda(Func0.class);
        Func1<InputStream, Object> loadMappings = ReflectionMethod.of(mappingFileClass, "load", InputStream.class).lambda(Func1.class);
        Func1<Object, Collection<?>> getClasses = ReflectionMethod.of(mappingFileClass, "getClasses").lambda(Func1.class);
        Func1<Object, Collection<?>> getMethods = ReflectionMethod.of(classMappingClass, "getMethods").lambda(Func1.class);
        Func1<Object, Collection<?>> getFields = ReflectionMethod.of(classMappingClass, "getFields").lambda(Func1.class);
        Func1<Object, String> getClassMapped = ReflectionMethod.of(classMappingClass, "getMapped").lambda(Func1.class);
        Func1<Object, String> getMethodOriginal = ReflectionMethod.of(methodMappingClass, "getOriginal").lambda(Func1.class);
        Func1<Object, String> getMethodMapped = ReflectionMethod.of(methodMappingClass, "getMapped").lambda(Func1.class);
        Func1<Object, String> getMethodMappedDescriptor = ReflectionMethod.of(methodMappingClass, "getMappedDescriptor").lambda(Func1.class);
        Func1<Object, String> getFieldOriginal = ReflectionMethod.of(fieldMappingClass, "getOriginal").lambda(Func1.class);
        Func1<Object, String> getFieldMapped = ReflectionMethod.of(fieldMappingClass, "getMapped").lambda(Func1.class);
        Func1<Object, String> getFieldMappedDescriptor = ReflectionMethod.of(fieldMappingClass, "getMappedDescriptor").lambda(Func1.class);

        try (InputStream mappingsInputStream = mappingsStreamIfPresent.invoke()) {
            if (mappingsInputStream == null)
                return;

            var tree = loadMappings.invoke(mappingsInputStream);
            for (var classMapping : getClasses.invoke(tree)) {
                Map<MemberInfo, String> mojang_to_mapped_members = new HashMap<>();
                Map<MemberInfo, String> mapped_to_mojang_members = new HashMap<>();
                //MOJANG+YARN - ORIGINAL
                //SPIGOT - MAPPED
                for (var member : getMethods.invoke(classMapping)) {
                    String original = getMethodOriginal.invoke(member);
                    String mapped = getMethodMapped.invoke(member);
                    String mappedDescriptor = getMethodMappedDescriptor.invoke(member);
                    mojang_to_mapped_members.put(new MemberInfo(original, mappedDescriptor, true), mapped);
                    mapped_to_mojang_members.put(new MemberInfo(mapped, mappedDescriptor, true), original);
                }
                for (var member : getFields.invoke(classMapping)) {
                    String original = getFieldOriginal.invoke(member);
                    String mapped = getFieldMapped.invoke(member);
                    String mappedDescriptor = getFieldMappedDescriptor.invoke(member);
                    mojang_to_mapped_members.put(new MemberInfo(original, mappedDescriptor, false), mapped);
                    mapped_to_mojang_members.put(new MemberInfo(mapped, mappedDescriptor, false), original);
                }
                String class_key = getClassMapped.invoke(classMapping).replace('/', '.');
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
