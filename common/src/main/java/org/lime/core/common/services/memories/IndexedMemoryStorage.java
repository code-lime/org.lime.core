package org.lime.core.common.services.memories;

import org.jetbrains.annotations.Nullable;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.execute.Action2;
import org.lime.core.common.utils.execute.Action3;
import org.lime.core.common.utils.execute.Func1;
import org.lime.core.common.utils.execute.Func2;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class IndexedMemoryStorage<Index, T>
        extends BaseMemoryStorage<Index, T> {
    public IndexedMemoryStorage(
            BaseConnectionStorageService owner,
            MemoryKey<Index, T> key) {
        super(owner, key);
    }

    @Override
    public Optional<T> get(Index index, UUID playerId) {
        return super.get(index, playerId);
    }
    @Override
    public Stream<Map.Entry<Index, T>> stream(UUID playerId) {
        return super.stream(playerId);
    }
    public Iterator<Map.Entry<Index, T>> iterator(UUID playerId) {
        return stream(playerId).iterator();
    }
    @Override
    public Optional<T> getOrCreate(Index index, UUID playerId, Supplier<T> supplier) {
        return super.getOrCreate(index, playerId, supplier);
    }

    @Override
    public void set(Index index, UUID playerId, @Nullable T value) {
        super.set(index, playerId, value);
    }
    @Override
    public boolean has(Index index, UUID playerId) {
        return super.has(index, playerId);
    }
    @Override
    public boolean remove(Index index, UUID playerId) {
        return super.remove(index, playerId);
    }

    @Override
    public void modify(Index index, UUID playerId, Func1<@Nullable T, @Nullable T> modify) {
        super.modify(index, playerId, modify);
    }
    @Override
    public void modifyEvery(Index index, Func2<UUID, @Nullable T, @Nullable T> modify) {
        super.modifyEvery(index, modify);
    }

    @Override
    public void every(Index index, Action2<UUID, T> action) {
        super.every(index, action);
    }
    @Override
    public void every(Action3<UUID, Index, T> action) {
        super.every(action);
    }

    @Override
    public Disposable listenUpdating(Action3<UUID, Index, @Nullable T> callback) {
        return super.listenUpdating(callback);
    }
}
