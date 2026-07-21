package org.lime.core.common.services.buffers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.utils.Disposable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/** Server-thread-confined registry and lifecycle for packet entities. */
public final class PacketEntityStorage<Entity, Attachment, Cache,
        EntityTracker extends PacketEntityStorage.Tracker<Cache>> implements Disposable {
    private record Entry<Entity, Value>(@NotNull Entity entity, @NotNull Value value) {}

    private final Runnable accessCheck;
    private final ToIntFunction<? super Entity> entityId;
    private final BiFunction<? super Entity, ? super Attachment, ? extends EntityTracker>
            trackerFactory;
    private final Consumer<? super Entity> discardEntity;
    private final Supplier<? extends Cache> cacheFactory;
    private final Map<Integer, Entry<Entity, Attachment>> pending = new HashMap<>();
    private final Map<Integer, Entry<Entity, EntityTracker>> trackers = new HashMap<>();
    private boolean closed;

    public PacketEntityStorage(
            @NotNull Runnable accessCheck,
            @NotNull ToIntFunction<? super Entity> entityId,
            @NotNull BiFunction<
                    ? super Entity,
                    ? super Attachment,
                    ? extends EntityTracker> trackerFactory,
            @NotNull Consumer<? super Entity> discardEntity,
            @NotNull Supplier<? extends Cache> cacheFactory) {
        this.accessCheck = accessCheck;
        this.entityId = entityId;
        this.trackerFactory = trackerFactory;
        this.discardEntity = discardEntity;
        this.cacheFactory = cacheFactory;
    }

    public void attach(@NotNull Entity entity, @NotNull Attachment attachment) {
        checkOpen();
        int id = entityId.applyAsInt(entity);
        if (pending.putIfAbsent(id, new Entry<>(entity, attachment)) != null)
            throw duplicate("attachment", id);
    }

    /** Registers the tracker created by an entity spawn and cleans up atomically on failure. */
    public <Type extends Entity> @NotNull Type registerSpawn(@NotNull Type entity, @NotNull Runnable initializer) {
        checkOpen();
        int id = entityId.applyAsInt(entity);
        @Nullable EntityTracker tracker = null;
        boolean registered = false;
        try {
            initializer.run();
            Attachment attachment = takeAttachment(id, entity);
            if (attachment == null)
                throw new IllegalStateException("Packet entity context not attached");
            tracker = trackerFactory.apply(entity, attachment);
            if (trackers.putIfAbsent(id, new Entry<>(entity, tracker)) != null)
                throw duplicate("tracker", id);
            registered = true;
            return entity;
        } finally {
            if (!registered) {
                cancelAttachment(id, entity);
                try {
                    if (tracker != null)
                        tracker.close();
                } finally {
                    discardEntity.accept(entity);
                }
            }
        }
    }

    public @Nullable EntityTracker tracker(int id) {
        accessCheck.run();
        Entry<Entity, EntityTracker> entry = closed ? null : trackers.get(id);
        return entry == null ? null : entry.value();
    }

    public @Nullable EntityTracker tracker(@NotNull Entity entity) {
        accessCheck.run();
        if (closed)
            return null;
        Entry<Entity, EntityTracker> entry = trackers.get(entityId.applyAsInt(entity));
        return entry != null && entry.entity() == entity ? entry.value() : null;
    }

    /** Synchronizes the supplied entities using one fresh cache for the complete pass. */
    public void synchronize(@NotNull Iterable<? extends Entity> entities) {
        checkAccess();
        if (closed)
            return;

        Cache cache = cacheFactory.get();
        for (Entity entity : entities) {
            Entry<Entity, EntityTracker> entry = trackers.get(entityId.applyAsInt(entity));
            if (entry != null && entry.entity() == entity)
                entry.value().synchronize(cache);
        }
    }

    public void remove(@NotNull Entity entity) {
        checkAccess();
        if (closed)
            return;
        int id = entityId.applyAsInt(entity);
        cancelAttachment(id, entity);
        Entry<Entity, EntityTracker> entry = trackers.get(id);
        if (entry == null || entry.entity() != entity || !trackers.remove(id, entry))
            return;
        entry.value().close();
    }

    /** Detaches a self-closing tracker without invoking close recursively. */
    public void detach(@NotNull Entity entity, @NotNull EntityTracker tracker) {
        checkAccess();
        if (closed)
            return;
        int id = entityId.applyAsInt(entity);
        Entry<Entity, EntityTracker> entry = trackers.get(id);
        if (entry != null && entry.entity() == entity && entry.value() == tracker)
            trackers.remove(id, entry);
    }

    @Override
    public void close() {
        checkAccess();
        if (closed)
            return;
        closed = true;
        pending.clear();

        while (!trackers.isEmpty()) {
            var iterator = trackers.values().iterator();
            EntityTracker tracker = iterator.next().value();
            iterator.remove();
            tracker.close();
        }
    }

    private @Nullable Attachment takeAttachment(int id, @NotNull Entity entity) {
        Entry<Entity, Attachment> entry = pending.get(id);
        return entry != null && entry.entity() == entity && pending.remove(id, entry)
                ? entry.value()
                : null;
    }

    private void cancelAttachment(int id, @NotNull Entity entity) {
        Entry<Entity, Attachment> entry = pending.get(id);
        if (entry != null && entry.entity() == entity)
            pending.remove(id, entry);
    }

    private void checkAccess() {
        accessCheck.run();
    }

    private void checkOpen() {
        checkAccess();
        if (closed)
            throw new IllegalStateException("Packet entity storage is closed");
    }

    private static @NotNull IllegalStateException duplicate(@NotNull String value, int entityId) {
        return new IllegalStateException("Duplicate packet entity " + value + " for id " + entityId);
    }

    public interface Tracker<Cache> extends Disposable {
        void synchronize(@NotNull Cache cache);
    }
}
