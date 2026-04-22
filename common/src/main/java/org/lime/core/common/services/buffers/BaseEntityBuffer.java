package org.lime.core.common.services.buffers;

import org.jetbrains.annotations.Nullable;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.execute.Action1;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseEntityBuffer<Index, T extends Entity, Entity, Location>
        implements Disposable {
    private boolean closed = false;

    protected final Map<Index, T> displayBuffer = new ConcurrentHashMap<>();
    protected final BaseEntityBufferStorage<Entity, Location> owner;
    protected final String tag;
    protected final Class<T> tClass;
    protected final BaseEntityBufferSetup<Location> setup;

    protected final Map<UUID, Action1<T>> setupActions = new ConcurrentHashMap<>();

    protected BaseEntityBuffer(BaseEntityBufferStorage<Entity, Location> owner, BaseEntityBufferSetup<Location> setup, Class<T> tClass) {
        this.owner = owner;
        this.tag = "generic-" + setup.tag();
        this.tClass = tClass;
        this.setup = setup;
        owner.buffers.add(this);
        owner.forEntities(this::entityLoad);
    }

    private Location orDefault(@Nullable Location location) {
        return location == null ? setup.defaultLocation().orElseGet(owner::defaultLocation) : location;
    }

    private T spawnEntity(Index index, @Nullable Location location) {
        location = orDefault(location);
        return owner.spawn(location, tClass, setup.entityKey().orElse(null), v -> {
            owner.getTags(v).add(this.tag);
            displayBuffer.put(index, v);
            setupActions.values().forEach(action -> action.invoke(v));
        });
    }

    protected @Nullable Set<Index> usedIndexes = null;
    public void beginBuffer() {
        if (closed)
            throw new IllegalArgumentException("Buffer "+this.tag+" closed");
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
        if (closed)
            throw new IllegalArgumentException("Buffer "+this.tag+" closed");
        if (usedIndexes == null)
            throw new IllegalArgumentException("Buffer "+this.tag+" not begin");
        if (!usedIndexes.add(index))
            throw new IllegalArgumentException("Duplicate index "+index+" in buffer "+this.tag);

        T entity = displayBuffer.get(index);
        if (entity == null) {
            entity = spawnEntity(index, location);
        } else {
            entity = displayBuffer.get(index);
            if (owner.isValid(entity)) {
                if (location != null && !owner.isEquals(owner.getLocation(entity), location, worldOnly))
                    owner.teleport(entity, location);
            } else {
                owner.remove(entity);
                entity = spawnEntity(index, location);
            }
        }

        return entity;
    }
    public void endBuffer() {
        if (closed)
            throw new IllegalArgumentException("Buffer "+this.tag+" closed");
        if (usedIndexes == null)
            throw new IllegalArgumentException("Buffer "+this.tag+" not begin");
        displayBuffer.entrySet().removeIf(kv -> {
            if (usedIndexes.contains(kv.getKey()))
                return false;
            owner.remove(kv.getValue());
            return true;
        });
        usedIndexes = null;
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
        if (displayBuffer.containsValue(tClass.cast(entity)))
            return;
        owner.remove(entity);
    }
    boolean hasEntity(Entity entity) {
        if (!tClass.isInstance(entity))
            return false;
        if (!owner.getTags(entity).contains(this.tag))
            return false;
        return displayBuffer.containsValue(tClass.cast(entity));
    }

    public Disposable listenSetup(Action1<T> setupAction) {
        UUID id = UUID.randomUUID();
        setupActions.put(id, setupAction);
        return () -> setupActions.remove(id);
    }

    @Override
    public void close() {
        closed = true;
        owner.buffers.remove(this);
        displayBuffer.values().removeIf(v -> {
            owner.remove(v);
            return true;
        });
        setupActions.clear();
    }
}
