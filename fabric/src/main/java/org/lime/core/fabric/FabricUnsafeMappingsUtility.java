package org.lime.core.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import org.lime.core.common.services.UnsafeMappingsUtility;
import org.objectweb.asm.Type;

import java.util.Optional;

public class FabricUnsafeMappingsUtility implements UnsafeMappingsUtility {
    public static final FabricUnsafeMappingsUtility INSTANCE = new FabricUnsafeMappingsUtility();
    public static FabricUnsafeMappingsUtility instance() {
        return INSTANCE;
    }

    private final MappingResolver resolver;

    private FabricUnsafeMappingsUtility() {
        this.resolver = FabricLoader.getInstance().getMappingResolver();
    }

    @Override
    public String ofMojang(Class<?> tClass, String name, String desc, boolean isMethod) {
        String owner = tClass.getName().replace('.', '/');
        try {
            return isMethod
                    ? resolver.mapMethodName("intermediary", owner, name, desc)
                    : resolver.mapFieldName("intermediary", owner, name, desc);
        } catch (Exception e) {
            return name;
        }
    }
    @Override
    public String ofMojang(Class<?> tClass, String name, Type desc, boolean isMethod) {
        return ofMojang(tClass, name, desc.getDescriptor(), isMethod);
    }

    @Override
    public Optional<String> ofMapped(Class<?> tClass, String name, String desc, boolean isMethod) {
        String owner = tClass.getName().replace('.', '/');
        try {
            String mapped = isMethod
                    ? resolver.mapMethodName("named", owner, name, desc)
                    : resolver.mapFieldName("named", owner, name, desc);
            return Optional.ofNullable(mapped);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    @Override
    public Optional<String> ofMapped(Class<?> tClass, String name, Type desc, boolean isMethod) {
        return ofMapped(tClass, name, desc.getDescriptor(), isMethod);
    }
}
