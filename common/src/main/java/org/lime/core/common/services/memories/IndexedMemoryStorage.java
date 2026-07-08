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

public class IndexedMemoryStorage<Index, T>
        extends BaseMemoryStorage<T> {
    private final String key;
    private final TypeLiteral<Index> indexType;
    private final TypeLiteral<T> type;

    IndexedMemoryStorage(
            BaseConnectionStorageService owner,
            String key,
            TypeLiteral<Index> indexType,
            TypeLiteral<T> type) {
        super(owner);
        this.key = key;
        this.indexType = indexType;
        this.type = type;
    }

    public String key() {
        return key;
    }
    public TypeLiteral<Index> indexType() {
        return indexType;
    }
    public TypeLiteral<T> type() {
        return type;
    }
    public MemoryKey<T> key(Index index) {
        return new MemoryKey<>(key, type, indexType, index);
    }

    public Optional<T> get(UUID playerId, Index index) {
        return get(key(index), playerId);
    }
    public Optional<T> getOrCreate(UUID playerId, Index index, Supplier<T> supplier) {
        return getOrCreate(key(index), playerId, supplier);
    }

    public void set(UUID playerId, Index index, @Nullable T value) {
        set(key(index), playerId, value);
    }
    public boolean has(UUID playerId, Index index) {
        return has(key(index), playerId);
    }
    public boolean remove(UUID playerId, Index index) {
        return remove(key(index), playerId);
    }

    public void modify(UUID playerId, Index index, Func1<@Nullable T, @Nullable T> modify) {
        modify(key(index), playerId, modify);
    }
    public void modifyEvery(Index index, Func2<UUID, @Nullable T, @Nullable T> modify) {
        modifyEvery(key(index), modify);
    }

    public void every(Index index, Action2<UUID, T> action) {
        every(key(index), action);
    }

    public Disposable listenUpdating(Index index, Action2<UUID, @Nullable T> callback) {
        return listenUpdating(key(index), callback);
    }
}
