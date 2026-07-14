package org.lime.core.paper.services.buffers;

import com.google.inject.TypeLiteral;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.lime.core.common.services.buffers.BaseEntityBufferStorage;
import org.lime.core.common.services.buffers.BaseEntityBufferSetup;
import org.lime.core.common.services.buffers.BaseIndexedEntityBuffer;

public class IndexedEntityBuffer<Index, T extends Entity>
        extends BaseIndexedEntityBuffer<Index, T, Entity, Location> {
    protected IndexedEntityBuffer(EntityBufferStorage owner, BaseEntityBufferSetup<Location> setup, TypeLiteral<Index> indexClass, Class<T> tClass) {
        this((BaseEntityBufferStorage<Entity, Location>)owner, setup, indexClass, tClass);
    }
    protected IndexedEntityBuffer(BaseEntityBufferStorage<Entity, Location> owner, BaseEntityBufferSetup<Location> setup, TypeLiteral<Index> indexClass, Class<T> tClass) {
        super(owner, setup, indexClass, tClass);
    }
}
