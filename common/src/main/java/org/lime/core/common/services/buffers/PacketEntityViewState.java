package org.lime.core.common.services.buffers;

import org.jetbrains.annotations.ApiStatus;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.execute.Action4;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Shared listener and visibility state for a packet entity buffer.
 *
 * <p>Platforms provide the visibility map so they can use specialized
 * collections without adding those libraries to the common module. Listener
 * reads are lock-free through an immutable snapshot; mutations are serialized
 * and publish a new snapshot before invalidating current views.</p>
 *
 * <p>The supplied access check defines the platform thread contract. Mutations
 * validate it, while hot-path source reads assume they already run on that
 * thread; this class is not a general-purpose concurrently readable map.</p>
 */
@ApiStatus.Internal
public final class PacketEntityViewState<Key, Entity, Viewer, Property, Update, Editor>
        implements Disposable {
    private final Object listenerLock = new Object();
    private final Object visibilityLock = new Object();
    private final ArrayList<ViewListener<Key, Entity, Viewer, Property, Editor>> listeners =
            new ArrayList<>();
    private final Map<Key, PacketEntityVisibility> visibilityOverrides;
    private final Runnable accessCheck;
    private final Runnable listenersChanged;
    private final BiPredicate<Property, Property> propertyMatcher;
    private final BiPredicate<Property, Update> updateMatcher;

    private volatile List<ViewListener<Key, Entity, Viewer, Property, Editor>> listenerSnapshot =
            List.of();
    private volatile PacketEntityVisibility defaultVisibility = PacketEntityVisibility.all();

    public PacketEntityViewState(
            Map<Key, PacketEntityVisibility> visibilityOverrides,
            Runnable accessCheck,
            Runnable listenersChanged,
            BiPredicate<Property, Property> propertyMatcher,
            BiPredicate<Property, Update> updateMatcher) {
        this.visibilityOverrides = Objects.requireNonNull(visibilityOverrides, "visibilityOverrides");
        this.accessCheck = Objects.requireNonNull(accessCheck, "accessCheck");
        this.listenersChanged = Objects.requireNonNull(listenersChanged, "listenersChanged");
        this.propertyMatcher = Objects.requireNonNull(propertyMatcher, "propertyMatcher");
        this.updateMatcher = Objects.requireNonNull(updateMatcher, "updateMatcher");
    }

    public Disposable listen(
            Property trigger,
            Action4<Key, Entity, Viewer, Editor> action) {
        accessCheck.run();
        Objects.requireNonNull(trigger, "trigger");
        Objects.requireNonNull(action, "action");

        ViewListener<Key, Entity, Viewer, Property, Editor> listener =
                new ViewListener<>(trigger, action);
        synchronized (listenerLock) {
            listeners.add(listener);
            listenerSnapshot = List.copyOf(listeners);
        }
        listenersChanged.run();

        AtomicBoolean disposed = new AtomicBoolean();
        return () -> {
            accessCheck.run();
            if (!disposed.compareAndSet(false, true))
                return;

            boolean removed;
            synchronized (listenerLock) {
                removed = listeners.remove(listener);
                if (removed)
                    listenerSnapshot = List.copyOf(listeners);
            }
            if (removed)
                listenersChanged.run();
        };
    }

    public boolean hasListeners() {
        return !listenerSnapshot.isEmpty();
    }

    public boolean isTriggeredProperty(Property trigger) {
        Objects.requireNonNull(trigger, "trigger");
        List<ViewListener<Key, Entity, Viewer, Property, Editor>> snapshot = listenerSnapshot;
        for (ViewListener<Key, Entity, Viewer, Property, Editor> listener : snapshot) {
            if (propertyMatcher.test(listener.trigger, trigger))
                return true;
        }
        return false;
    }

    public boolean isTriggeredUpdate(Update update) {
        Objects.requireNonNull(update, "update");
        List<ViewListener<Key, Entity, Viewer, Property, Editor>> snapshot = listenerSnapshot;
        for (ViewListener<Key, Entity, Viewer, Property, Editor> listener : snapshot) {
            if (updateMatcher.test(listener.trigger, update))
                return true;
        }
        return false;
    }

    public void edit(Key key, Entity entity, Viewer viewer, Editor editor) {
        List<ViewListener<Key, Entity, Viewer, Property, Editor>> snapshot = listenerSnapshot;
        for (ViewListener<Key, Entity, Viewer, Property, Editor> listener : snapshot)
            listener.action.invoke(key, entity, viewer, editor);
    }

    public PacketEntityVisibility visibility(Key key) {
        Objects.requireNonNull(key, "key");
        PacketEntityVisibility visibility = visibilityOverrides.get(key);
        return visibility == null ? defaultVisibility : visibility;
    }

    public boolean hasVisibilityOverride(Key key) {
        Objects.requireNonNull(key, "key");
        return visibilityOverrides.containsKey(key);
    }

    public boolean setDefaultVisibility(PacketEntityVisibility visibility) {
        accessCheck.run();
        Objects.requireNonNull(visibility, "visibility");
        synchronized (visibilityLock) {
            if (defaultVisibility.equals(visibility))
                return false;
            defaultVisibility = visibility;
            return true;
        }
    }

    public boolean setVisibility(Key key, PacketEntityVisibility visibility) {
        accessCheck.run();
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(visibility, "visibility");
        synchronized (visibilityLock) {
            PacketEntityVisibility previous = visibility(key);
            if (visibility.equals(defaultVisibility))
                visibilityOverrides.remove(key);
            else
                visibilityOverrides.put(key, visibility);
            return !previous.equals(visibility(key));
        }
    }

    public boolean clearVisibility(Key key) {
        accessCheck.run();
        Objects.requireNonNull(key, "key");
        synchronized (visibilityLock) {
            return visibilityOverrides.remove(key) != null;
        }
    }

    public void retainVisibilityKeys(Predicate<? super Key> keep) {
        accessCheck.run();
        Objects.requireNonNull(keep, "keep");
        synchronized (visibilityLock) {
            visibilityOverrides.keySet().removeIf(key -> !keep.test(key));
        }
    }

    public PacketEntityViewSource<Viewer, Property, Update, Editor> source(Key key, Entity entity) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(entity, "entity");
        return new PacketEntityViewSource<>() {
            @Override
            public PacketEntityVisibility visibility() {
                return PacketEntityViewState.this.visibility(key);
            }

            @Override
            public boolean hasListeners() {
                return PacketEntityViewState.this.hasListeners();
            }

            @Override
            public boolean isTriggeredProperty(Property trigger) {
                return PacketEntityViewState.this.isTriggeredProperty(trigger);
            }

            @Override
            public boolean isTriggeredUpdate(Update update) {
                return PacketEntityViewState.this.isTriggeredUpdate(update);
            }

            @Override
            public void edit(Viewer viewer, Editor editor) {
                PacketEntityViewState.this.edit(key, entity, viewer, editor);
            }
        };
    }

    @Override
    public void close() {
        accessCheck.run();
        synchronized (listenerLock) {
            listeners.clear();
            listenerSnapshot = List.of();
        }
        synchronized (visibilityLock) {
            visibilityOverrides.clear();
        }
    }

    private static final class ViewListener<Key, Entity, Viewer, Property, Editor> {
        private final Property trigger;
        private final Action4<Key, Entity, Viewer, Editor> action;

        private ViewListener(
                Property trigger,
                Action4<Key, Entity, Viewer, Editor> action) {
            this.trigger = trigger;
            this.action = action;
        }
    }
}
