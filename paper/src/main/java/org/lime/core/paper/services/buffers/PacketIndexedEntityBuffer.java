package org.lime.core.paper.services.buffers;

import com.google.inject.TypeLiteral;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.lime.core.common.services.buffers.BaseEntityBufferSetup;
import org.lime.core.common.services.buffers.PacketEntityViewState;
import org.lime.core.common.services.buffers.PacketEntityVisibility;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.execute.Action4;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PacketIndexedEntityBuffer<Index, T extends Entity>
        extends IndexedEntityBuffer<Index, T> {
    private final PacketEntityBufferStorage packetOwner;
    private final PacketEntityViewState<
            Index,
            T,
            Player,
            EntityDataAccessor<?>,
            SynchedEntityData.DataValue<?>,
            PacketEntityDataEditor> viewState;

    protected PacketIndexedEntityBuffer(
            PacketEntityBufferStorage owner,
            BaseEntityBufferSetup<Location> setup,
            TypeLiteral<Index> indexClass,
            Class<T> tClass) {
        super(owner, setup, indexClass, tClass);
        this.packetOwner = owner;
        this.viewState = new PacketEntityViewState<>(
                new Object2ObjectOpenHashMap<>(),
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

    public void setVisibility(Index index, PacketEntityVisibility visibility) {
        packetOwner.requireServerThread();
        Objects.requireNonNull(index, "index");
        Objects.requireNonNull(visibility, "visibility");
        T entity = requireEntity(index);
        if (viewState.setVisibility(index, visibility))
            packetOwner.refreshTracking(List.of(entity));
    }

    public void clearVisibility(Index index) {
        packetOwner.requireServerThread();
        Objects.requireNonNull(index, "index");
        T entity = requireEntity(index);
        if (viewState.clearVisibility(index))
            packetOwner.refreshTracking(List.of(entity));
    }

    /**
     * Recomputes a per-player metadata overlay when {@code trigger} changes.
     * Listeners run in registration order and share one editor.
     */
    public Disposable listenView(
            EntityDataAccessor<?> trigger,
            Action4<Index, T, Player, PacketEntityDataEditor> listener) {
        return viewState.listen(trigger, listener);
    }

    /** Recomputes all visible entities whose listeners use {@code trigger}. */
    public void refreshView(EntityDataAccessor<?> trigger) {
        packetOwner.requireServerThread();
        Objects.requireNonNull(trigger, "trigger");
        packetOwner.refreshViews(displayBuffer.values(), trigger, null);
    }

    /** Recomputes one player's view for listeners using {@code trigger}. */
    public void refreshView(EntityDataAccessor<?> trigger, Player player) {
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

    private T requireEntity(Index index) {
        T entity = displayBuffer.get(index);
        if (entity == null)
            throw new IllegalArgumentException("Unknown packet entity index " + index);
        return entity;
    }
}
