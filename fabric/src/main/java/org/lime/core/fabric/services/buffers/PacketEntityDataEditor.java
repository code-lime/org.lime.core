package org.lime.core.fabric.services.buffers;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import org.lime.core.common.reflection.ReflectionMethod;
import org.lime.core.common.services.buffers.BasePacketEntityDataEditor;
import org.lime.core.common.utils.execute.Func2;

import java.util.Map;

/**
 * Builds the metadata overlay of one packet entity for one player.
 */
public final class PacketEntityDataEditor
        extends BasePacketEntityDataEditor<
                EntityDataAccessor<?>,
                PacketEntityDataEditor.Entry<?>> {
    private static final Func2<
            SynchedEntityData,
            EntityDataAccessor<?>,
            SynchedEntityData.DataItem<?>> GET_ITEM = ReflectionMethod.ofMojang(
                    SynchedEntityData.class,
                    "getItem",
                    EntityDataAccessor.class)
            .lambda(Func2.class);

    private final SynchedEntityData entityData;

    PacketEntityDataEditor(SynchedEntityData entityData) {
        this.entityData = entityData;
    }

    /**
     * Resolves a Mojang-mapped static {@link EntityDataAccessor} field.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <Value> EntityDataAccessor<Value> property(
            Class<? extends Entity> entityClass,
            String mojangField) {
        return (EntityDataAccessor<Value>)resolveProperty(
                entityClass,
                mojangField,
                (Class)EntityDataAccessor.class);
    }

    public <Value> Value get(EntityDataAccessor<Value> property) {
        return getValue(property);
    }

    public <Value> PacketEntityDataEditor set(EntityDataAccessor<Value> property, Value value) {
        setValue(property, value);
        return this;
    }

    public PacketEntityDataEditor clear(EntityDataAccessor<?> property) {
        clearValue(property);
        return this;
    }

    public PacketEntityDataEditor clear() {
        clearValues();
        return this;
    }

    Map<Integer, Entry<?>> snapshot() {
        return snapshotValues();
    }

    @Override
    protected int propertyId(EntityDataAccessor<?> property) {
        return id(property);
    }

    @Override
    protected Object canonicalValue(EntityDataAccessor<?> property) {
        requireCanonical(property);
        return entityData.get(property);
    }

    @Override
    protected Object entryValue(EntityDataAccessor<?> property, Entry<?> entry) {
        if (!matches(property, entry.property()))
            throw invalidProperty(property);
        return entry.value().value();
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected Object copyValue(EntityDataAccessor<?> property, Object value) {
        return ((EntityDataSerializer)serializer((EntityDataAccessor)property)).copy(value);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected Entry<?> createEntry(EntityDataAccessor<?> property, Object value) {
        EntityDataAccessor rawProperty = (EntityDataAccessor)property;
        return new Entry(rawProperty, SynchedEntityData.DataValue.create(rawProperty, value));
    }

    private void requireCanonical(EntityDataAccessor<?> property) {
        SynchedEntityData.DataItem<?> item = GET_ITEM.invoke(entityData, property);
        if (!matches(property, item.getAccessor()))
            throw invalidProperty(property);
    }

    private static IllegalArgumentException invalidProperty(EntityDataAccessor<?> property) {
        return new IllegalArgumentException(
                "Entity data property " + id(property) + " has a different serializer");
    }

    static int id(EntityDataAccessor<?> property) {
        //#switch PROPERTIES.versionMinecraft
        //#caseof 1.20.1
        return property.getId();
        //#default
        //OF// return property.id();
        //#endswitch
    }

    static <Value> EntityDataSerializer<Value> serializer(EntityDataAccessor<Value> property) {
        //#switch PROPERTIES.versionMinecraft
        //#caseof 1.20.1
        return property.getSerializer();
        //#default
        //OF//        return property.serializer();
        //#endswitch
    }

    static boolean matches(EntityDataAccessor<?> first, EntityDataAccessor<?> second) {
        return id(first) == id(second) && serializer(first) == serializer(second);
    }

    static boolean matches(EntityDataAccessor<?> property, SynchedEntityData.DataValue<?> value) {
        return id(property) == value.id() && serializer(property) == value.serializer();
    }

    record Entry<Value>(
            EntityDataAccessor<Value> property,
            SynchedEntityData.DataValue<Value> value) {
    }
}
