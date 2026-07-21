package org.lime.core.fabric.services.buffers;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.lime.core.common.services.buffers.BaseEntityBufferSetup;
import org.lime.core.common.services.buffers.PacketEntityBufferState;
import org.lime.core.common.services.buffers.PacketEntityInteraction;
import org.lime.core.common.services.buffers.PacketEntityVisibility;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.execute.Action1;
import org.lime.core.common.utils.execute.Action3;
import org.lime.core.common.utils.execute.Action4;
import org.lime.core.fabric.utils.WorldLocation;

public class PacketIterationEntityBuffer<T extends Entity>
        extends IterationEntityBuffer<T> {
    private final PacketEntityBufferState<
            Integer,
            T,
            ServerPlayer,
            PacketEntityDataEditor.PropertyAccess<?>,
            SynchedEntityData.DataValue<?>,
            PacketEntityDataEditor,
            Packet<?>> packetState;

    protected PacketIterationEntityBuffer(@NotNull PacketEntityBufferStorage owner, @NotNull BaseEntityBufferSetup<WorldLocation> setup, @NotNull Class<T> tClass) {
        super(owner, setup, tClass);
        this.packetState = owner.packetState(displayBuffer, new Int2ObjectOpenHashMap<>());
        listenSetup(packetState::attach);
    }

    public void setDefaultVisibility(@NotNull PacketEntityVisibility visibility) {
        packetState.setDefaultVisibility(visibility);
    }

    public void setVisibility(@NotNull T entity, @NotNull PacketEntityVisibility visibility) {
        packetState.setVisibility(entity, this::indexOf, visibility);
    }

    public void clearVisibility(@NotNull T entity) {
        packetState.clearVisibility(entity, this::indexOf);
    }

    /**
     * Registers a setup-time view listener. Future pairing and matching metadata
     * updates run listeners in registration order and share one callback-scoped editor.
     * Disposing the listener does not refresh already paired players. Listener
     * registration must remain unchanged while callbacks are running.
     */
    public @NotNull Disposable listenView(@NotNull PacketEntityDataEditor.PropertyAccess<?> trigger, @NotNull Action3<T, ServerPlayer, PacketEntityDataEditor> listener) {
        return packetState.listenView(trigger, listener);
    }

    /**
     * Listens for client interaction packets addressed to a packet entity.
     * The client is fully trusted; the listener must validate visibility, distance and
     * interaction eligibility when required. Register listeners during setup and do not
     * change their registration while callbacks are running.
     */
    public @NotNull Disposable listenInteract(@NotNull Action3<T, ServerPlayer, PacketEntityInteraction> listener) {
        return packetState.listenInteract(listener);
    }

    /**
     * Registers a setup-time listener for future tracking transitions. The packet
     * sink is valid only during the callback. Listener registration must not change
     * while callbacks are being dispatched.
     */
    public @NotNull Disposable listenTracking(@NotNull Action4<T, ServerPlayer, Boolean, Action1<Packet<?>>> listener) {
        return packetState.listenTracking(listener);
    }

    @Override
    public void beginBuffer() {
        packetState.begin(super::beginBuffer);
    }

    @Override
    public void endBuffer() {
        packetState.end(super::endBuffer);
    }

    @Override
    public void close() {
        packetState.close(super::close);
    }
}
