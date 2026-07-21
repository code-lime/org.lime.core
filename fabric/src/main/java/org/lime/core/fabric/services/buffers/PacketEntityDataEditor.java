package org.lime.core.fabric.services.buffers;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.lime.core.common.services.buffers.BasePacketEntityDataEditor;

/**
 * Builds the metadata overlay of one packet entity for one player.
 */
public final class PacketEntityDataEditor
        extends BasePacketEntityDataEditor<SynchedEntityData, SynchedEntityData.DataValue<?>> {
    PacketEntityDataEditor(@NotNull SynchedEntityData entityData) {
        super(entityData);
    }

    /**
     * Resolves a reusable immutable descriptor for a Mojang-mapped static
     * {@link EntityDataAccessor} field.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <Value> @NotNull PropertyAccess<Value> property(@NotNull Class<? extends Entity> entityClass, @NotNull String mojangField) {
        EntityDataAccessor<Value> property = (EntityDataAccessor<Value>)resolveProperty(entityClass, mojangField, (Class)EntityDataAccessor.class);
        return new PropertyAccess<>(property);
    }

    public <Value> @NotNull Value get(@NotNull PropertyAccess<Value> property) {
        return getValue(property);
    }

    public <Value> @NotNull PacketEntityDataEditor set(@NotNull PropertyAccess<Value> property, @NotNull Value value) {
        setValue(property, value);
        return this;
    }

    public @NotNull PacketEntityDataEditor clear(@NotNull PropertyAccess<?> property) {
        clearValue(property);
        return this;
    }

    public @NotNull PacketEntityDataEditor clearAll() {
        clearAllValues();
        return this;
    }

    private static int id(@NotNull EntityDataAccessor<?> property) {
        //#switch PROPERTIES.versionMinecraft
        //#caseof 1.20.1
        return property.getId();
        //#default
        //OF//        return property.id();
        //#endswitch
    }

    private static <Value> @NotNull EntityDataSerializer<Value> serializer(@NotNull EntityDataAccessor<Value> property) {
        //#switch PROPERTIES.versionMinecraft
        //#caseof 1.20.1
        return property.getSerializer();
        //#default
        //OF//        return property.serializer();
        //#endswitch
    }

    /** Immutable metadata property descriptor reusable across editors and viewers. */
    public static final class PropertyAccess<Value>
            extends BasePacketEntityDataEditor.PropertyAccess<Value, SynchedEntityData, SynchedEntityData.DataValue<?>> {
        private final EntityDataAccessor<Value> property;
        private final EntityDataSerializer<Value> serializer;

        private PropertyAccess(@NotNull EntityDataAccessor<Value> property) {
            super(PacketEntityDataEditor.id(property));
            this.property = property;
            this.serializer = PacketEntityDataEditor.serializer(property);
        }

        boolean matches(@NotNull SynchedEntityData.DataValue<?> entry) {
            return id() == entry.id() && serializer == entry.serializer();
        }

        @Override
        protected @NotNull Value read(@NotNull SynchedEntityData entityData) {
            return serializer.copy(entityData.get(property));
        }

        @Override
        @SuppressWarnings("unchecked")
        protected @NotNull Value decode(@NotNull SynchedEntityData.DataValue<?> entry) {
            return serializer.copy((Value)entry.value());
        }

        @Override
        protected @NotNull SynchedEntityData.DataValue<?> encode(@NotNull Value value) {
            return SynchedEntityData.DataValue.create(property, value);
        }
    }
}
