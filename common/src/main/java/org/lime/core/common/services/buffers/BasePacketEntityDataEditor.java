package org.lime.core.common.services.buffers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.reflection.ReflectionField;

import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Platform-neutral storage and lifecycle for a per-viewer entity-data overlay.
 * Platform implementations expose each metadata property through an immutable,
 * reusable {@link PropertyAccess}.
 */
public abstract class BasePacketEntityDataEditor<Data, Entry> {
    private static final ConcurrentHashMap<PropertyKey, Object> PROPERTY_CACHE = new ConcurrentHashMap<>();

    final Data data;
    final LinkedHashMap<PropertyAccess<?, Data, Entry>, Entry> overrides = new LinkedHashMap<>();

    protected BasePacketEntityDataEditor(@NotNull Data data) {
        this.data = data;
    }

    protected final <Value> @NotNull Value getValue(@NotNull PropertyAccess<Value, Data, Entry> access) {
        Entry current = overrides.get(access);
        if (current == null)
            return access.read(data);
        return access.decode(current);
    }

    protected final <Value> void setValue(@NotNull PropertyAccess<Value, Data, Entry> access, @NotNull Value value) {
        Entry entry = access.encode(value);
        overrides.remove(access);
        overrides.put(access, entry);
    }

    protected final void clearValue(@NotNull PropertyAccess<?, Data, Entry> access) {
        overrides.remove(access);
    }

    protected final void clearAllValues() {
        overrides.clear();
    }

    /** Immutable typed descriptor of one heterogeneous metadata property. */
    public abstract static class PropertyAccess<Value, Data, Entry> {
        private final int id;

        protected PropertyAccess(int id) {
            this.id = id;
        }

        protected abstract @NotNull Value read(@NotNull Data data);
        protected abstract @NotNull Value decode(@NotNull Entry entry);
        protected abstract @NotNull Entry encode(@NotNull Value value);

        public final int id() {
            return id;
        }

        final @NotNull Entry reset(@NotNull Data data) {
            return encode(read(data));
        }

        @Override
        public final boolean equals(@Nullable Object value) {
            return this == value || value instanceof PropertyAccess<?, ?, ?> access && id == access.id;
        }

        @Override
        public final int hashCode() {
            return id;
        }
    }

    protected static <Property> @NotNull Property resolveProperty(@NotNull Class<?> ownerClass, @NotNull String mojangField, @NotNull Class<Property> propertyClass) {
        PropertyKey key = new PropertyKey(ownerClass, mojangField, propertyClass);
        return propertyClass.cast(PROPERTY_CACHE.computeIfAbsent(key, PropertyKey::resolve));
    }

    private record PropertyKey(@NotNull Class<?> ownerClass, @NotNull String mojangField, @NotNull Class<?> propertyClass) {
        private @NotNull Object resolve() {
            return ReflectionField.ofMojang(ownerClass, mojangField).get(null);
        }
    }
}
