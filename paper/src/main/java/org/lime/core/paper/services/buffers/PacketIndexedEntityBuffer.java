package org.lime.core.paper.services.buffers;

import com.google.inject.TypeLiteral;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.lime.core.common.services.buffers.BaseEntityBufferSetup;

public class PacketIndexedEntityBuffer<Index, T extends Entity>
        extends IndexedEntityBuffer<Index, T> {
    protected PacketIndexedEntityBuffer(
            PacketEntityBufferStorage owner,
            BaseEntityBufferSetup<Location> setup,
            TypeLiteral<Index> indexClass,
            Class<T> tClass) {
        super(owner, setup, indexClass, tClass);
    }
}
