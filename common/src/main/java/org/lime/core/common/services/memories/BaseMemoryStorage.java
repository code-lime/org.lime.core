package org.lime.core.common.services.memories;

import org.jetbrains.annotations.Nullable;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.execute.Action2;
import org.lime.core.common.utils.execute.Func1;
import org.lime.core.common.utils.execute.Func2;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public abstract class BaseMemoryStorage<T> {
    protected final BaseConnectionStorageService owner;

    protected BaseMemoryStorage(BaseConnectionStorageService owner) {
        this.owner = owner;
    }

    protected Optional<T> get(MemoryKey<T> key, UUID playerId) {
        return owner.get(key, playerId);
    }
    protected Optional<T> getOrCreate(MemoryKey<T> key, UUID playerId, Supplier<T> supplier) {
        return owner.getOrCreate(key, playerId, supplier);
    }

    protected void set(MemoryKey<T> key, UUID playerId, @Nullable T value) {
        owner.set(key, playerId, value);
    }
    protected boolean has(MemoryKey<T> key, UUID playerId) {
        return owner.has(key, playerId);
    }
    protected boolean remove(MemoryKey<T> key, UUID playerId) {
        return owner.remove(key, playerId);
    }

    protected void modify(MemoryKey<T> key, UUID playerId, Func1<@Nullable T, @Nullable T> modify) {
        owner.modify(key, playerId, modify);
    }
    protected void modifyEvery(MemoryKey<T> key, Func2<UUID, @Nullable T, @Nullable T> modify) {
        owner.modifyEvery(key, modify);
    }

    protected void every(MemoryKey<T> key, Action2<UUID, T> action) {
        owner.every(key, action);
    }

    protected Disposable listenUpdating(MemoryKey<T> key, Action2<UUID, @Nullable T> callback) {
        return owner.listenUpdating(key, callback);
    }
}
