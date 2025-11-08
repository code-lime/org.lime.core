package org.lime.core.common.services.buffers;

import org.lime.core.common.api.Service;
import org.lime.core.common.utils.execute.Action1;

import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseEntityBufferStorage<Entity, Location>
        implements Service {
    final Set<BaseEntityBuffer<?, ? extends Entity, Entity, Location>> buffers = ConcurrentHashMap.newKeySet();

    @Override
    public void unregister() {
        buffers.removeIf(v -> {
            v.close();
            return true;
        });
    }

    public abstract <T extends Entity> BaseIterationEntityBuffer<T, Entity, Location> entity(BaseEntityBufferSetup<Location> setup, Class<T> tClass);
    public abstract <Index, T extends Entity> BaseIndexedEntityBuffer<Index, T, Entity, Location> entity(BaseEntityBufferSetup<Location> setup, Class<Index> indexClass, Class<T> tClass);

    protected abstract Location defaultLocation();

    protected abstract <T extends Entity>T spawn(Location location, Class<T> entityClass, Action1<T> setup);
    protected abstract void remove(Entity entity);
    protected abstract void forEntities(Action1<Entity> entityLoad);
    protected abstract Set<String> getTags(Entity v);

    protected abstract boolean isValid(Entity entity);
    protected abstract Location getLocation(Entity entity);
    protected abstract void teleport(Entity entity, Location location);

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
