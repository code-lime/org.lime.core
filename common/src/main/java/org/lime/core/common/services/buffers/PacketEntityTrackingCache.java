package org.lime.core.common.services.buffers;

import org.jetbrains.annotations.NotNull;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Per-pass identity-level cache of the immutable players tracking a chunk. */
public final class PacketEntityTrackingCache<Level, Chunk, Viewer> {
    private final Supplier<? extends @NotNull Map<Chunk, List<Viewer>>> mapFactory;
    private final IdentityHashMap<Level, Map<Chunk, List<Viewer>>> values =
            new IdentityHashMap<>();

    public PacketEntityTrackingCache(@NotNull Supplier<? extends @NotNull Map<Chunk, List<Viewer>>> mapFactory) {
        this.mapFactory = mapFactory;
    }

    public @NotNull List<Viewer> get(@NotNull Level level, @NotNull Chunk chunk, @NotNull Supplier<? extends @NotNull List<Viewer>> loader) {
        return values.computeIfAbsent(level, ignored -> mapFactory.get()).computeIfAbsent(chunk, ignored -> loader.get());
    }
}
