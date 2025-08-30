package org.lime.core.velocity;

import org.lime.core.common.services.UnsafeMappingsUtility;
import org.objectweb.asm.Type;

import java.util.Optional;

public class VelocityUnsafeMappingsUtility
        implements UnsafeMappingsUtility {
    public static final VelocityUnsafeMappingsUtility INSTANCE = new VelocityUnsafeMappingsUtility();
    public static VelocityUnsafeMappingsUtility instance() {
        return INSTANCE;
    }
    private VelocityUnsafeMappingsUtility() {}

    @Override
    public String ofMojang(Class<?> tClass, String name, String desc, boolean isMethod) {
        return name;
    }
    @Override
    public String ofMojang(Class<?> tClass, String name, Type desc, boolean isMethod) {
        return name;
    }
    @Override
    public Optional<String> ofMapped(Class<?> tClass, String name, String desc, boolean isMethod) {
        return Optional.empty();
    }
    @Override
    public Optional<String> ofMapped(Class<?> tClass, String name, Type desc, boolean isMethod) {
        return Optional.empty();
    }
}
