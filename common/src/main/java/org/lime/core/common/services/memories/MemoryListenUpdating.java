package org.lime.core.common.services.memories;

import org.jetbrains.annotations.Nullable;
import org.lime.core.common.utils.execute.Action3;

import java.util.UUID;

public record MemoryListenUpdating<Index, T>(
        MemoryKey<Index, T> key,
        Action3<UUID, Index, @Nullable T> callback) {
    public void tryHandle(UUID uid, Object index, Object v) {
        //noinspection unchecked
        callback.invoke(uid, (Index)index, key.castOrNull(v));
    }
}
