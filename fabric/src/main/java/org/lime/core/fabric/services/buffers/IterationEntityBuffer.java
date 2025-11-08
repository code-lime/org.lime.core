package org.lime.core.fabric.services.buffers;

import net.minecraft.world.entity.Entity;
import org.lime.core.common.services.buffers.BaseEntityBufferSetup;
import org.lime.core.common.services.buffers.BaseIterationEntityBuffer;
import org.lime.core.fabric.utils.WorldLocation;

public class IterationEntityBuffer<T extends Entity>
        extends BaseIterationEntityBuffer<T, Entity, WorldLocation> {
    protected IterationEntityBuffer(EntityBufferStorage owner, BaseEntityBufferSetup<WorldLocation> setup, Class<T> tClass) {
        super(owner, setup, tClass);
    }
}
