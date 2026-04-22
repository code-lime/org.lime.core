package org.lime.core.common.utils.proxy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface BaseProxyMap<K, V, Impl extends Map<K, V>>
        extends Map<K, V>, BaseProxy<K, Impl> {
    @Override default int size() {
        return proxy().size();
    }

    @Override default boolean isEmpty() {
        return proxy().isEmpty();
    }

    @Override default boolean containsKey(Object key) {
        return proxy().containsKey(mapObjectItem(key));
    }

    @Override default boolean containsValue(Object value) {
        return proxy().containsValue(value);
    }

    @Override default V get(Object key) {
        return proxy().get(mapObjectItem(key));
    }

    @Override default @Nullable V put(K key, V value) {
        return proxy().put(mapItem(key), value);
    }

    @Override default V remove(Object key) {
        return proxy().remove(mapObjectItem(key));
    }

    @Override default void putAll(@NotNull Map<? extends K, ? extends V> m) {
        m.forEach(this::put);
    }

    @Override default void clear() {
        proxy().clear();
    }

    @Override default @NotNull Set<K> keySet() {
        return new KeysSet<>(this);
    }

    @Override default @NotNull Collection<V> values() {
        return proxy().values();
    }

    @Override default @NotNull Set<Entry<K, V>> entrySet() {
        return proxy().entrySet();
    }

    @Override default V getOrDefault(Object key, V defaultValue) {
        return proxy().getOrDefault(mapObjectItem(key), defaultValue);
    }

    @Override default void forEach(BiConsumer<? super K, ? super V> action) {
        proxy().forEach(action);
    }

    @Override default void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        proxy().replaceAll(function);
    }

    @Override default @Nullable V putIfAbsent(K key, V value) {
        return proxy().putIfAbsent(mapItem(key), value);
    }

    @Override default boolean remove(Object key, Object value) {
        return proxy().remove(mapObjectItem(key), value);
    }

    @Override default boolean replace(K key, V oldValue, V newValue) {
        return proxy().replace(mapItem(key), oldValue, newValue);
    }

    @Override default @Nullable V replace(K key, V value) {
        return proxy().replace(mapItem(key), value);
    }

    @Override default V computeIfAbsent(K key, @NotNull Function<? super K, ? extends V> mappingFunction) {
        return proxy().computeIfAbsent(mapItem(key), mappingFunction);
    }

    @Override default V computeIfPresent(K key, @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return proxy().computeIfPresent(mapItem(key), remappingFunction);
    }

    @Override default V compute(K key, @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return proxy().compute(mapItem(key), remappingFunction);
    }

    @Override default V merge(K key, @NotNull V value, @NotNull BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return proxy().merge(mapItem(key), value, remappingFunction);
    }

    record KeysSet<T>(BaseProxyMap<T, ?, ?> owner)
            implements BaseProxySet<T, Set<T>> {
        @Override
        public Map<T, T> proxyItems() {
            return owner.proxyItems();
        }

        @Override
        public Set<T> proxy() {
            return owner.keySet();
        }

        @Override
        public T creatProxyItem(T current) {
            return owner.creatProxyItem(current);
        }
    }

    abstract class Impl<K, V, Impl extends Map<K, V>>
            extends BaseProxyImpl<K, Impl>
            implements BaseProxyMap<K, V, Impl> {
        public Impl(Impl proxy) {
            super(proxy);
        }
    }
}
