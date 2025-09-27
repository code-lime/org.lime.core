package org.lime.core.paper.services.buffers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.utils.Disposable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseEntityBuffer<Index, T extends Entity>
        implements Disposable {
    private boolean closed = false;

    protected final Map<Index, T> displayBuffer = new ConcurrentHashMap<>();
    protected final EntityBufferStorage owner;
    protected final String tag;
    protected final Class<T> tClass;
    protected final World defaultWorld;

    protected BaseEntityBuffer(EntityBufferStorage owner, String tag, Class<T> tClass, World defaultWorld) {
        this.owner = owner;
        this.tag = "generic-" + tag;
        this.tClass = tClass;
        this.defaultWorld = defaultWorld;
        owner.buffers.add(this);
        Bukkit.getWorlds()
                .forEach(world -> world.getEntities()
                        .forEach(this::entityLoad));
    }

    private Location orDefault(@Nullable Location location) {
        return location == null ? new Location(defaultWorld, 0, 0, 0) : location;
    }

    private T spawnEntity(@Nullable Location location) {
        location = orDefault(location);
        T entity = location.getWorld().spawn(location, tClass);
        entity.addScoreboardTag(this.tag);
        return entity;
    }

    protected @Nullable Set<Index> usedIndexes = null;
    public void beginBuffer() {
        if (closed)
            throw new IllegalArgumentException("Buffer "+this.tag+" closed");
        if (usedIndexes != null)
            throw new IllegalArgumentException("Buffer "+this.tag+" already begin");
        usedIndexes = new HashSet<>();
    }
    protected T nextBuffer(Index index) {
        return nextBuffer(index, null);
    }
    protected T nextBuffer(Index index, @Nullable Location location) {
        if (closed)
            throw new IllegalArgumentException("Buffer "+this.tag+" closed");
        if (usedIndexes == null)
            throw new IllegalArgumentException("Buffer "+this.tag+" not begin");
        if (!usedIndexes.add(index))
            throw new IllegalArgumentException("Duplicate index "+index+" in buffer "+this.tag);

        T entity = displayBuffer.get(index);
        if (entity == null) {
            displayBuffer.put(index, entity = spawnEntity(location));
        } else {
            entity = displayBuffer.get(index);
            if (entity.isValid()) {
                if (location != null && !entity.getLocation().equals(location))
                    entity.teleport(location);
            } else {
                entity.remove();
                displayBuffer.put(index, entity = spawnEntity(location));
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
            kv.getValue().remove();
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
        if (!entity.getScoreboardTags().contains(this.tag))
            return;
        if (displayBuffer.containsValue(tClass.cast(entity)))
            return;
        entity.remove();
    }
    boolean hasEntity(Entity entity) {
        if (!tClass.isInstance(entity))
            return false;
        if (!entity.getScoreboardTags().contains(this.tag))
            return false;
        return displayBuffer.containsValue(tClass.cast(entity));
    }

    @Override
    public void close() {
        closed = true;
        owner.buffers.remove(this);
        displayBuffer.values().removeIf(v -> {
            v.remove();
            return true;
        });
    }
}
