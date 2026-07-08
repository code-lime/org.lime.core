package org.lime.core.common.services.memories;

import org.jetbrains.annotations.Nullable;
import org.lime.core.common.utils.execute.Action2;

import java.util.UUID;

public record MemoryListenUpdating<T>(
        MemoryKey<T> key,
        Action2<UUID, @Nullable T> callback) {
    public void tryHandle(UUID uid, Object v) {
        callback.invoke(uid, key.castOrNull(v));
    }
}
