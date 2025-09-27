package org.lime.core.paper.services.buffers;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

public class IndexedEntityBuffer<Index, T extends Entity>
        extends BaseEntityBuffer<Index, T> {
    protected Class<Index> indexClass;

    protected IndexedEntityBuffer(EntityBufferStorage owner, EntityBufferSetup setup, Class<Index> indexClass, Class<T> tClass) {
        super(owner, setup, tClass);
        this.indexClass = indexClass;
    }

    @Override
    public T nextBuffer(Index index) {
        return super.nextBuffer(index);
    }
    @Override
    public T nextBuffer(Index index, @Nullable Location location) {
        return super.nextBuffer(index, location);
    }
}
