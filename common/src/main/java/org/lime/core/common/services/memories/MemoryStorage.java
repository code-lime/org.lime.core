package org.lime.core.common.services.memories;

import org.jetbrains.annotations.Nullable;
import org.lime.core.common.utils.execute.Func1;

import java.util.Optional;
import java.util.UUID;

public interface MemoryStorage<T> {
    MemoryKey<T> key();

    Optional<T> get(UUID playerId);
    void set(UUID playerId, @Nullable T value);
    boolean has(UUID playerId);
    boolean remove(UUID playerId);
    void modify(UUID playerId, Func1<@Nullable T, @Nullable T> modify);
}
