package org.lime.core.paper.services.buffers;

import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.OptionalInt;

public record EntityBufferSetup(
        String tag,
        Optional<World> defaultWorld,
        OptionalInt trackingDistance) {
    public EntityBufferSetup tag(String tag) {
        return new EntityBufferSetup(tag, defaultWorld, trackingDistance);
    }
    public EntityBufferSetup defaultWorld(@Nullable World defaultWorld) {
        return new EntityBufferSetup(tag, Optional.ofNullable(defaultWorld), trackingDistance);
    }
    public EntityBufferSetup trackingDistance(int trackingDistance) {
        return new EntityBufferSetup(tag, defaultWorld, OptionalInt.of(trackingDistance));
    }

    public static EntityBufferSetup of(String tag) {
        return new EntityBufferSetup(tag, Optional.empty(), OptionalInt.empty());
    }
    public static EntityBufferSetup of(String tag, World defaultWorld) {
        return new EntityBufferSetup(tag, Optional.of(defaultWorld), OptionalInt.empty());
    }
    public static EntityBufferSetup of(String tag, World defaultWorld, int trackingDistance) {
        return new EntityBufferSetup(tag, Optional.of(defaultWorld), OptionalInt.of(trackingDistance));
    }
}
