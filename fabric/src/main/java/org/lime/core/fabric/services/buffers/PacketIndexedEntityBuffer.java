package org.lime.core.fabric.services.buffers;

import com.google.inject.TypeLiteral;
import net.minecraft.world.entity.Entity;
import org.lime.core.common.services.buffers.BaseEntityBufferSetup;
import org.lime.core.fabric.utils.WorldLocation;

public class PacketIndexedEntityBuffer<Index, T extends Entity>
        extends IndexedEntityBuffer<Index, T> {
    protected PacketIndexedEntityBuffer(
            PacketEntityBufferStorage owner,
            BaseEntityBufferSetup<WorldLocation> setup,
            TypeLiteral<Index> indexClass,
            Class<T> tClass) {
        super(owner, setup, indexClass, tClass);
    }
}
