package org.lime.core.common.services.buffers;

import com.google.inject.TypeLiteral;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.execute.Action2;

public abstract class BaseIndexedEntityBuffer<Index, T extends Entity, Entity, Location>
        extends BaseEntityBuffer<Index, T, Entity, Location> {
    protected TypeLiteral<Index> indexClass;

    protected BaseIndexedEntityBuffer(BaseEntityBufferStorage<Entity, Location> owner, BaseEntityBufferSetup<Location> setup, TypeLiteral<Index> indexClass, Class<T> tClass) {
        super(owner, setup, tClass);
        this.indexClass = indexClass;
    }

    @Override
    public T indexedNextBuffer(Index index) {
        return super.indexedNextBuffer(index);
    }
    @Override
    public T indexedNextBuffer(Index index, @Nullable Location location) {
        return super.indexedNextBuffer(index, location);
    }
    @Override
    public T indexedNextBuffer(Index index, @Nullable Location location, boolean worldOnly) {
        return super.indexedNextBuffer(index, location, worldOnly);
    }
    @Override
    public Disposable listenSetup(Action2<Index, T> setupAction) {
        return super.listenSetup(setupAction);
    }
}
