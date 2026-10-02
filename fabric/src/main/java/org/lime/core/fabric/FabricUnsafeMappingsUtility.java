package org.lime.core.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.tree.*;
import org.lime.core.common.services.UnsafeMappingsUtility;
import org.objectweb.asm.Type;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class FabricUnsafeMappingsUtility
        implements UnsafeMappingsUtility {
    private static final String MAPPINGS_RESOURCE = "/META-INF/org-lime-core-fabric/mappings/mappings.tiny";
    private static final String NAMED_NAMESPACE = "named";
    private static final String INTERMEDIARY_NAMESPACE = "intermediary";

    public static final FabricUnsafeMappingsUtility INSTANCE = new FabricUnsafeMappingsUtility();
    public static FabricUnsafeMappingsUtility instance() {
        return INSTANCE;
    }

    private final MappingResolver resolver;
    private final MemoryMappingTree mappings;
    private final int namedNamespace;
    private final int intermediaryNamespace;

    private FabricUnsafeMappingsUtility() {
        resolver = FabricLoader.getInstance().getMappingResolver();
        var stream = FabricUnsafeMappingsUtility.class.getResourceAsStream(MAPPINGS_RESOURCE);
        if (stream == null)
            throw new IllegalStateException("Missing bundled official Mojang mappings: " + MAPPINGS_RESOURCE);

        mappings = new MemoryMappingTree(true);
        try (stream; var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            MappingReader.read(reader, mappings);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read bundled official Mojang mappings: " + MAPPINGS_RESOURCE, e);
        }
        namedNamespace = mappings.getNamespaceId(NAMED_NAMESPACE);
        intermediaryNamespace = mappings.getNamespaceId(INTERMEDIARY_NAMESPACE);
        if (namedNamespace == MappingTreeView.NULL_NAMESPACE_ID || intermediaryNamespace == MappingTreeView.NULL_NAMESPACE_ID)
            throw new IllegalStateException("Bundled official mappings do not contain namespace '" + NAMED_NAMESPACE + "' or '" + INTERMEDIARY_NAMESPACE + "'");
        if (!resolver.getNamespaces().contains(INTERMEDIARY_NAMESPACE))
            throw new IllegalStateException("Fabric MappingResolver does not contain required namespace '" + INTERMEDIARY_NAMESPACE + "' for runtime namespace '" + resolver.getCurrentRuntimeNamespace() + "'");
    }

    private String runtimeToIntermediaryClass(String runtimeName) {
        return resolver.unmapClassName(INTERMEDIARY_NAMESPACE, runtimeName.replace('/', '.'))
                .replace('.', '/');
    }

    private Type runtimeToIntermediaryType(Type type) {
        return switch (type.getSort()) {
            case Type.OBJECT -> Type.getObjectType(runtimeToIntermediaryClass(type.getInternalName()));
            case Type.ARRAY -> Type.getType("[".repeat(type.getDimensions())
                    + runtimeToIntermediaryType(type.getElementType()).getDescriptor());
            case Type.METHOD -> Type.getMethodType(
                    runtimeToIntermediaryType(type.getReturnType()),
                    Arrays.stream(type.getArgumentTypes())
                            .map(this::runtimeToIntermediaryType)
                            .toArray(Type[]::new));
            default -> type;
        };
    }

    private String intermediaryToRuntimeMember(String owner, String name, String desc, boolean isMethod) {
        String binaryOwner = owner.replace('/', '.');
        return isMethod
                ? resolver.mapMethodName(INTERMEDIARY_NAMESPACE, binaryOwner, name, desc)
                : resolver.mapFieldName(INTERMEDIARY_NAMESPACE, binaryOwner, name, desc);
    }

    @Override
    public String ofMojang(Class<?> tClass, String name, String desc, boolean isMethod) {
        return ofMojang(tClass, name, Type.getType(desc), isMethod);
    }
    @Override
    public String ofMojang(Class<?> tClass, String name, Type desc, boolean isMethod) {
        String intermediaryOwner = runtimeToIntermediaryClass(tClass.getName());
        var owner = mappings.getClass(intermediaryOwner, intermediaryNamespace);
        if (owner == null || owner.getName(namedNamespace) == null)
            return name;

        String intermediaryDesc = runtimeToIntermediaryType(desc).getDescriptor();
        String namedDesc = mappings.mapDesc(intermediaryDesc, intermediaryNamespace, namedNamespace);
        var member = isMethod ? owner.getMethod(name, namedDesc, namedNamespace) : owner.getField(name, namedDesc, namedNamespace);
        if (member == null)
            return name;

        String intermediaryName = member.getName(intermediaryNamespace);
        String memberDesc = member.getDesc(intermediaryNamespace);
        return intermediaryName == null || memberDesc == null
                ? name
                : intermediaryToRuntimeMember(intermediaryOwner, intermediaryName, memberDesc, isMethod);
    }
    @Override
    public Optional<String> ofMapped(Class<?> tClass, String name, String desc, boolean isMethod) {
        return ofMapped(tClass, name, Type.getType(desc), isMethod);
    }
    @Override
    public Optional<String> ofMapped(Class<?> tClass, String name, Type desc, boolean isMethod) {
        String intermediaryOwner = runtimeToIntermediaryClass(tClass.getName());
        var owner = mappings.getClass(intermediaryOwner, intermediaryNamespace);
        if (owner == null)
            return Optional.empty();

        String intermediaryDesc = runtimeToIntermediaryType(desc).getDescriptor();
        Collection<? extends MappingTreeView.MemberMappingView> members = isMethod ? owner.getMethods() : owner.getFields();
        for (var member : members) {
            String memberDesc = member.getDesc(intermediaryNamespace);
            if (!Objects.equals(memberDesc, intermediaryDesc))
                continue;

            String intermediaryName = member.getName(intermediaryNamespace);
            if (intermediaryName == null || !intermediaryToRuntimeMember(intermediaryOwner, intermediaryName, memberDesc, isMethod).equals(name))
                continue;

            return Optional.ofNullable(member.getName(namedNamespace));
        }
        return Optional.empty();
    }
}
