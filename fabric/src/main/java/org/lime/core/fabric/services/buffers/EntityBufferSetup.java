package org.lime.core.fabric.services.buffers;

import org.jetbrains.annotations.Nullable;
import org.lime.core.common.services.buffers.BaseEntityBufferSetup;
import org.lime.core.fabric.utils.WorldLocation;

import java.util.Optional;
import java.util.OptionalInt;

public record EntityBufferSetup(
        String tag,
        Optional<WorldLocation> defaultLocation,
        OptionalInt trackingDistance)
        implements BaseEntityBufferSetup<WorldLocation> {
    public EntityBufferSetup tag(String tag) {
        return new EntityBufferSetup(tag, defaultLocation, trackingDistance);
    }
    public EntityBufferSetup defaultLocation(@Nullable WorldLocation defaultLocation) {
        return new EntityBufferSetup(tag, Optional.ofNullable(defaultLocation), trackingDistance);
    }
    public EntityBufferSetup trackingDistance(int trackingDistance) {
        return new EntityBufferSetup(tag, defaultLocation, OptionalInt.of(trackingDistance));
    }

    public static EntityBufferSetup of(String tag) {
        return new EntityBufferSetup(tag, Optional.empty(), OptionalInt.empty());
    }
    public static EntityBufferSetup of(String tag, @Nullable WorldLocation defaultLocation) {
        return new EntityBufferSetup(tag, Optional.ofNullable(defaultLocation), OptionalInt.empty());
    }
    public static EntityBufferSetup of(String tag, @Nullable WorldLocation defaultLocation, int trackingDistance) {
        return new EntityBufferSetup(tag, Optional.ofNullable(defaultLocation), OptionalInt.of(trackingDistance));
    }
}
