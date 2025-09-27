package org.lime.core.paper.services.buffers;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

public class IndexedEntityBuffer<Index, T extends Entity>
        extends BaseEntityBuffer<Index, T> {
    protected Class<Index> indexClass;

    protected IndexedEntityBuffer(EntityBufferStorage owner, String tag, Class<Index> indexClass, Class<T> tClass, World defaultWorld) {
        super(owner, tag, tClass, defaultWorld);
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
