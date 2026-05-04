package org.lime.core.common.services.memories;

import org.jetbrains.annotations.Nullable;
import org.lime.core.common.utils.execute.Action2;
import org.lime.core.common.utils.execute.Func1;
import org.lime.core.common.utils.execute.Func2;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public interface MemoryStorage<T> {
    MemoryKey<T> key();

    Optional<T> get(UUID playerId);
    Optional<T> getOrCreate(UUID playerId, Supplier<T> supplier);

    void set(UUID playerId, @Nullable T value);
    boolean has(UUID playerId);
    boolean remove(UUID playerId);

    void modify(UUID playerId, Func1<@Nullable T, @Nullable T> modify);
    void modifyEvery(Func2<UUID, T, @Nullable T> modify);

    void every(Action2<UUID, T> action);
}
