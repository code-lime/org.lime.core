package org.lime.core.common.services.buffers;

import org.lime.core.common.reflection.ReflectionField;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Platform-neutral storage and lifecycle for a per-viewer entity-data overlay.
 * Platform implementations only provide access to their metadata property,
 * serializer and packed-value types.
 */
public abstract class BasePacketEntityDataEditor<Property, Entry> {
    private static final ConcurrentHashMap<PropertyKey, Object> PROPERTY_CACHE = new ConcurrentHashMap<>();

    private final LinkedHashMap<Integer, Entry> values = new LinkedHashMap<>();

    protected abstract int propertyId(Property property);
    protected abstract Object canonicalValue(Property property);
    protected abstract Object entryValue(Property property, Entry entry);
    protected abstract Object copyValue(Property property, Object value);
    protected abstract Entry createEntry(Property property, Object value);

    @SuppressWarnings("unchecked")
    protected final <Value> Value getValue(Property property) {
        Objects.requireNonNull(property, "property");
        Entry entry = values.get(propertyId(property));
        Object value = entry == null ? canonicalValue(property) : entryValue(property, entry);
        return (Value)copyValue(property, value);
    }

    protected final <Value> void setValue(Property property, Value value) {
        Objects.requireNonNull(property, "property");
        canonicalValue(property);
        values.put(propertyId(property), createEntry(property, value));
    }

    protected final void clearValue(Property property) {
        Objects.requireNonNull(property, "property");
        canonicalValue(property);
        values.remove(propertyId(property));
    }

    protected final void clearValues() {
        values.clear();
    }

    protected final Map<Integer, Entry> snapshotValues() {
        return values.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    protected static <Property> Property resolveProperty(
            Class<?> ownerClass,
            String mojangField,
            Class<Property> propertyClass) {
        Objects.requireNonNull(ownerClass, "ownerClass");
        Objects.requireNonNull(mojangField, "mojangField");
        Objects.requireNonNull(propertyClass, "propertyClass");

        PropertyKey key = new PropertyKey(ownerClass, mojangField, propertyClass);
        return propertyClass.cast(PROPERTY_CACHE.computeIfAbsent(key, PropertyKey::resolve));
    }

    private record PropertyKey(
            Class<?> ownerClass,
            String mojangField,
            Class<?> propertyClass) {
        private Object resolve() {
            Object value = ReflectionField.ofMojang(ownerClass, mojangField).get(null);
            if (!propertyClass.isInstance(value)) {
                throw new IllegalArgumentException(
                        ownerClass.getName() + "." + mojangField + " is not " + propertyClass.getName());
            }
            return value;
        }
    }
}
