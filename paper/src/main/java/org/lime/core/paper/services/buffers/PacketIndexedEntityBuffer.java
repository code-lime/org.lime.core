package org.lime.core.paper.services.buffers;

import com.google.inject.TypeLiteral;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.lime.core.common.services.buffers.BaseEntityBufferSetup;
import org.lime.core.common.services.buffers.PacketEntityBufferState;
import org.lime.core.common.services.buffers.PacketEntityInteraction;
import org.lime.core.common.services.buffers.PacketEntityVisibility;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.execute.Action1;
import org.lime.core.common.utils.execute.Action4;
import org.lime.core.common.utils.execute.Action5;

public class PacketIndexedEntityBuffer<Index, T extends Entity>
        extends IndexedEntityBuffer<Index, T> {
    private final PacketEntityBufferState<
            Index,
            T,
            ServerPlayer,
            PacketEntityDataEditor.PropertyAccess<?>,
            SynchedEntityData.DataValue<?>,
            PacketEntityDataEditor,
            Packet<? super ClientGamePacketListener>> packetState;

    protected PacketIndexedEntityBuffer(@NotNull PacketEntityBufferStorage owner, @NotNull BaseEntityBufferSetup<Location> setup, @NotNull TypeLiteral<Index> indexClass, @NotNull Class<T> tClass) {
        super(owner, setup, indexClass, tClass);
        this.packetState = owner.packetState(displayBuffer, new Object2ObjectOpenHashMap<>());
        listenSetup(packetState::attach);
    }

    public void setDefaultVisibility(@NotNull PacketEntityVisibility visibility) {
        packetState.setDefaultVisibility(visibility);
    }

    public void setVisibility(@NotNull Index index, @NotNull PacketEntityVisibility visibility) {
        packetState.setVisibility(index, visibility);
    }

    public void clearVisibility(@NotNull Index index) {
        packetState.clearVisibility(index);
    }

    /**
     * Registers a setup-time view listener. Future pairing and matching metadata
     * updates run listeners in registration order and share one callback-scoped editor.
     * Disposing the listener does not refresh already paired players. Listener
     * registration must remain unchanged while callbacks are running.
     */
    public @NotNull Disposable listenView(@NotNull PacketEntityDataEditor.PropertyAccess<?> trigger, @NotNull Action4<Index, T, Player, PacketEntityDataEditor> listener) {
        return packetState.listenView(trigger, (index, entity, player, editor) -> listener.invoke(index, entity, player.getBukkitEntity(), editor));
    }

    /**
     * Receives every matching client interaction packet for a registered packet entity.
     * Visibility, world, distance and reach are not validated; the caller is responsible
     * for deciding whether the client packet should be trusted. Register listeners during
     * setup and do not change their registration while callbacks are running.
     */
    public @NotNull Disposable listenInteract(@NotNull Action4<Index, T, Player, PacketEntityInteraction> listener) {
        return packetState.listenInteract((index, entity, player, interaction) -> listener.invoke(index, entity, player.getBukkitEntity(), interaction));
    }

    /**
     * Registers a setup-time listener for future tracking transitions. The packet
     * sink is valid only during the callback. Listener registration must not change
     * while callbacks are being dispatched.
     */
    public @NotNull Disposable listenTracking(@NotNull Action5<Index, T, Player, Boolean, Action1<Packet<? super ClientGamePacketListener>>> listener) {
        return packetState.listenTracking((index, entity, player, added, packets) -> listener.invoke(index, entity, player.getBukkitEntity(), added, packets));
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
