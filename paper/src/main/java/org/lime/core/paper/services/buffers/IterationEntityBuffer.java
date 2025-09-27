package org.lime.core.paper.services.buffers;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

public class IterationEntityBuffer<T extends Entity>
        extends BaseEntityBuffer<Integer, T> {
    protected IterationEntityBuffer(EntityBufferStorage owner, EntityBufferSetup setup, Class<T> tClass) {
        super(owner, setup, tClass);
    }

    public T nextBuffer() {
        if (usedIndexes == null)
            throw new IllegalArgumentException("Buffer "+this.tag+" not begin");
        return nextBuffer(usedIndexes.size());
    }
    public T nextBuffer(@Nullable Location location) {
        if (usedIndexes == null)
            throw new IllegalArgumentException("Buffer "+this.tag+" not begin");
        return nextBuffer(usedIndexes.size(), location);
    }
}
