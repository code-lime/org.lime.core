package org.lime.core.fabric.utils;

import com.google.gson.reflect.TypeToken;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;
import org.lime.core.common.reflection.ReflectionField;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Stream;

public final class RegistryUtils {
    public record NmsRegistry<T>(@NotNull TypeToken<T> type, @NotNull Registry<T> registry) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        private static @NotNull Optional<NmsRegistry<?>> create(@NotNull RegistryAccess registryAccess, @NotNull ReflectionField<?> field) {
            if (!(field.target().getGenericType() instanceof ParameterizedType resourceKeyType)
                    || !ResourceKey.class.equals(resourceKeyType.getRawType())
                    || !(resourceKeyType.getActualTypeArguments()[0] instanceof ParameterizedType registryType)
                    || !Registry.class.equals(registryType.getRawType()))
                return Optional.empty();
            var elementType = registryType.getActualTypeArguments()[0];
            if (elementType.getTypeName().startsWith("java."))
                return Optional.empty();
            ResourceKey<? extends Registry<?>> registryKey = (ResourceKey<? extends Registry<?>>)field.get(null);
            var type = TypeToken.get(elementType);
            //#switch PROPERTIES.versionMinecraft
            //#caseofregex 1\.21\.[4-8]
            //OF//            return registryAccess.lookup((ResourceKey)registryKey)
            //#default
            return registryAccess.registry((ResourceKey)registryKey)
            //#endswitch
                    .map(registry -> new NmsRegistry(type, (Registry)registry));
        }
    }

    public static @NotNull Collection<NmsRegistry<?>> nmsRegistries(@NotNull RegistryAccess registryAccess) {
        Map<TypeToken<?>, List<NmsRegistry<?>>> collected = new HashMap<>();
        Stream.of(Registries.class.getDeclaredFields())
                .map(ReflectionField::of)
                .filter(v -> v.is(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL))
                .filter(v -> ResourceKey.class.isAssignableFrom(v.target().getType()))
                .map(v -> NmsRegistry.create(registryAccess, v))
                .flatMap(Optional::stream)
                .forEach(v -> collected.computeIfAbsent(v.type(), ignored -> new ArrayList<>()).add(v));
        return collected.values().stream()
                .filter(v -> v.size() == 1)
                .<NmsRegistry<?>>map(v -> v.get(0))
                .toList();
    }
}
