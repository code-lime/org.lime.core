package org.lime.core.common.utils.proxy;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Predicate;

public interface BaseProxyCollection<T, Impl extends Collection<T>>
        extends Collection<T>, BaseProxyIterable<T, Impl> {
    @Override
    default int size() {
        return proxy().size();
    }

    @Override
    default boolean isEmpty() {
        return proxy().isEmpty();
    }

    @Override
    default boolean contains(Object o) {
        return proxy().contains(mapObjectItem(o));
    }

    @Override
    default @NotNull Iterator<T> iterator() {
        return Iterators.transform(proxy().iterator(), this::mapItem);
    }

    @Override
    default @NotNull Object @NotNull [] toArray() {
        return proxy().toArray();
    }

    @Override
    default @NotNull <T1> T1 @NotNull [] toArray(@NotNull T1 @NotNull [] a) {
        return proxy().toArray(a);
    }

    @Override
    default boolean add(T t) {
        return proxy().add(t);
    }

    @Override
    default boolean remove(Object o) {
        return proxy().remove(mapObjectItem(o));
    }

    @Override
    default boolean containsAll(@NotNull Collection<?> c) {
        for (var value : c)
            if (!contains(value))
                return false;
        return true;
    }

    @Override
    default boolean addAll(@NotNull Collection<? extends T> c) {
        return proxy().addAll(c);
    }

    @Override
    default boolean retainAll(@NotNull Collection<?> c) {
        return proxy().removeIf(c::contains);
    }

    @Override
    default boolean removeAll(@NotNull Collection<?> c) {
        boolean changed = false;
        for (var value : c)
            if (remove(value))
                changed = true;
        return changed;
    }

    @Override
    default boolean removeIf(@NotNull Predicate<? super T> filter) {
        return proxy().removeIf(filter);
    }

    @Override
    default void clear() {
        proxy().clear();
    }

    abstract class Impl<T, Impl extends Collection<T>>
            extends BaseProxyIterable.Impl<T, Impl>
            implements BaseProxyCollection<T, Impl> {
        public Impl(Impl proxy) {
            super(proxy);
        }
    }
}
