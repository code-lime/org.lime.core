package org.lime.core.fabric.hooks;

import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.services.buffers.PacketEntityInteraction;
import org.lime.core.common.utils.Disposable;

import java.util.concurrent.atomic.AtomicReference;

public final class PacketEntityInteractionHook {
    private static final AtomicReference<@Nullable Listener> LISTENER = new AtomicReference<>();

    private PacketEntityInteractionHook() {}

    public static @NotNull Disposable register(@NotNull Listener listener) {
        if (!LISTENER.compareAndSet(null, listener))
            throw new IllegalStateException("Packet entity interaction listener already registered");
        return () -> LISTENER.compareAndSet(listener, null);
    }

    public static boolean interact(@NotNull ServerPlayer player, int entityId, @NotNull ServerboundInteractPacket packet) {
        @Nullable Listener listener = LISTENER.get();
        return listener != null && listener.interact(player, entityId, packet);
    }

    public static @NotNull PacketEntityInteraction decode(@NotNull ServerboundInteractPacket packet) {
        PacketEntityInteraction[] result = new PacketEntityInteraction[1];
        boolean secondary = packet.isUsingSecondaryAction();
        packet.dispatch(new ServerboundInteractPacket.Handler() {
            @Override
            public void onInteraction(@NotNull InteractionHand hand) {
                result[0] = PacketEntityInteraction.interact(hand(hand), null, secondary);
            }

            @Override
            public void onInteraction(@NotNull InteractionHand hand, @NotNull Vec3 location) {
                result[0] = PacketEntityInteraction.interact(hand(hand), new PacketEntityInteraction.Position(location.x, location.y, location.z), secondary);
            }

            @Override
            public void onAttack() {
                result[0] = PacketEntityInteraction.attack(secondary);
            }
        });
        return result[0];
    }

    private static @NotNull PacketEntityInteraction.Hand hand(@NotNull InteractionHand hand) {
        return hand == InteractionHand.OFF_HAND
                ? PacketEntityInteraction.Hand.OFF_HAND
                : PacketEntityInteraction.Hand.MAIN_HAND;
    }

    @FunctionalInterface
    public interface Listener {
        boolean interact(@NotNull ServerPlayer player, int entityId, @NotNull ServerboundInteractPacket packet);
    }
}
