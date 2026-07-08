package org.lime.core.paper.utils.adapters;

import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.NonNull;
import org.lime.core.common.reflection.Reflection;
import org.lime.core.common.reflection.ReflectionMethod;
import org.lime.core.common.utils.execute.Func1;

@SuppressWarnings("unchecked")
public record ResourceLocationIdentifierProxy(Object handle) {
    private static final Class<?> handleClass = Reflection.findClassOptional("net.minecraft.resources.ResourceLocation")
            .orElseGet(() -> Reflection.findClass("net.minecraft.resources.Identifier"));
    private static final Func1<String, Object> parse = ReflectionMethod.ofMojang(handleClass, "parse", String.class).lambda(Func1.class);
    private static final Func1<ResourceKey<?>, Object> identifierLocation = ReflectionMethod.ofMojangOptional(ResourceKey.class, "location")
            .orElseGet(() -> ReflectionMethod.ofMojang(ResourceKey.class, "identifier"))
            .lambda(Func1.class);

    @Override
    public @NonNull String toString() {
        return handle.toString();
    }

    public static ResourceLocationIdentifierProxy identifierLocation(ResourceKey<?> resourceKey) {
        return new ResourceLocationIdentifierProxy(identifierLocation.invoke(resourceKey));
    }

    public static ResourceLocationIdentifierProxy parse(String value) {
        return new ResourceLocationIdentifierProxy(parse.invoke(value));
    }
}
