package org.lime.core.common.utils.range;

import org.jetbrains.annotations.NotNull;

public interface Range<T extends Comparable<T>> {
    Factory<? extends Range<T>, T> factory();

    @NotNull T min();
    @NotNull T max();

    default boolean contains(final T element) {
        return element != null
                && element.compareTo(min()) > -1
                && element.compareTo(max()) < 1;
    }
    default boolean contains(final Range<T> range) {
        return range != null
                && contains(range.min())
                && contains(range.max());
    }

    default String format() {
        return "[" + min() + ";" + max() + "]";
    }

    interface Factory<R extends Range<T>, T extends Comparable<T>> {
        Class<R> rangeClass();

        R create(T a, T b);
    }
}

