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
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.bukkit.*;
import org.jetbrains.annotations.NotNull;
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
    @Inject protected io.papermc.paper.registry.RegistryAccess registryAccessPaper;
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
        Map<TypeToken<? extends ResourceKey<?>>, List<ResourceKey<Registry<?>>>> collected = new HashMap<>();
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
                    var type = registryType.getActualTypeArguments()[0];
                    if (type.getTypeName().startsWith("java"))
                        return;
                    var typeToken = (TypeToken<? extends ResourceKey<?>>)TypeToken.getParameterized(ResourceKey.class, type);
                    ResourceKey<Registry<?>> resourceKey = (ResourceKey<Registry<?>>)v.get(null);
                    collected.computeIfAbsent(typeToken, vv -> new ArrayList<>()).add(resourceKey);
                });
        Map<TypeToken<? extends ResourceKey<?>>, ResourceKey<Registry<?>>> registries = new HashMap<>();
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
                TypeAdapters.newTypeHierarchyFactory(World.class, new StringTypeAdapter<World>() {
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
    protected TypeAdapterFactory registryKeyAuto(
            io.papermc.paper.registry.RegistryAccess registryAccess) {
        Map<TypeToken<?>, List<org.bukkit.Registry<?>>> collected = new HashMap<>();
        Stream.of(RegistryKey.class.getDeclaredFields())
                .map(ReflectionField::of)
                .filter(v -> v.is(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL))
                .filter(v -> RegistryKey.class.isAssignableFrom(v.target().getType()))
                .forEach(v -> {
                    if (!(v.target().getGenericType() instanceof ParameterizedType registryKeyType))
                        return;
                    if (!RegistryKey.class.equals(registryKeyType.getRawType()))
                        return;
                    var itemType = registryKeyType.getActualTypeArguments()[0];
                    var itemTypeClass = TypeUtils.getRawType(itemType, null);
                    if (itemTypeClass == null || !Keyed.class.isAssignableFrom(itemTypeClass))
                        return;
                    var typeToken = TypeToken.get(itemType);
                    @SuppressWarnings("unchecked")
                    org.bukkit.Registry<?> registry = registryAccess.getRegistry((RegistryKey<? extends @NotNull Keyed>)v.get(null));
                    collected.computeIfAbsent(typeToken, vv -> new ArrayList<>()).add(registry);
                });
        Stream.of(org.bukkit.Registry.class.getDeclaredFields())
                .map(ReflectionField::of)
                .filter(v -> v.is(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL))
                .filter(v -> org.bukkit.Registry.class.isAssignableFrom(v.target().getType()))
                .forEach(v -> {
                    if (!(v.target().getGenericType() instanceof ParameterizedType registryKeyType))
                        return;
                    if (!org.bukkit.Registry.class.equals(registryKeyType.getRawType()))
                        return;
                    var itemType = registryKeyType.getActualTypeArguments()[0];
                    var itemTypeClass = TypeUtils.getRawType(itemType, null);
                    if (itemTypeClass == null || !Keyed.class.isAssignableFrom(itemTypeClass))
                        return;
                    var typeToken = TypeToken.get(itemType);
                    org.bukkit.Registry<?> registry = (org.bukkit.Registry<?>)v.get(null);
                    var list = collected.computeIfAbsent(typeToken, vv -> new ArrayList<>());
                    if (list.isEmpty())
                        list.add(registry);
                });
        Map<TypeToken<?>, org.bukkit.Registry<?>> registries = new HashMap<>();
        collected.forEach((type, list) -> {
            if (list.size() == 1)
                registries.put(type, list.getFirst());
        });
        return new TypeAdapterFactory() {
            @Override
            public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
                org.bukkit.Registry<?> registry = registries.get(type);
                if (registry == null)
                    return null;
                return new StringTypeAdapter<>() {
                    @Override
                    public String write(T value) throws IOException {
                        return ((Keyed) value).key().asString();
                    }
                    @SuppressWarnings({"unchecked", "PatternValidation"})
                    @Override
                    public T read(String value) throws IOException {
                        return (T) registry.getOrThrow(Key.key(value));
                    }
                };
            }
        };
    }

    @Override
    public Stream<TypeAdapterFactory> factories() {
        return Streams.concat(super.factories(), Stream.of(
                blockPos(),
                vec3(),
                resourceKeyAuto(registryAccess),
                registryKeyAuto(registryAccessPaper),
                positions(),
                new CodecTypeAdapterFactory(server)
        ));
    }
}
