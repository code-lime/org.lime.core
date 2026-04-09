package org.lime.core.common.services.memories;

import com.google.inject.TypeLiteral;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record MemoryKey<T>(
        String key,
        TypeLiteral<T> type) {
    public Optional<T> cast(Object value) {
        return Optional.ofNullable(castOrNull(value));
    }
    public @Nullable T castOrNull(Object value) {
        var raw = type.getRawType();
        if (!raw.isInstance(value))
            return null;
        //noinspection unchecked
        return (T)value;
    }
}
