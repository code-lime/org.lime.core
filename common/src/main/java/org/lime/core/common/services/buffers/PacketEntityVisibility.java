package org.lime.core.common.services.buffers;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable viewer set for a packet-only entity.
 *
 * <p>{@code exceptions} contains UUIDs whose visibility is the opposite of
 * {@code defaultVisible}. This represents both small allowlists and small
 * denylists without allocating a boolean entry for every online player.</p>
 */
public record PacketEntityVisibility(boolean defaultVisible, @NotNull Set<UUID> exceptions) {
    private static final PacketEntityVisibility ALL = new PacketEntityVisibility(true, Set.of());
    private static final PacketEntityVisibility NONE = new PacketEntityVisibility(false, Set.of());

    public PacketEntityVisibility {
        exceptions = Set.copyOf(exceptions);
    }

    public static @NotNull PacketEntityVisibility all() {
        return ALL;
    }

    public static @NotNull PacketEntityVisibility none() {
        return NONE;
    }

    public static @NotNull PacketEntityVisibility only(@NotNull UUID... viewers) {
        return only(Arrays.asList(viewers));
    }

    public static @NotNull PacketEntityVisibility only(@NotNull Collection<UUID> viewers) {
        return viewers.isEmpty()
                ? NONE
                : new PacketEntityVisibility(false, Set.copyOf(viewers));
    }

    public static @NotNull PacketEntityVisibility allExcept(@NotNull UUID... viewers) {
        return allExcept(Arrays.asList(viewers));
    }

    public static @NotNull PacketEntityVisibility allExcept(@NotNull Collection<UUID> viewers) {
        return viewers.isEmpty()
                ? ALL
                : new PacketEntityVisibility(true, Set.copyOf(viewers));
    }

    public boolean isVisible(@NotNull UUID viewer) {
        return defaultVisible != exceptions.contains(viewer);
    }
}
