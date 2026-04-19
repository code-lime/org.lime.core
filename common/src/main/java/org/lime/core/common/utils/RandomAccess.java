package org.lime.core.common.utils;

import org.lime.core.common.utils.execute.Func0;

import java.util.Collection;
import java.util.Iterator;

public interface RandomAccess<T>
    extends Iterator<T> {
    @Override
    default boolean hasNext() {
        return true;
    }

    static <T>RandomAccess<T> of(Collection<? extends T> original, boolean cycled) {
        return cycled
                ? RandomCycledAccess.of(original)
                : RandomShuffledAccess.of(original);
    }
    static <T>RandomAccess<T> of(Func0<Collection<? extends T>> original, boolean cycled) {
        return cycled
                ? RandomCycledAccess.of(original)
                : RandomShuffledAccess.of(original);
    }
}
