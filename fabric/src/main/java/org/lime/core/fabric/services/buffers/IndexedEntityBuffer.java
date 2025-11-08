package org.lime.core.fabric.services.buffers;

import net.minecraft.world.entity.Entity;
import org.lime.core.common.services.buffers.BaseEntityBufferSetup;
import org.lime.core.common.services.buffers.BaseIndexedEntityBuffer;
import org.lime.core.fabric.utils.WorldLocation;

public class IndexedEntityBuffer<Index, T extends Entity>
        extends BaseIndexedEntityBuffer<Index, T, Entity, WorldLocation> {
    protected IndexedEntityBuffer(EntityBufferStorage owner, BaseEntityBufferSetup<WorldLocation> setup, Class<Index> indexClass, Class<T> tClass) {
        super(owner, setup, indexClass, tClass);
    }
}
