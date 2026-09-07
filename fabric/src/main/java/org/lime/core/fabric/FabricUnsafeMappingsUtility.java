package org.lime.core.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.tree.*;
import org.lime.core.common.services.UnsafeMappingsUtility;
import org.objectweb.asm.Type;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class FabricUnsafeMappingsUtility implements UnsafeMappingsUtility {
    private static final String MAPPINGS_RESOURCE = "/META-INF/org-lime-core-fabric/mappings/mappings.tiny";
    private static final String NAMED_NAMESPACE = "named";

    public static final FabricUnsafeMappingsUtility INSTANCE = new FabricUnsafeMappingsUtility();
    public static FabricUnsafeMappingsUtility instance() {
        return INSTANCE;
    }

    private final MemoryMappingTree mappings;
    private final int namedNamespace;
    private final int runtimeNamespace;

    private FabricUnsafeMappingsUtility() {
        mappings = loadMappings();
        namedNamespace = requiredNamespace(NAMED_NAMESPACE);
        runtimeNamespace = requiredNamespace(FabricLoader.getInstance()
                .getMappingResolver()
                .getCurrentRuntimeNamespace());
    }

    private static MemoryMappingTree loadMappings() {
        var stream = FabricUnsafeMappingsUtility.class.getResourceAsStream(MAPPINGS_RESOURCE);
        if (stream == null)
            throw new IllegalStateException("Missing bundled official Mojang mappings: " + MAPPINGS_RESOURCE);

        MemoryMappingTree mappings = new MemoryMappingTree(true);
        try (stream; var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            MappingReader.read(reader, mappings);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read bundled official Mojang mappings: " + MAPPINGS_RESOURCE, e);
        }
        return mappings;
    }

    private int requiredNamespace(String namespace) {
        int id = mappings.getNamespaceId(namespace);
        if (id == MappingTreeView.NULL_NAMESPACE_ID)
            throw new IllegalStateException("Bundled official Mojang mappings do not contain namespace '" + namespace + "'");
        return id;
    }

    private MappingTreeView.MemberMappingView member(
            String owner,
            String name,
            String desc,
            int namespace,
            boolean isMethod) {
        return isMethod
                ? mappings.getMethod(owner, name, desc, namespace)
                : mappings.getField(owner, name, desc, namespace);
    }

    @Override
    public String ofMojang(Class<?> tClass, String name, String desc, boolean isMethod) {
        String runtimeOwner = tClass.getName().replace('.', '/');
        var owner = mappings.getClass(runtimeOwner, runtimeNamespace);
        if (owner == null)
            return name;

        String namedOwner = owner.getName(namedNamespace);
        if (namedOwner == null)
            return name;

        String namedDesc = mappings.mapDesc(desc, runtimeNamespace, namedNamespace);
        var member = member(namedOwner, name, namedDesc, namedNamespace, isMethod);
        if (member == null)
            return name;

        String mappedName = member.getName(runtimeNamespace);
        return mappedName == null ? name : mappedName;
    }
    @Override
    public String ofMojang(Class<?> tClass, String name, Type desc, boolean isMethod) {
        return ofMojang(tClass, name, desc.getDescriptor(), isMethod);
    }

    @Override
    public Optional<String> ofMapped(Class<?> tClass, String name, String desc, boolean isMethod) {
        String owner = tClass.getName().replace('.', '/');
        var member = member(owner, name, desc, runtimeNamespace, isMethod);
        return member == null
                ? Optional.empty()
                : Optional.ofNullable(member.getName(namedNamespace));
    }
    @Override
    public Optional<String> ofMapped(Class<?> tClass, String name, Type desc, boolean isMethod) {
        return ofMapped(tClass, name, desc.getDescriptor(), isMethod);
    }
}
