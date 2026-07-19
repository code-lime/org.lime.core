package org.lime.core.fabric.services.buffers;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.lime.core.common.services.buffers.BaseEntityBufferSetup;
import org.lime.core.common.services.buffers.PacketEntityViewState;
import org.lime.core.common.services.buffers.PacketEntityVisibility;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.execute.Action3;
import org.lime.core.fabric.utils.WorldLocation;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PacketIterationEntityBuffer<T extends Entity>
        extends IterationEntityBuffer<T> {
    private final PacketEntityBufferStorage packetOwner;
    private final PacketEntityViewState<
            Integer,
            T,
            ServerPlayer,
            EntityDataAccessor<?>,
            SynchedEntityData.DataValue<?>,
            PacketEntityDataEditor> viewState;

    protected PacketIterationEntityBuffer(
            PacketEntityBufferStorage owner,
            BaseEntityBufferSetup<WorldLocation> setup,
            Class<T> tClass) {
        super(owner, setup, tClass);
        this.packetOwner = owner;
        this.viewState = new PacketEntityViewState<>(
                new Int2ObjectOpenHashMap<>(),
                owner::requireServerThread,
                this::refreshAll,
                PacketEntityDataEditor::matches,
                PacketEntityDataEditor::matches);
        listenSetup((index, entity) -> owner.attachView(entity, viewState.source(index, entity)));
    }

    public void setDefaultVisibility(PacketEntityVisibility visibility) {
        if (!viewState.setDefaultVisibility(visibility))
            return;
        packetOwner.refreshTracking(displayBuffer.entrySet().stream()
                .filter(entry -> !viewState.hasVisibilityOverride(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList());
    }

    public void setVisibility(T entity, PacketEntityVisibility visibility) {
        packetOwner.requireServerThread();
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(visibility, "visibility");
        Integer index = indexOf(entity);
        if (index == null)
            throw new IllegalArgumentException("Entity does not belong to this packet entity buffer");
        if (viewState.setVisibility(index, visibility))
            packetOwner.refreshTracking(List.of(entity));
    }

    public void clearVisibility(T entity) {
        packetOwner.requireServerThread();
        Objects.requireNonNull(entity, "entity");
        Integer index = indexOf(entity);
        if (index == null)
            throw new IllegalArgumentException("Entity does not belong to this packet entity buffer");
        if (viewState.clearVisibility(index))
            packetOwner.refreshTracking(List.of(entity));
    }

    /**
     * Recomputes a per-player metadata overlay when {@code trigger} changes.
     * Listeners run in registration order and share one editor.
     */
    public Disposable listenView(
            EntityDataAccessor<?> trigger,
            Action3<T, ServerPlayer, PacketEntityDataEditor> listener) {
        packetOwner.requireServerThread();
        Objects.requireNonNull(trigger, "trigger");
        Objects.requireNonNull(listener, "listener");
        return viewState.listen(
                trigger,
                (index, entity, player, editor) -> listener.invoke(entity, player, editor));
    }

    /** Recomputes all visible entities whose listeners use {@code trigger}. */
    public void refreshView(EntityDataAccessor<?> trigger) {
        packetOwner.requireServerThread();
        Objects.requireNonNull(trigger, "trigger");
        packetOwner.refreshViews(displayBuffer.values(), trigger, null);
    }

    /** Recomputes one player's view for listeners using {@code trigger}. */
    public void refreshView(EntityDataAccessor<?> trigger, ServerPlayer player) {
        packetOwner.requireServerThread();
        Objects.requireNonNull(trigger, "trigger");
        Objects.requireNonNull(player, "player");
        packetOwner.refreshViews(displayBuffer.values(), trigger, player);
    }

    @Override
    public void endBuffer() {
        packetOwner.requireServerThread();
        super.endBuffer();
        viewState.retainVisibilityKeys(displayBuffer::containsKey);
    }

    @Override
    public void close() {
        viewState.close();
        super.close();
    }

    private void refreshAll() {
        packetOwner.refreshViews(displayBuffer.values(), null, null);
    }
}
