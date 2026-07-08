package org.lime.core.common.services.memories;

import com.google.inject.TypeLiteral;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record MemoryKey<Index, T>(
        String key,
        TypeLiteral<Index> indexType,
        TypeLiteral<T> type) {
    public enum SingleIndex {
        KEY,
        ;

        public static final TypeLiteral<SingleIndex> TYPE = TypeLiteral.get(SingleIndex.class);
    }
    public static <T> MemoryKey<SingleIndex, T> single(String key, TypeLiteral<T> type) {
        return new MemoryKey<>(key, SingleIndex.TYPE, type);
    }

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
