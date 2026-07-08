package org.lime.core.common.services.memories;

import com.google.inject.TypeLiteral;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.execute.Action2;
import org.lime.core.common.utils.execute.Func1;
import org.lime.core.common.utils.execute.Func2;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class MemoryStorage<T>
        extends BaseMemoryStorage<MemoryKey.SingleIndex, T> {
    private final MemoryKey.SingleIndex index = MemoryKey.SingleIndex.KEY;

    public MemoryStorage(BaseConnectionStorageService owner, String key, TypeLiteral<T> type) {
        this(owner, MemoryKey.single(key, type));
    }
    public MemoryStorage(BaseConnectionStorageService owner, MemoryKey<MemoryKey.SingleIndex, T> key) {
        super(owner, key);
    }

    public Optional<T> get(UUID playerId) {
        return get(index, playerId);
    }
    public Optional<T> getOrCreate(UUID playerId, Supplier<T> supplier) {
        return getOrCreate(index, playerId, supplier);
    }

    public void set(UUID playerId, @Nullable T value) {
        set(index, playerId, value);
    }
    public boolean has(UUID playerId) {
        return has(index, playerId);
    }
    public boolean remove(UUID playerId) {
        return remove(index, playerId);
    }

    public void modify(UUID playerId, Func1<@Nullable T, @Nullable T> modify) {
        modify(index, playerId, modify);
    }
    public void modifyEvery(Func2<UUID, @Nullable T, @Nullable T> modify) {
        modifyEvery(index, modify);
    }

    public void every(Action2<UUID, T> action) {
        every(index, action);
    }

    public Disposable listenUpdating(Action2<UUID, @Nullable T> callback) {
        return listenUpdating((playerId, ignoredIndex, value) -> callback.invoke(playerId, value));
    }
}
