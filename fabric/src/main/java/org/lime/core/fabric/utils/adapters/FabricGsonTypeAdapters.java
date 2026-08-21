package org.lime.core.fabric.utils.adapters;

import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.Vec3;
import org.lime.core.common.utils.adapters.CommonGsonTypeAdapters;
import org.lime.core.common.utils.adapters.StringTypeAdapter;
import org.lime.core.fabric.utils.RegistryUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

public class FabricGsonTypeAdapters
        extends CommonGsonTypeAdapters {
    @Inject protected RegistryAccess registryAccess;
    @Inject protected MinecraftServer server;

    protected TypeAdapterFactory blockPos() {
        final TypeAdapter<BlockPos> keyTypeAdapter = new StringTypeAdapter<>() {
            @Override
            public String write(BlockPos value) {
                return value.getX() + " " + value.getY() + " " + value.getZ();
            }
            @Override
            public BlockPos read(String value) {
                String[] parts = value.split(" ");
                return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            }
        };
        return TypeAdapters.newFactory(BlockPos.class, keyTypeAdapter);
    }
    protected TypeAdapterFactory vec3() {
        final TypeAdapter<Vec3> keyTypeAdapter = new StringTypeAdapter<>() {
            @Override
            public String write(Vec3 value) {
                return value.x() + " " + value.y() + " " + value.z();
            }
            @Override
            public Vec3 read(String value) {
                String[] parts = value.split(" ");
                return new Vec3(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
            }
        };
        return TypeAdapters.newFactory(Vec3.class, keyTypeAdapter);
    }
    protected <T> TypeAdapterFactory resourceKey(
            Class<T> resourceClass,
            Iterable<ResourceKey<T>> resourceKeys) {
        return TypeAdapters.newFactory(
                getParameterized(ResourceKey.class, resourceClass),
                new StringTypeAdapter<ResourceKey<T>>() {
                    @Override
                    public String write(ResourceKey<T> value) {
                        return value.location().toString();
                    }
                    @Override
                    public ResourceKey<T> read(String value) {
                        ResourceLocation location = Objects.requireNonNull(ResourceLocation.tryParse(value), "Invalid resource key: " + value);
                        for (ResourceKey<T> key : resourceKeys) {
                            if (key.location().equals(location))
                                return key;
                        }
                        throw new IllegalArgumentException("Resource "+resourceClass.getSimpleName()+"#"+location+" not found. Allowed: " + String.join(", ", Iterables.transform(resourceKeys, v -> Objects.requireNonNull(v).location().toString())));
                    }
                });
    }
    protected <T>TypeAdapterFactory resourceKey(
            Class<T> resourceClass,
            ResourceKey<Registry<T>> registryKey,
            RegistryAccess registryAccess) {
        return resourceKey(resourceClass, registryAccess.lookupOrThrow(registryKey));
    }
    protected <T>TypeAdapterFactory resourceKey(
            Class<T> resourceClass,
            HolderLookup.RegistryLookup<T> registry) {
        return TypeAdapters.newFactory(
                getParameterized(ResourceKey.class, resourceClass),
                new StringTypeAdapter<ResourceKey<T>>() {
                    @Override
                    public String write(ResourceKey<T> value) {
                        return value.location().toString();
                    }
                    @Override
                    public ResourceKey<T> read(String value) {
                        ResourceLocation location = Objects.requireNonNull(ResourceLocation.tryParse(value), "Invalid resource key: " + value);
                        return registry.listElementIds()
                                .filter(v -> v.location().equals(location))
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException("Missing element " + location + " in " + registry.key()));
                    }
                });
    }
    protected TypeAdapterFactory resourceKeyAuto(RegistryAccess registryAccess) {
        Map<TypeToken<?>, Registry<?>> registries = new HashMap<>();
        RegistryUtils.nmsRegistries(registryAccess).forEach(v -> registries.put(TypeToken.getParameterized(ResourceKey.class, v.type().getType()), v.registry()));
        return new TypeAdapterFactory() {
            @Override
            public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
                Registry<?> registry = registries.get(type);
                if (registry == null)
                    return null;
                return new StringTypeAdapter<>() {
                    @Override
                    public String write(T value) throws IOException {
                        return ((ResourceKey<?>)value).location().toString();
                    }
                    @SuppressWarnings("unchecked")
                    @Override
                    public T read(String value) throws IOException {
                        ResourceLocation location = Objects.requireNonNull(ResourceLocation.tryParse(value), "Invalid resource key: " + value);
                        return registry.keySet().stream()
                                .filter(location::equals)
                                .map(v -> (T)ResourceKey.create(registry.key(), v))
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException("Missing element " + location + " in " + registry.key()));
                    }
                };
            }
        };
    }

    @Override
    public Stream<TypeAdapterFactory> factories() {
        return Streams.concat(
                Stream.of(
                        resourceKeyAuto(registryAccess),
                        new CodecTypeAdapterFactory(server),
                        blockPos(),
                        vec3()),
                super.factories());
    }
}
