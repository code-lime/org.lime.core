package org.lime.core.common.services.buffers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A client interaction with a packet-only entity.
 *
 * @param action interaction kind
 * @param hand hand used by the client, or {@code null} for an attack
 * @param position hit position relative to the entity, present only for
 *                 {@link Action#INTERACT_AT}
 * @param usingSecondaryAction whether the client was using its secondary
 *                             action (normally sneaking)
 */
public record PacketEntityInteraction(@NotNull Action action, @Nullable Hand hand, @Nullable Position position, boolean usingSecondaryAction) {
    public static @NotNull PacketEntityInteraction interact(@NotNull Hand hand, @Nullable Position position, boolean usingSecondaryAction) {
        return new PacketEntityInteraction(position == null ? Action.INTERACT : Action.INTERACT_AT, hand, position, usingSecondaryAction);
    }

    public static @NotNull PacketEntityInteraction attack(boolean usingSecondaryAction) {
        return new PacketEntityInteraction(Action.ATTACK, null, null, usingSecondaryAction);
    }

    public enum Action {
        INTERACT,
        INTERACT_AT,
        ATTACK
    }

    public enum Hand {
        MAIN_HAND,
        OFF_HAND
    }

    /** Hit position relative to the packet entity. */
    public record Position(double x, double y, double z) {}
}
