package org.lime.core.common.services.buffers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.execute.Action1;
import org.lime.core.common.utils.execute.Action3;
import org.lime.core.common.utils.execute.Action4;
import org.lime.core.common.utils.execute.Action5;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Function;

/** Shared API state and end-only lifecycle of one packet-entity buffer. */
public final class PacketEntityBufferState<Key, Entity, Viewer, Property, Update, Editor, Packet> {
    private record ViewListener<Key, Entity, Viewer, Property, Editor>(@NotNull Property trigger, @NotNull Action4<Key, Entity, Viewer, Editor> action) {}

    private final Backend<? super Entity, Viewer, Update, Editor, Packet> runtime;
    private final Map<Key, Entity> entities;
    private final Map<Key, PacketEntityVisibility> visibility;
    private final BiPredicate<Property, Update> updateMatcher;
    private final PacketEntityBatch<Viewer, Packet> batch;
    private final Map<Object, ViewListener<Key, Entity, Viewer, Property, Editor>> viewActions = new LinkedHashMap<>();
    private final Map<Object, Action4<Key, Entity, Viewer, PacketEntityInteraction>> interactionActions = new LinkedHashMap<>();
    private final Map<Object, Action5<Key, Entity, Viewer, Boolean, Action1<Packet>>> trackingActions = new LinkedHashMap<>();
    private PacketEntityVisibility defaultVisibility = PacketEntityVisibility.all();
    private boolean closed;

    public PacketEntityBufferState(@NotNull Backend<? super Entity, Viewer, Update, Editor, Packet> runtime, @NotNull Map<Key, Entity> entities, @NotNull Map<Key, PacketEntityVisibility> visibility, @NotNull BiPredicate<Property, Update> updateMatcher) {
        this.runtime = runtime;
        this.entities = entities;
        this.visibility = visibility;
        this.updateMatcher = updateMatcher;
        this.batch = runtime.createBatch();
    }

    public void attach(@NotNull Key key, @NotNull Entity entity) {
        checkOpen();
        runtime.attach(entity, new Source(key, entity), batch);
    }

    public void setDefaultVisibility(@NotNull PacketEntityVisibility visibility) {
        checkOpen();
        defaultVisibility = visibility;
    }

    public void setVisibility(@NotNull Key key, @NotNull PacketEntityVisibility visibility) {
        checkOpen();
        entity(key);
        if (visibility.equals(defaultVisibility))
            this.visibility.remove(key);
        else
            this.visibility.put(key, visibility);
    }

    public void setVisibility(@NotNull Entity entity, @NotNull Function<? super Entity, ? extends @Nullable Key> key, @NotNull PacketEntityVisibility visibility) {
        setVisibility(key(entity, key), visibility);
    }

    public void clearVisibility(@NotNull Key key) {
        checkOpen();
        entity(key);
        visibility.remove(key);
    }

    public void clearVisibility(@NotNull Entity entity, @NotNull Function<? super Entity, ? extends @Nullable Key> key) {
        clearVisibility(key(entity, key));
    }

    public @NotNull Disposable listenView(@NotNull Property trigger, @NotNull Action4<Key, Entity, Viewer, Editor> listener) {
        return listen(viewActions, new ViewListener<>(trigger, listener));
    }

    public @NotNull Disposable listenView(@NotNull Property trigger, @NotNull Action3<Entity, Viewer, Editor> listener) {
        return listenView(trigger, (key, entity, viewer, editor) -> listener.invoke(entity, viewer, editor));
    }

    public @NotNull Disposable listenInteract(@NotNull Action4<Key, Entity, Viewer, PacketEntityInteraction> listener) {
        return listen(interactionActions, listener);
    }

    public @NotNull Disposable listenInteract(@NotNull Action3<Entity, Viewer, PacketEntityInteraction> listener) {
        return listenInteract((key, entity, viewer, interaction) -> listener.invoke(entity, viewer, interaction));
    }

    public @NotNull Disposable listenTracking(@NotNull Action5<Key, Entity, Viewer, Boolean, Action1<Packet>> listener) {
        return listen(trackingActions, listener);
    }

    public @NotNull Disposable listenTracking(@NotNull Action4<Entity, Viewer, Boolean, Action1<Packet>> listener) {
        return listenTracking((key, entity, viewer, added, packets) -> listener.invoke(entity, viewer, added, packets));
    }

    public void begin(@NotNull Runnable begin) {
        checkOpen();
        batch.begin(begin);
    }

