package net.minecraft.paper.java.view;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

public class OtherViewMap<K,I extends V,V> implements Map<K,V> {
    protected final Map<K,I> other;
    protected final Function<V,V> modify;

    public OtherViewMap(Map<K,I> other, Function<V,V> modify) {
        this.other = other;
        this.modify = modify;
    }

    @Override
    public int size() {
        return other.size();
    }
    @Override
    public boolean isEmpty() {
        return other.isEmpty();
    }
    @Override
    public boolean containsKey(Object key) {
        return other.containsKey(key);
    }
    @Override
    public boolean containsValue(Object value) {
        return other.containsValue(value);
    }
    @Override
    public V get(Object key) {
        return modify.apply(other.get(key));
    }

    @Override
    public @NotNull Set<K> keySet() {
        return other.keySet();
    }
    @Override
    public @NotNull Collection<V> values() {
        return new OtherViewCollection<>(other.values(), modify);
    }
    @Override
    public @NotNull Set<Entry<K, V>> entrySet() {
        return new OtherViewSetEntry<>(other.entrySet(), modify);
    }

    @Override
    public @Nullable V put(K key, V value) {
        throw new UnsupportedOperationException();
    }
    @Override
    public V remove(Object key) {
        throw new UnsupportedOperationException();
    }
    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }
    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

}
