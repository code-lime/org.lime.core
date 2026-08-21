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
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.Vec3;
import org.bukkit.*;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.lime.core.common.reflection.HierarchyMap;
import org.lime.core.common.utils.adapters.CommonGsonTypeAdapters;
import org.lime.core.common.utils.adapters.StringTypeAdapter;
import org.lime.core.paper.utils.RegistryUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

public class PaperGsonTypeAdapters
        extends CommonGsonTypeAdapters {
    @Inject protected RegistryAccess registryAccess;
    @Inject protected io.papermc.paper.registry.RegistryAccess registryAccessPaper;
    @Inject protected MinecraftServer server;
    @Inject Logger logger;

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
                        return ResourceLocationIdentifierProxy.identifierLocation(value).toString();
                    }
                    @Override
                    public ResourceKey<T> read(String value) {
                        ResourceLocationIdentifierProxy location = ResourceLocationIdentifierProxy.parse(value);
                        for (ResourceKey<T> key : resourceKeys) {
                            if (location.equals(ResourceLocationIdentifierProxy.identifierLocation(key)))
                                return key;
                        }
                        throw new IllegalArgumentException("Resource "+resourceClass.getSimpleName()+"#"+location+" not found. Allowed: " + String.join(", ", Iterables.transform(resourceKeys, v -> ResourceLocationIdentifierProxy.identifierLocation(Objects.requireNonNull(v)).toString())));
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
                        return ResourceLocationIdentifierProxy.identifierLocation(value).toString();
                    }
                    @Override
                    public ResourceKey<T> read(String value) {
                        ResourceLocationIdentifierProxy location = ResourceLocationIdentifierProxy.parse(value);
                        return registry.listElementIds()
                                .filter(v -> ResourceLocationIdentifierProxy.identifierLocation(v).equals(location))
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
                        return ResourceLocationIdentifierProxy.identifierLocation((ResourceKey<?>)value).toString();
                    }
                    @SuppressWarnings("unchecked")
                    @Override
                    public T read(String value) throws IOException {
                        ResourceLocationIdentifierProxy location = ResourceLocationIdentifierProxy.parse(value);
                        return registry.listElementIds()
                                .filter(v -> ResourceLocationIdentifierProxy.identifierLocation(v).equals(location))
                                .map(v -> (T)v)
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException("Missing element " + location + " in " + registry.key()));
                    }
                };
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
    protected TypeAdapterFactory registryKeyAuto(io.papermc.paper.registry.RegistryAccess registryAccess) {
        Map<TypeToken<?>, RegistryUtils.PaperRegistry<?>> registries = new HashMap<>();
        RegistryUtils.paperRegistries(registryAccess).forEach(v -> registries.put(v.type(), v));
        return new TypeAdapterFactory() {
            final Map<TypeToken<?>, RegistryUtils.PaperRegistry<?>> hierarchy = HierarchyMap.ofToken(registries);
            @Override
            public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
                RegistryUtils.PaperRegistry<?> registryEntry = hierarchy.get(type);
                if (registryEntry == null)
                    return null;
                var registry = registryEntry.registry();
                logger.info("Created registry adapter for {} by {}", type, registryEntry.registryKey().asString());
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
        return Streams.concat(
                Stream.of(
                        resourceKeyAuto(registryAccess),
                        registryKeyAuto(registryAccessPaper),
                        new CodecTypeAdapterFactory(server),
                        TypeAdapters.newTypeHierarchyFactory(ConfigurationSerializable.class, new ConfigurationSerializableTypeAdapter(gson)
                                .addByJson(ItemStack.class, Bukkit.getUnsafe()::serializeItemAsJson, Bukkit.getUnsafe()::deserializeItemFromJson)),
                        blockPos(),
                        vec3(),
                        positions()),
                super.factories());
    }
}
