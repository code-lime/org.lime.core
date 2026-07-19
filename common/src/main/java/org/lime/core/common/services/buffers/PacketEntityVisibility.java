package org.lime.core.common.services.buffers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable viewer set for a packet-only entity.
 *
 * <p>{@code exceptions} contains UUIDs whose visibility is the opposite of
 * {@code defaultVisible}. This represents both small allowlists and small
 * denylists without allocating a boolean entry for every online player.</p>
 */
public record PacketEntityVisibility(
        boolean defaultVisible,
        Set<UUID> exceptions) {
    private static final PacketEntityVisibility ALL = new PacketEntityVisibility(true, Set.of());
    private static final PacketEntityVisibility NONE = new PacketEntityVisibility(false, Set.of());

    public PacketEntityVisibility {
        exceptions = Set.copyOf(Objects.requireNonNull(exceptions, "exceptions"));
    }

    public static PacketEntityVisibility all() {
        return ALL;
    }

    public static PacketEntityVisibility none() {
        return NONE;
    }

    public static PacketEntityVisibility only(UUID... viewers) {
        Objects.requireNonNull(viewers, "viewers");
        return only(Arrays.asList(viewers));
    }

    public static PacketEntityVisibility only(Collection<UUID> viewers) {
        Objects.requireNonNull(viewers, "viewers");
        return viewers.isEmpty()
                ? NONE
                : new PacketEntityVisibility(false, Set.copyOf(viewers));
    }

    public static PacketEntityVisibility allExcept(UUID... viewers) {
        Objects.requireNonNull(viewers, "viewers");
        return allExcept(Arrays.asList(viewers));
    }

    public static PacketEntityVisibility allExcept(Collection<UUID> viewers) {
        Objects.requireNonNull(viewers, "viewers");
        return viewers.isEmpty()
                ? ALL
                : new PacketEntityVisibility(true, Set.copyOf(viewers));
    }

    public boolean isVisible(UUID viewer) {
        Objects.requireNonNull(viewer, "viewer");
        return defaultVisible != exceptions.contains(viewer);
    }
}
