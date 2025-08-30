package org.lime.core.common.services;

import org.objectweb.asm.Type;

import java.util.Optional;

public interface UnsafeMappingsUtility {
    String ofMojang(Class<?> tClass, String name, String desc, boolean isMethod);
    String ofMojang(Class<?> tClass, String name, Type desc, boolean isMethod);
    Optional<String> ofMapped(Class<?> tClass, String name, String desc, boolean isMethod);
    Optional<String> ofMapped(Class<?> tClass, String name, Type desc, boolean isMethod);
}
