package org.lime.core.paper.services.buffers;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.SynchedEntityData;
import org.jetbrains.annotations.NotNull;
import org.lime.core.common.services.buffers.BasePacketEntityDataEditor;

/**
 * Builds a per-player overlay over an entity's synchronized data without
 * changing the entity itself.
 */
public final class PacketEntityDataEditor
        extends BasePacketEntityDataEditor<SynchedEntityData, SynchedEntityData.DataValue<?>> {
    PacketEntityDataEditor(@NotNull SynchedEntityData entityData) {
        super(entityData);
    }

    /**
     * Resolves a reusable immutable descriptor for a Mojang-named static
     * {@link EntityDataAccessor} field. Mojang names are resolved through the
     * runtime mappings and therefore also work on an obfuscated production server.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <Value> @NotNull PropertyAccess<Value> property(@NotNull Class<?> entityClass, @NotNull String mojangField) {
        EntityDataAccessor<Value> property = (EntityDataAccessor<Value>)resolveProperty(entityClass, mojangField, (Class)EntityDataAccessor.class);
        return new PropertyAccess<>(property);
    }

    /** Returns the effective value, including edits made by earlier callbacks. */
    public <Value> @NotNull Value get(@NotNull PropertyAccess<Value> property) {
        return getValue(property);
    }

    /** Sets a persistent per-player override for this recomputation. */
    public <Value> @NotNull PacketEntityDataEditor set(@NotNull PropertyAccess<Value> property, @NotNull Value value) {
        setValue(property, value);
        return this;
    }

    /** Removes an override and exposes the canonical entity value again. */
    public @NotNull PacketEntityDataEditor clear(@NotNull PropertyAccess<?> property) {
        clearValue(property);
        return this;
    }

    /** Removes all overrides produced by earlier callbacks. */
    public @NotNull PacketEntityDataEditor clearAll() {
        clearAllValues();
        return this;
    }

    /**
     * Immutable typed descriptor of one synchronized entity-data property.
     * Instances can be stored statically and reused for any compatible entity.
     */
    public static final class PropertyAccess<Value>
            extends BasePacketEntityDataEditor.PropertyAccess<Value, SynchedEntityData, SynchedEntityData.DataValue<?>> {
        private final EntityDataAccessor<Value> property;
        private final EntityDataSerializer<Value> serializer;

        private PropertyAccess(@NotNull EntityDataAccessor<Value> property) {
            super(property.id());
            this.property = property;
            this.serializer = property.serializer();
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

        boolean matches(@NotNull SynchedEntityData.DataValue<?> value) {
            return id() == value.id() && serializer == value.serializer();
        }
    }
}
