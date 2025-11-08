package org.lime.core.common.services.buffers;

import java.util.Optional;
import java.util.OptionalInt;

public interface BaseEntityBufferSetup<Location> {
    String tag();
    Optional<Location> defaultLocation();
    OptionalInt trackingDistance();
}
