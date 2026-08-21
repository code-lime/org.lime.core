package org.lime.core.paper.utils;

import com.google.common.base.CaseFormat;
import com.google.gson.reflect.TypeToken;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.bukkit.Keyed;
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
            return registryAccess.lookup((ResourceKey)registryKey)
                    .map(registry -> new NmsRegistry(type, (Registry)registry));
        }
    }
    public record PaperRegistry<T extends Keyed>(@NotNull TypeToken<T> type, @NotNull org.bukkit.Registry<T> registry, @NotNull Key registryKey) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        private static @NotNull Optional<PaperRegistry<?>> create(@NotNull io.papermc.paper.registry.RegistryAccess registryAccess, @NotNull ReflectionField<?> field) {
            if (!(field.target().getGenericType() instanceof ParameterizedType registryKeyType)
                    || !RegistryKey.class.equals(registryKeyType.getRawType()))
                return Optional.empty();
            var elementType = registryKeyType.getActualTypeArguments()[0];
            var elementClass = TypeUtils.getRawType(elementType, null);
            if (elementClass == null || !Keyed.class.isAssignableFrom(elementClass))
                return Optional.empty();
            RegistryKey<? extends Keyed> registryKey = (RegistryKey<? extends Keyed>)field.get(null);
            return Optional.of(new PaperRegistry(TypeToken.get(elementType), registryAccess.getRegistry(registryKey), registryKey.key()));
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static @NotNull Optional<PaperRegistry<?>> createLegacy(@NotNull ReflectionField<?> field) {
            if (!(field.target().getGenericType() instanceof ParameterizedType registryType)
                    || !org.bukkit.Registry.class.equals(registryType.getRawType()))
                return Optional.empty();
            var elementType = registryType.getActualTypeArguments()[0];
            var elementClass = TypeUtils.getRawType(elementType, null);
            if (elementClass == null || !Keyed.class.isAssignableFrom(elementClass))
                return Optional.empty();
            var registryKey = Key.key(Key.MINECRAFT_NAMESPACE, CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_UNDERSCORE, field.target().getName()));
            return Optional.of(new PaperRegistry(TypeToken.get(elementType), (org.bukkit.Registry<?>)field.get(null), registryKey));
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
        return unique(collected);
    }

    public static @NotNull Collection<PaperRegistry<?>> paperRegistries(@NotNull io.papermc.paper.registry.RegistryAccess registryAccess) {
        Map<TypeToken<?>, List<PaperRegistry<?>>> collected = new HashMap<>();
        Stream.of(RegistryKey.class.getDeclaredFields())
                .map(ReflectionField::of)
                .filter(v -> v.is(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL))
                .filter(v -> RegistryKey.class.isAssignableFrom(v.target().getType()))
                .map(v -> PaperRegistry.create(registryAccess, v))
                .flatMap(Optional::stream)
                .forEach(v -> collected.computeIfAbsent(v.type(), ignored -> new ArrayList<>()).add(v));
        Stream.of(org.bukkit.Registry.class.getDeclaredFields())
                .map(ReflectionField::of)
                .filter(v -> v.is(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL))
                .filter(v -> org.bukkit.Registry.class.isAssignableFrom(v.target().getType()))
                .map(PaperRegistry::createLegacy)
                .flatMap(Optional::stream)
                .forEach(v -> {
                    var registries = collected.computeIfAbsent(v.type(), ignored -> new ArrayList<>());
                    if (registries.isEmpty())
                        registries.add(v);
                });
        return unique(collected);
    }

    private static <T> @NotNull Collection<T> unique(@NotNull Map<TypeToken<?>, List<T>> collected) {
        return collected.values().stream()
                .filter(v -> v.size() == 1)
                .map(List::getFirst)
                .toList();
    }
}
