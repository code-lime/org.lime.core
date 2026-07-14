package org.lime.core.fabric.services.buffers;

import net.minecraft.world.entity.Entity;
import org.lime.core.common.services.buffers.BaseEntityBufferSetup;
import org.lime.core.fabric.utils.WorldLocation;

public class PacketIterationEntityBuffer<T extends Entity>
        extends IterationEntityBuffer<T> {
    protected PacketIterationEntityBuffer(
            PacketEntityBufferStorage owner,
            BaseEntityBufferSetup<WorldLocation> setup,
            Class<T> tClass) {
        super(owner, setup, tClass);
    }
}
