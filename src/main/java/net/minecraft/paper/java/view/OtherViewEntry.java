package net.minecraft.paper.java.view;

import java.util.Map;
import java.util.function.Function;

public class OtherViewEntry<K,V> implements Map.Entry<K, V> {
    private final Map.Entry<K,? extends V> other;
    private final Function<V,V> modify;

    public OtherViewEntry(Map.Entry<K,? extends V> other, Function<V,V> modify) {
        this.other = other;
        this.modify = modify;
    }

    @Override
    public K getKey() {
        return other.getKey();
    }
    @Override
    public V getValue() {
        return modify.apply(other.getValue());
    }
    @Override
    public V setValue(V value) {
        throw new UnsupportedOperationException();
    }
}
