package org.lime.core.common.services.buffers;

import org.jetbrains.annotations.ApiStatus;

/**
 * Platform-neutral view of a packet entity for one viewer.
 *
 * <p>The property/update split is intentional: both types are platform-owned
 * and their generic methods would otherwise have the same erased signature.</p>
 */
@ApiStatus.Internal
public interface PacketEntityViewSource<Viewer, Property, Update, Editor> {
    PacketEntityVisibility visibility();

    boolean hasListeners();

    boolean isTriggeredProperty(Property trigger);

    boolean isTriggeredUpdate(Update update);

    void edit(Viewer viewer, Editor editor);
}
