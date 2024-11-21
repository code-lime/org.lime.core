package org.lime.packetwrapper.data;

import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.AutoWrapper;
import com.comphenix.protocol.wrappers.MinecraftKey;

import java.util.Objects;

/**
 * @author Lukas Alt
 * @since 15.05.2023
 */
public class ResourceKey {
    public static final EquivalentConverter<ResourceKey> CONVERTER = AutoWrapper.wrap(ResourceKey.class,
                    MinecraftReflection.getResourceKey())
            .field(0,MinecraftKey.getConverter()).field(1, MinecraftKey.getConverter());

    private MinecraftKey registry;
    private MinecraftKey location;

    /**
     * Creates a new ResourceKey. A resource key represents a key inside a given registry of a fixed type.
     * @param registry The id of the registry
     * @param location The location withing the registry
     */
    public ResourceKey(MinecraftKey registry, MinecraftKey location) {
        this.registry = registry;
        this.location = location;
    }

    /**
     * Creates a new ResourceKey. A resource key represents a key inside a given registry of a fixed type.
     * With this constructor, both of them initially are null.
     */
    public ResourceKey() {
    }

    /**
     * Gets the minecraft key indicating the registry (e.g, 'minecraft:dimension_type')
     * @return type of the registry
     */
    public MinecraftKey getRegistry() {
        return registry;
    }

    /**
     * Sets the minecraft key indicating the registry (e.g, 'minecraft:dimension_type')
     * @param registry type of registry
     */
    public void setRegistry(MinecraftKey registry) {
        this.registry = registry;
    }

    /**
     * Gets the key of the resource within the registry.
     * @return resource key within registry
     */
    public MinecraftKey getLocation() {
        return location;
    }

    /**
     * Set the key of the resource within the registry.
     * @param location the resource key within registry
     */
    public void setLocation(MinecraftKey location) {
        this.location = location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourceKey that = (ResourceKey) o;

        if (!Objects.equals(registry, that.registry)) return false;
        return Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        int result = registry != null ? registry.hashCode() : 0;
        result = 31 * result + (location != null ? location.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ResourceKey{" +
                "registry=" + registry +
                ", location=" + location +
                '}';
    }
}