    public void end(@NotNull Runnable removeUnused) {
        checkOpen();
        batch.end(() -> {
            removeUnused.run();
            visibility.keySet().removeIf(key -> !entities.containsKey(key));
            runtime.synchronize(entities.values());
        });
    }

    public void close(@NotNull Runnable closeEntities) {
        runtime.checkAccess();
        if (closed)
            return;
        batch.close(() -> {
            closeEntities.run();
            closed = true;
            visibility.clear();
            viewActions.clear();
            interactionActions.clear();
            trackingActions.clear();
        });
    }

    private void checkOpen() {
        runtime.checkAccess();
        if (closed)
            throw new IllegalStateException("Packet entity buffer is closed");
    }

    private <Listener> @NotNull Disposable listen(@NotNull Map<Object, Listener> listeners, @NotNull Listener listener) {
        checkOpen();
        Object id = new Object();
        listeners.put(id, listener);
        return () -> {
            runtime.checkAccess();
            listeners.remove(id);
        };
    }

    private @NotNull Entity entity(@NotNull Key key) {
        Entity entity = entities.get(key);
        if (entity == null)
            throw new IllegalArgumentException("Unknown packet entity index " + key);
        return entity;
    }

    private @NotNull Key key(@NotNull Entity entity, @NotNull Function<? super Entity, ? extends @Nullable Key> lookup) {
        checkOpen();
        Key key = lookup.apply(entity);
        if (key == null)
            throw new IllegalArgumentException("Entity does not belong to this packet entity buffer");
        return key;
    }

    private final class Source implements EntitySource<Viewer, Update, Editor, Packet> {
        private final Key key;
        private final Entity entity;

        private Source(@NotNull Key key, @NotNull Entity entity) {
            this.key = key;
            this.entity = entity;
        }

        @Override
        public boolean isVisible(@NotNull UUID viewer) {
            return visibility.getOrDefault(key, defaultVisibility).isVisible(viewer);
        }

        @Override
        public boolean hasViewListeners() {
            return !viewActions.isEmpty();
        }

        @Override
        public boolean isTriggeredUpdate(@NotNull Update update) {
            for (ViewListener<Key, Entity, Viewer, Property, Editor> listener : viewActions.values()) {
                if (updateMatcher.test(listener.trigger(), update))
                    return true;
            }
            return false;
        }

        @Override
        public void edit(@NotNull Viewer viewer, @NotNull Editor editor) {
            for (ViewListener<Key, Entity, Viewer, Property, Editor> listener : viewActions.values())
                listener.action().invoke(key, entity, viewer, editor);
        }

        @Override
        public boolean hasInteractionListeners() {
            return !interactionActions.isEmpty();
        }

        @Override
        public void interact(@NotNull Viewer viewer, @NotNull PacketEntityInteraction interaction) {
            for (Action4<Key, Entity, Viewer, PacketEntityInteraction> listener : interactionActions.values())
                listener.invoke(key, entity, viewer, interaction);
        }

        @Override
        public void tracking(@NotNull Viewer viewer, boolean added) {
            if (trackingActions.isEmpty())
                return;
            Action1<Packet> sink = packet -> batch.send(viewer, packet);
            for (Action5<Key, Entity, Viewer, Boolean, Action1<Packet>> listener : trackingActions.values())
                listener.invoke(key, entity, viewer, added, sink);
        }
    }

    /** Platform operations required by one packet-entity buffer. */
    public interface Backend<Entity, Viewer, Update, Editor, Packet> {
        void checkAccess();

        @NotNull PacketEntityBatch<Viewer, Packet> createBatch();

        void attach(@NotNull Entity entity, @NotNull EntitySource<Viewer, Update, Editor, Packet> source, @NotNull PacketEntityBatch<Viewer, Packet> batch);

        void synchronize(@NotNull Iterable<? extends Entity> entities);
    }

    /** Per-entity visibility and callback source shared by the platform tracker. */
    public interface EntitySource<Viewer, Update, Editor, Packet> {
        boolean isVisible(@NotNull UUID viewer);

        boolean hasViewListeners();

        boolean isTriggeredUpdate(@NotNull Update update);

        void edit(@NotNull Viewer viewer, @NotNull Editor editor);

        boolean hasInteractionListeners();

        void interact(@NotNull Viewer viewer, @NotNull PacketEntityInteraction interaction);

        void tracking(@NotNull Viewer viewer, boolean added);
    }
}
