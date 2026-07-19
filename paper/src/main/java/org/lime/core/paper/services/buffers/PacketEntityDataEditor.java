package org.lime.core.paper.services.buffers;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import org.lime.core.common.services.buffers.BasePacketEntityDataEditor;

import java.util.Map;

/**
 * Builds a per-player overlay over an entity's synchronized data without
 * changing the entity itself.
 */
public final class PacketEntityDataEditor
        extends BasePacketEntityDataEditor<
                EntityDataAccessor<?>,
                SynchedEntityData.DataValue<?>> {
    private final Int2ObjectMap<SynchedEntityData.DataValue<?>> canonicalValues;

    PacketEntityDataEditor(Int2ObjectMap<SynchedEntityData.DataValue<?>> canonicalValues) {
        this.canonicalValues = canonicalValues;
    }

    /**
     * Resolves a Mojang-named static {@link EntityDataAccessor} field. Mojang
     * names are resolved through the runtime mappings and therefore also work
     * on an obfuscated production server.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T> EntityDataAccessor<T> property(Class<?> entityClass, String mojangField) {
        return (EntityDataAccessor<T>)resolveProperty(
                entityClass,
                mojangField,
                (Class)EntityDataAccessor.class);
    }

    /** Returns the effective value, including edits made by earlier callbacks. */
    public <T> T get(EntityDataAccessor<T> property) {
        return getValue(property);
    }

    /** Sets a persistent per-player override for this recomputation. */
    public <T> PacketEntityDataEditor set(EntityDataAccessor<T> property, T value) {
        setValue(property, value);
        return this;
    }

    /** Removes an override and exposes the canonical entity value again. */
    public PacketEntityDataEditor clear(EntityDataAccessor<?> property) {
        clearValue(property);
        return this;
    }

    /** Removes all overrides produced by earlier callbacks. */
    public PacketEntityDataEditor clear() {
        clearValues();
        return this;
    }

    Map<Integer, SynchedEntityData.DataValue<?>> snapshot() {
        return snapshotValues();
    }

    @Override
    protected int propertyId(EntityDataAccessor<?> property) {
        return property.id();
    }

    @Override
    protected Object canonicalValue(EntityDataAccessor<?> property) {
        SynchedEntityData.DataValue<?> value = canonicalValues.get(property.id());
        if (value == null)
            throw new IllegalArgumentException("Entity data property " + property.id() + " is not defined by this entity");
        validateSerializer(property, value);
        return value.value();
    }

    @Override
    protected Object entryValue(
            EntityDataAccessor<?> property,
            SynchedEntityData.DataValue<?> entry) {
        validateSerializer(property, entry);
        return entry.value();
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected Object copyValue(EntityDataAccessor<?> property, Object value) {
        return ((EntityDataAccessor)property).serializer().copy(value);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected SynchedEntityData.DataValue<?> createEntry(EntityDataAccessor<?> property, Object value) {
        return SynchedEntityData.DataValue.create((EntityDataAccessor)property, value);
    }

    static boolean matches(EntityDataAccessor<?> first, EntityDataAccessor<?> second) {
        return first.id() == second.id() && first.serializer() == second.serializer();
    }

    static boolean matches(EntityDataAccessor<?> property, SynchedEntityData.DataValue<?> value) {
        return property.id() == value.id() && property.serializer() == value.serializer();
    }

    private static void validateSerializer(
            EntityDataAccessor<?> property,
            SynchedEntityData.DataValue<?> value) {
        if (property.serializer() != value.serializer()) {
            throw new IllegalArgumentException(
                    "Entity data property " + property.id() + " has a different serializer");
        }
    }

}
