package org.lime.core.common.utils;

import org.lime.core.common.utils.execute.Func0;

import java.util.Collection;

public class RandomShuffledAccess<T>
        implements RandomAccess<T> {
    private final Lazy<Collection<? extends T>> original;

    private RandomShuffledAccess(Func0<Collection<? extends T>> original) {
        this.original = Lazy.of(original);
    }

    @Override
    public T next() {
        return RandomUtils.at(original.value()).orElse(null);
    }

    public static <T>RandomShuffledAccess<T> of(Collection<? extends T> original) {
        return new RandomShuffledAccess<>(() -> original);
    }
    public static <T>RandomShuffledAccess<T> of(Func0<Collection<? extends T>> original) {
        return new RandomShuffledAccess<>(original);
    }
}
