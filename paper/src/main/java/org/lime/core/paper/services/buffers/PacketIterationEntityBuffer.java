package org.lime.core.paper.services.buffers;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.lime.core.common.services.buffers.BaseEntityBufferSetup;

public class PacketIterationEntityBuffer<T extends Entity>
        extends IterationEntityBuffer<T> {
    protected PacketIterationEntityBuffer(
            PacketEntityBufferStorage owner,
            BaseEntityBufferSetup<Location> setup,
            Class<T> tClass) {
        super(owner, setup, tClass);
    }
}
