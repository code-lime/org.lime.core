package org.lime.core.paper.utils.adapters;

import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.FinePosition;
import io.papermc.paper.math.Position;
import net.kyori.adventure.key.Key;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.lime.core.common.reflection.ReflectionField;
import org.lime.core.common.utils.adapters.CommonGsonTypeAdapters;
import org.lime.core.common.utils.adapters.StringTypeAdapter;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Stream;

public class PaperGsonTypeAdapters
        extends CommonGsonTypeAdapters {
    @Inject protected RegistryAccess registryAccess;

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
                        ResourceLocation location = ResourceLocation.parse(value);
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
                        ResourceLocation location = ResourceLocation.parse(value);
                        return registry.listElementIds()
                                .filter(v -> v.location().equals(location))
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException("Missing element " + location + " in " + registry.key()));
                    }
                });
    }
    protected TypeAdapterFactory resourceKeyAuto(
            RegistryAccess registryAccess) {
        Map<TypeToken<?>, List<ResourceKey<Registry<?>>>> collected = new HashMap<>();
        Stream.of(Registries.class.getDeclaredFields())
                .map(ReflectionField::of)
                .filter(v -> v.is(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL))
                .filter(v -> ResourceKey.class.isAssignableFrom(v.target().getType()))
                .forEach(v -> {
                    if (!(v.target().getGenericType() instanceof ParameterizedType resourceKeyType))
                        return;
                    if (!ResourceKey.class.equals(resourceKeyType.getRawType()))
                        return;
                    if (!(resourceKeyType.getActualTypeArguments()[0] instanceof ParameterizedType registryType))
                        return;
                    if (!Registry.class.equals(registryType.getRawType()))
                        return;
                    var typeToken = TypeToken.get(registryType.getActualTypeArguments()[0]);
                    ResourceKey<Registry<?>> resourceKey = (ResourceKey<Registry<?>>)v.get(null);
                    collected.computeIfAbsent(typeToken, vv -> new ArrayList<>()).add(resourceKey);
                });
        Map<TypeToken<?>, ResourceKey<Registry<?>>> registries = new HashMap<>();
        collected.forEach((type, list) -> {
            if (list.size() == 1)
                registries.put(type, list.get(0));
        });
        return new TypeAdapterFactory() {
            @Override
            public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
                ResourceKey<Registry<?>> registryKey = registries.get(type);
                if (registryKey == null)
                    return null;
                return registryAccess.lookup(registryKey)
                        .map(registry -> new StringTypeAdapter<T>() {
                            @Override
                            public String write(T value) throws IOException {
                                return ((ResourceKey<?>)value).location().toString();
                            }
                            @SuppressWarnings("unchecked")
                            @Override
                            public T read(String value) throws IOException {
                                ResourceLocation location = ResourceLocation.parse(value);
                                return registry.listElementIds()
                                        .filter(v -> v.location().equals(location))
                                        .map(v -> (T)v)
                                        .findFirst()
                                        .orElseThrow(() -> new IllegalStateException("Missing element " + location + " in " + registry.key()));
                            }
                        })
                        .orElse(null);
            }
        };
    }
    protected TypeAdapterFactory positions() {
        return combine(
                TypeAdapters.newFactory(World.class, new StringTypeAdapter<World>() {
                    @Override
                    public String write(World value) throws IOException {
                        return value.key().toString();
                    }
                    @Override
                    public World read(String value) throws IOException {
                        return Bukkit.getWorld(Key.key(value));
                    }
                }),
                TypeAdapters.newFactory(Location.class, new PositionTypeAdapters.LocationTypeAdapter()),
                TypeAdapters.newTypeHierarchyFactory(FinePosition.class, new PositionTypeAdapters.FineTypeAdapter()),
                TypeAdapters.newTypeHierarchyFactory(BlockPosition.class, new PositionTypeAdapters.BlockTypeAdapter()),
                TypeAdapters.newTypeHierarchyFactory(Position.class, new PositionTypeAdapters.BaseTypeAdapter()));
    }
    protected TypeAdapterFactory material() {
        final TypeAdapter<Material> keyTypeAdapter = new StringTypeAdapter<>() {
            @Override
            public String write(Material value) {
                return org.bukkit.Registry.MATERIAL.getKeyOrThrow(value).toString();
            }
            @Override
            public Material read(String value) {
                return org.bukkit.Registry.MATERIAL.getOrThrow(Key.key(value));
            }
        };
        return TypeAdapters.newFactory(Material.class, keyTypeAdapter);
    }

    @Override
    public Stream<TypeAdapterFactory> factories() {
        return Streams.concat(super.factories(), Stream.of(
                blockPos(),
                vec3(),
                resourceKeyAuto(registryAccess),
                positions(),
                material()
        ));
    }
}
