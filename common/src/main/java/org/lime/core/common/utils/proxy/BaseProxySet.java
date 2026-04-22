package org.lime.core.common.utils.proxy;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Predicate;

public interface BaseProxySet<T, Impl extends Set<T>>
        extends Set<T>, BaseProxyCollection<T, Impl> {
    @Override
    default int size() {
        return BaseProxyCollection.super.size();
    }

    @Override
    default boolean isEmpty() {
        return BaseProxyCollection.super.isEmpty();
    }

    @Override
    default boolean contains(Object o) {
        return BaseProxyCollection.super.contains(o);
    }

    @Override
    default @NotNull Iterator<T> iterator() {
        return BaseProxyCollection.super.iterator();
    }

    @Override
    default @NotNull Object @NotNull [] toArray() {
        return BaseProxyCollection.super.toArray();
    }

    @Override
    default @NotNull <T1> T1 @NotNull [] toArray(@NotNull T1 @NotNull [] a) {
        return BaseProxyCollection.super.toArray(a);
    }

    @Override
    default boolean add(T t) {
        return BaseProxyCollection.super.add(t);
    }

    @Override
    default boolean remove(Object o) {
        return BaseProxyCollection.super.remove(o);
    }

    @Override
    default boolean containsAll(@NotNull Collection<?> c) {
        return BaseProxyCollection.super.containsAll(c);
    }

    @Override
    default boolean addAll(@NotNull Collection<? extends T> c) {
        return BaseProxyCollection.super.addAll(c);
    }

    @Override
    default boolean retainAll(@NotNull Collection<?> c) {
        return BaseProxyCollection.super.retainAll(c);
    }

    @Override
    default boolean removeAll(@NotNull Collection<?> c) {
        return BaseProxyCollection.super.removeAll(c);
    }

    @Override
    default boolean removeIf(@NotNull Predicate<? super T> filter) {
        return BaseProxyCollection.super.removeIf(filter);
    }

    @Override
    default void clear() {
        BaseProxyCollection.super.clear();
    }

    abstract class Impl<T, Impl extends Set<T>>
            extends BaseProxyCollection.Impl<T, Impl>
            implements BaseProxySet<T, Impl> {
        public Impl(Impl proxy) {
            super(proxy);
        }
    }
}
