package org.lime.core.common.services.buffers;

import org.jetbrains.annotations.NotNull;
import org.lime.core.common.utils.execute.Action1;

/**
 * Marker storage for entities which only exist on clients and are never added
 * to a server world. Inbound interaction packets need a separate platform
 * router because the server cannot resolve these entities through its world.
 */
public abstract class BasePacketEntityBufferStorage<Entity, Location>
        extends BaseEntityBufferStorage<Entity, Location> {
    @Override
    protected final void forEntities(@NotNull Action1<Entity> entityLoad) {
    }
}
