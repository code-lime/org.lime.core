package org.lime.core.common.utils.range;

import org.apache.commons.lang3.compare.ComparableUtils;
import org.jetbrains.annotations.NotNull;
import org.lime.core.common.utils.execute.Func2;

public abstract class BaseRange<Self extends BaseRange<Self, T>, T extends Comparable<T>>
        implements FactoryRange<Self, T> {
    protected final Factory<Self, T> factory;

    protected final @NotNull T min;
    protected final @NotNull T max;

    protected BaseRange(@NotNull T a, @NotNull T b, Factory<Self, T> factory) {
        this.factory = factory;

        if (ComparableUtils.is(a).greaterThan(b)) {
            max = a;
            min = b;
        } else {
            max = b;
            min = a;
        }
    }

    @Override
    public Factory<Self, T> factory() {
        return factory;
    }

    @Override
    public @NotNull T min() {
        return min;
    }
    @Override
    public @NotNull T max() {
        return max;
    }

    @Override
    public @NotNull String toString() {
        return format();
    }

    protected static <R extends Range<T>, T extends Comparable<T>> Factory<R, T> createFactory(Class<R> rangeClass, Func2<T, T, R> factory) {
        return new DefaultFactory<>(rangeClass, factory);
    }

    private record DefaultFactory<R extends Range<T>, T extends Comparable<T>>(
            Class<R> rangeClass,
            Func2<T, T, R> factory)
            implements Factory<R, T> {
        @Override
        public R create(T a, T b) {
            return factory.invoke(a, b);
        }
    }
}
