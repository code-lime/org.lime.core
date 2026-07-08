package org.lime.core.common.services.memories;

import org.jetbrains.annotations.Nullable;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.execute.Action2;
import org.lime.core.common.utils.execute.Func1;
import org.lime.core.common.utils.execute.Func2;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class MemoryStorage<T>
        extends BaseMemoryStorage<T> {
    private final MemoryKey<T> key;

    MemoryStorage(BaseConnectionStorageService owner, MemoryKey<T> key) {
        super(owner);
        this.key = key;
    }

    public MemoryKey<T> key() {
        return key;
    }

    public Optional<T> get(UUID playerId) {
        return get(key, playerId);
    }
    public Optional<T> getOrCreate(UUID playerId, Supplier<T> supplier) {
        return getOrCreate(key, playerId, supplier);
    }

    public void set(UUID playerId, @Nullable T value) {
        set(key, playerId, value);
    }
    public boolean has(UUID playerId) {
        return has(key, playerId);
    }
    public boolean remove(UUID playerId) {
        return remove(key, playerId);
    }

    public void modify(UUID playerId, Func1<@Nullable T, @Nullable T> modify) {
        modify(key, playerId, modify);
    }
    public void modifyEvery(Func2<UUID, @Nullable T, @Nullable T> modify) {
        modifyEvery(key, modify);
    }

    public void every(Action2<UUID, T> action) {
        every(key, action);
    }

    public Disposable listenUpdating(Action2<UUID, @Nullable T> callback) {
        return listenUpdating(key, callback);
    }
}
