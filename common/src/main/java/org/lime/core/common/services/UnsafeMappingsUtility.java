package org.lime.core.common.services;

import org.objectweb.asm.Type;

import java.util.Optional;

public interface UnsafeMappingsUtility {
    String ofMojang(Class<?> tClass, String name, String desc, boolean isMethod);
    String ofMojang(Class<?> tClass, String name, Type desc, boolean isMethod);
    Optional<String> ofMapped(Class<?> tClass, String name, String desc, boolean isMethod);
    Optional<String> ofMapped(Class<?> tClass, String name, Type desc, boolean isMethod);

    Empty EMPTY = new Empty() {};

    interface Empty
            extends UnsafeMappingsUtility {
        @Override
        default String ofMojang(Class<?> tClass, String name, String desc, boolean isMethod) {
            return name;
        }
        @Override
        default String ofMojang(Class<?> tClass, String name, Type desc, boolean isMethod) {
            return name;
        }
        @Override
        default Optional<String> ofMapped(Class<?> tClass, String name, String desc, boolean isMethod) {
            return Optional.empty();
        }
        @Override
        default Optional<String> ofMapped(Class<?> tClass, String name, Type desc, boolean isMethod) {
            return Optional.empty();
        }
    }
}
