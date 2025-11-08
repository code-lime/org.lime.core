package org.lime.core.paper.services.buffers;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.lime.core.common.services.buffers.BaseEntityBufferSetup;
import org.lime.core.common.services.buffers.BaseIterationEntityBuffer;

public class IterationEntityBuffer<T extends Entity>
        extends BaseIterationEntityBuffer<T, Entity, Location> {
    protected IterationEntityBuffer(EntityBufferStorage owner, BaseEntityBufferSetup<Location> setup, Class<T> tClass) {
        super(owner, setup, tClass);
    }
}
