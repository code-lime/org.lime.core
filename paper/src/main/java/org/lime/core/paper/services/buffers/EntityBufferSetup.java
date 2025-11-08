package org.lime.core.paper.services.buffers;

import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.services.buffers.BaseEntityBufferSetup;

import java.util.Optional;
import java.util.OptionalInt;

public record EntityBufferSetup(
        String tag,
        Optional<Location> defaultLocation,
        OptionalInt trackingDistance)
        implements BaseEntityBufferSetup<Location> {
    public EntityBufferSetup tag(String tag) {
        return new EntityBufferSetup(tag, defaultLocation, trackingDistance);
    }
    public EntityBufferSetup defaultLocation(@Nullable Location defaultLocation) {
        return new EntityBufferSetup(tag, Optional.ofNullable(defaultLocation), trackingDistance);
    }
    public EntityBufferSetup trackingDistance(int trackingDistance) {
        return new EntityBufferSetup(tag, defaultLocation, OptionalInt.of(trackingDistance));
    }

    public static EntityBufferSetup of(String tag) {
        return new EntityBufferSetup(tag, Optional.empty(), OptionalInt.empty());
    }
    public static EntityBufferSetup of(String tag, @Nullable Location defaultLocation) {
        return new EntityBufferSetup(tag, Optional.ofNullable(defaultLocation), OptionalInt.empty());
    }
    public static EntityBufferSetup of(String tag, @Nullable Location defaultLocation, int trackingDistance) {
        return new EntityBufferSetup(tag, Optional.ofNullable(defaultLocation), OptionalInt.of(trackingDistance));
    }
}
