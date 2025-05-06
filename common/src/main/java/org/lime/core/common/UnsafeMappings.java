package org.lime.core.common;

import org.objectweb.asm.Type;

import java.util.Optional;

public interface UnsafeMappings {
    String ofMojang(Class<?> tClass, String name, String desc, boolean isMethod);
    String ofMojang(Class<?> tClass, String name, Type desc, boolean isMethod);
    Optional<String> ofMapped(Class<?> tClass, String name, String desc, boolean isMethod);
    Optional<String> ofMapped(Class<?> tClass, String name, Type desc, boolean isMethod);
}
