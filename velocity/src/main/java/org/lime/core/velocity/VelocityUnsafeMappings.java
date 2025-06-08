package org.lime.core.velocity;

import org.lime.core.common.UnsafeMappings;
import org.objectweb.asm.Type;

import java.util.Optional;

public class VelocityUnsafeMappings
        implements UnsafeMappings {
    public static final VelocityUnsafeMappings INSTANCE = new VelocityUnsafeMappings();
    public static VelocityUnsafeMappings instance() {
        return INSTANCE;
    }
    private VelocityUnsafeMappings() {}

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
