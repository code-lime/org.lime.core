package org.lime.core.common.services.buffers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.execute.Action1;
import org.lime.core.common.utils.execute.Action2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public abstract class BaseEntityBuffer<Index, T extends Entity, Entity, Location>
        implements Disposable {
    private boolean spawning;
    private boolean ending;
    private boolean closed;

    protected final Map<Index, T> displayBuffer = new HashMap<>();
    protected final Map<T, Index> entityIndexes = new IdentityHashMap<>();
    protected final BaseEntityBufferStorage<Entity, Location> owner;
    protected final String tag;
    protected final Class<T> tClass;
    protected final BaseEntityBufferSetup<Location> setup;
    private final Disposable registration;

    protected final Map<UUID, Action2<Index, T>> setupActions = new LinkedHashMap<>();

    protected BaseEntityBuffer(BaseEntityBufferStorage<Entity, Location> owner, BaseEntityBufferSetup<Location> setup, Class<T> tClass) {
        this.owner = owner;
        this.tag = "generic-" + setup.tag();
        this.tClass = tClass;
        this.setup = setup;
        this.registration = owner.registerBuffer(this);
        boolean completed = false;
        try {
            owner.forEntities(this::entityLoad);
            completed = true;
        } finally {
            if (!completed)
                registration.close();
        }
    }

    private Location orDefault(@Nullable Location location) {
        return location == null ? setup.defaultLocation().orElseGet(owner::defaultLocation) : location;
    }

    private @NotNull T spawnEntity(Index index, @Nullable Location location) {
        location = orDefault(location);
        AtomicReference<@Nullable T> boundEntity = new AtomicReference<>();
        boolean completed = false;
        spawning = true;
        try {
            T entity = owner.spawn(location, tClass, setup.entityKey().orElse(null), v -> {
                boundEntity.set(v);
                owner.getTags(v).add(this.tag);
                bind(index, v);
                for (Action2<Index, T> action : setupActions.values())
                    action.invoke(index, v);
            });
            completed = true;
            return entity;
        } finally {
            try {
                if (!completed) {
                    @Nullable T entity = boundEntity.get();
                    if (entity != null) {
                        unbind(index, entity);
                        owner.remove(entity);
                    }
                }
            } finally {
                spawning = false;
            }
        }
    }

    private void bind(Index index, T entity) {
        displayBuffer.put(index, entity);
        entityIndexes.put(entity, index);
    }

    private void unbind(Index index, T entity) {
        displayBuffer.remove(index);
        entityIndexes.remove(entity);
    }

    protected @Nullable Set<Index> usedIndexes = null;
    public void beginBuffer() {
        checkLifecycle();
        if (usedIndexes != null)
            throw new IllegalArgumentException("Buffer "+this.tag+" already begin");
        usedIndexes = new HashSet<>();
    }
    protected T indexedNextBuffer(Index index) {
        return indexedNextBuffer(index, null);
    }
    protected T indexedNextBuffer(Index index, @Nullable Location location) {
        return indexedNextBuffer(index, location, false);
    }
    protected T indexedNextBuffer(Index index, @Nullable Location location, boolean worldOnly) {
        checkLifecycle();
        if (usedIndexes == null)
            throw new IllegalArgumentException("Buffer "+this.tag+" not begin");
        if (!usedIndexes.add(index))
            throw new IllegalArgumentException("Duplicate index "+index+" in buffer "+this.tag);

        T entity = displayBuffer.get(index);
        if (entity == null) {
            entity = spawnEntity(index, location);
        } else {
            if (owner.isValid(entity)) {
                if (location != null && !owner.isEquals(owner.getLocation(entity), location, worldOnly))
                    owner.teleport(entity, location);
            } else {
                unbind(index, entity);
                spawning = true;
                try {
                    owner.remove(entity);
                } finally {
                    spawning = false;
                }
                entity = spawnEntity(index, location);
            }
        }

        return entity;
    }
    protected Disposable listenSetup(Action2<Index, T> setupAction) {
        UUID id = UUID.randomUUID();
        setupActions.put(id, setupAction);
        return () -> setupActions.remove(id);
    }
    public Disposable listenSetup(Action1<T> setupAction) {
        return listenSetup((v,e) -> setupAction.invoke(e));
    }
    public void endBuffer() {
        checkLifecycle();
        if (usedIndexes == null)
            throw new IllegalArgumentException("Buffer "+this.tag+" not begin");
        Set<Index> used = usedIndexes;
        ending = true;
        try {
            var iterator = displayBuffer.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Index, T> entry = iterator.next();
                if (used.contains(entry.getKey()))
                    continue;
                iterator.remove();
                entityIndexes.remove(entry.getValue());
                owner.remove(entry.getValue());
            }
        } finally {
            usedIndexes = null;
            ending = false;
        }
    }

    public Disposable use() {
        beginBuffer();
        return this::endBuffer;
    }

    void entityLoad(Entity entity) {
        if (!tClass.isInstance(entity))
            return;
        if (!owner.getTags(entity).contains(this.tag))
            return;
        if (entityIndexes.containsKey(tClass.cast(entity)))
            return;
        owner.remove(entity);
    }
    boolean hasEntity(Entity entity) {
        if (!tClass.isInstance(entity))
            return false;
        if (!owner.getTags(entity).contains(this.tag))
            return false;
        return entityIndexes.containsKey(tClass.cast(entity));
    }

    protected final @Nullable Index indexOf(T entity) {
        return entityIndexes.get(entity);
    }

    @Override
    public void close() {
        if (closed)
            return;
        if (spawning || ending)
            throw new IllegalStateException("Buffer " + tag + " is changing");
        closed = true;
        registration.close();
        usedIndexes = null;
        setupActions.clear();

        while (!displayBuffer.isEmpty()) {
            var iterator = displayBuffer.values().iterator();
            T entity = iterator.next();
            iterator.remove();
            entityIndexes.remove(entity);
            owner.remove(entity);
        }
        entityIndexes.clear();
    }

    private void checkLifecycle() {
        if (closed)
            throw new IllegalStateException("Buffer " + tag + " closed");
        if (spawning || ending)
            throw new IllegalStateException("Buffer " + tag + " is changing");
    }
}
