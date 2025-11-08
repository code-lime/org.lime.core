package org.lime.core.common.services.buffers;

import org.jetbrains.annotations.Nullable;

public abstract class BaseIndexedEntityBuffer<Index, T extends Entity, Entity, Location>
        extends BaseEntityBuffer<Index, T, Entity, Location> {
    protected Class<Index> indexClass;

    protected BaseIndexedEntityBuffer(BaseEntityBufferStorage<Entity, Location> owner, BaseEntityBufferSetup<Location> setup, Class<Index> indexClass, Class<T> tClass) {
        super(owner, setup, tClass);
        this.indexClass = indexClass;
    }

    @Override
    public T indexedNextBuffer(Index index) {
        return super.indexedNextBuffer(index);
    }
    @Override
    public T indexedNextBuffer(Index index, @Nullable Location location) {
        return super.indexedNextBuffer(index, location);
    }
}
