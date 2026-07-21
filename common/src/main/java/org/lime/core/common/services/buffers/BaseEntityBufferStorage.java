package org.lime.core.common.services.buffers;

import com.google.inject.TypeLiteral;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.api.Service;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.execute.Action1;

import java.util.LinkedHashSet;
import java.util.OptionalInt;
import java.util.Set;

public abstract class BaseEntityBufferStorage<Entity, Location>
        implements Service {
    private final Set<BaseEntityBuffer<?, ? extends Entity, Entity, Location>> buffers = new LinkedHashSet<>();
    private boolean closed;

    @Override
    public void unregister() {
        closed = true;
        while (!buffers.isEmpty())
            buffers.iterator().next().close();
    }

    final @NotNull Disposable registerBuffer(@NotNull BaseEntityBuffer<?, ? extends Entity, Entity, Location> buffer) {
        if (closed)
            throw new IllegalStateException("Entity buffer storage is closed");
        buffers.add(buffer);
        return () -> buffers.remove(buffer);
    }

    public abstract <T extends Entity> BaseIterationEntityBuffer<T, Entity, Location> entity(BaseEntityBufferSetup<Location> setup, Class<T> tClass);
    public abstract <Index, T extends Entity> BaseIndexedEntityBuffer<Index, T, Entity, Location> entity(BaseEntityBufferSetup<Location> setup, Class<Index> indexClass, Class<T> tClass);
    public abstract <Index, T extends Entity> BaseIndexedEntityBuffer<Index, T, Entity, Location> entity(BaseEntityBufferSetup<Location> setup, TypeLiteral<Index> indexClass, Class<T> tClass);
    public abstract BaseEntityBufferSetup<Location> createSetup(InjectBuffer injectBuffer);

    protected abstract Location defaultLocation();

    protected abstract <T extends Entity>T spawn(Location location, Class<T> entityClass, @Nullable Key entityKey, Action1<T> setup);
    protected abstract void remove(Entity entity);
    protected abstract void forEntities(Action1<Entity> entityLoad);
    protected abstract Set<String> getTags(Entity entity);
    protected abstract int getEntityId(Entity entity);

    protected abstract boolean isValid(Entity entity);
    protected abstract Location getLocation(Entity entity);
    protected abstract void teleport(Entity entity, Location location);
    protected abstract boolean isEquals(@Nullable Location a, @Nullable Location b, boolean worldOnly);

    protected void onLoaded(Iterable<Entity> entities) {
        for (Entity entity : entities)
            buffers.forEach(buffer -> buffer.entityLoad(entity));
    }
    protected boolean isShouldBeSave(boolean save, Entity entity) {
        if (save) {
            for (var buffer : buffers)
                if (buffer.hasEntity(entity))
                    return false;
        }
        return save;
    }
    protected OptionalInt getTrackingRange(Entity entity) {
        for (var buffer : buffers)
            if (buffer.hasEntity(entity))
                return buffer.setup.trackingDistance();
        return OptionalInt.empty();
    }
}
