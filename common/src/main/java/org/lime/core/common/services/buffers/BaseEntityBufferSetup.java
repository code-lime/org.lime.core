package org.lime.core.common.services.buffers;

import net.kyori.adventure.key.Key;

import java.util.Optional;
import java.util.OptionalInt;

public interface BaseEntityBufferSetup<Location> {
    String tag();
    Optional<Key> entityKey();
    Optional<Location> defaultLocation();
    OptionalInt trackingDistance();
}
