package org.lime.core.common.services.buffers;

import org.jetbrains.annotations.Nullable;

public abstract class BaseIterationEntityBuffer<T extends Entity, Entity, Location>
        extends BaseEntityBuffer<Integer, T, Entity, Location> {
    protected BaseIterationEntityBuffer(BaseEntityBufferStorage<Entity, Location> owner, BaseEntityBufferSetup<Location> setup, Class<T> tClass) {
        super(owner, setup, tClass);
    }

    public T nextBuffer() {
        if (usedIndexes == null)
            throw new IllegalArgumentException("Buffer "+this.tag+" not begin");
        return indexedNextBuffer(usedIndexes.size());
    }
    public T nextBuffer(@Nullable Location location) {
        if (usedIndexes == null)
            throw new IllegalArgumentException("Buffer "+this.tag+" not begin");
        return indexedNextBuffer(usedIndexes.size(), location);
    }
}
