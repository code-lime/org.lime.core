package org.lime.core.paper.services.buffers;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.lime.core.common.services.buffers.BaseEntityBufferSetup;
import org.lime.core.common.services.buffers.BaseIndexedEntityBuffer;

public class IndexedEntityBuffer<Index, T extends Entity>
        extends BaseIndexedEntityBuffer<Index, T, Entity, Location> {
    protected IndexedEntityBuffer(EntityBufferStorage owner, BaseEntityBufferSetup<Location> setup, Class<Index> indexClass, Class<T> tClass) {
        super(owner, setup, indexClass, tClass);
    }
}
