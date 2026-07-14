package org.lime.core.common.services.buffers;

/**
 * Marker storage for entities which only exist on clients and are never added
 * to a server world. Inbound interaction packets need a separate platform
 * router because the server cannot resolve these entities through its world.
 */
public abstract class BasePacketEntityBufferStorage<Entity, Location>
        extends BaseEntityBufferStorage<Entity, Location> {
}
