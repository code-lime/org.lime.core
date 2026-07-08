package org.lime.core.common.services.memories;

import org.jetbrains.annotations.Nullable;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.execute.Action2;
import org.lime.core.common.utils.execute.Action3;
import org.lime.core.common.utils.execute.Func1;
import org.lime.core.common.utils.execute.Func2;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class BaseMemoryStorage<Index, T> {
    protected final MemoryKey<Index, T> key;
    protected final BaseConnectionStorageService owner;

    protected BaseMemoryStorage(BaseConnectionStorageService owner, MemoryKey<Index, T> key) {
        this.key = key;
        this.owner = owner;
    }

    protected Optional<T> get(Index index, UUID playerId) {
        return owner.get(key, index, playerId);
    }
    protected Stream<Map.Entry<Index, T>> stream(UUID playerId) {
        return owner.streamIndexed(key, playerId);
    }
    protected Optional<T> getOrCreate(Index index, UUID playerId, Supplier<T> supplier) {
        return owner.getOrCreate(key, index, playerId, supplier);
    }

    protected void set(Index index, UUID playerId, @Nullable T value) {
        owner.set(key, index, playerId, value);
    }
    protected boolean has(Index index, UUID playerId) {
        return owner.has(key, index, playerId);
    }
    protected boolean remove(Index index, UUID playerId) {
        return owner.remove(key, index, playerId);
    }

    protected void modify(Index index, UUID playerId, Func1<@Nullable T, @Nullable T> modify) {
        owner.modify(key, index, playerId, modify);
    }
    protected void modifyEvery(Index index, Func2<UUID, @Nullable T, @Nullable T> modify) {
        owner.modifyEvery(key, index, modify);
    }

    protected void every(Index index, Action2<UUID, T> action) {
        owner.every(key, index, action);
    }
    protected void every(Action3<UUID, Index, T> action) {
        owner.every(key, action);
    }

    protected Disposable listenUpdating(Action3<UUID, Index, @Nullable T> callback) {
        return owner.listenUpdating(key, callback);
    }
}
